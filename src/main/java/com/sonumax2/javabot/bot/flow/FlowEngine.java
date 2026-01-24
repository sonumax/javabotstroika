package com.sonumax2.javabot.bot.flow;

import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.domain.draft.service.DraftService;
import com.sonumax2.javabot.domain.session.UserState;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class FlowEngine {

    private final BotUi ui;
    private final KeyboardService keyboard;
    private final UserSessionService session;
    private final DraftService drafts;

    public FlowEngine(BotUi ui, KeyboardService keyboard, UserSessionService session, DraftService drafts) {
        this.ui = ui;
        this.keyboard = keyboard;
        this.session = session;
        this.drafts = drafts;
    }

    public <D extends OpDraftBase> void handle(Update update, FlowDefinition<D> def) {

        if (update == null) return;

        if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            handleCallback(update, def);
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            handleText(update, def);
            return;
        }

        if (update.hasMessage() && (update.getMessage().hasPhoto() || update.getMessage().hasDocument())) {
            handleFile(update, def);
        }
    }

    private <D extends OpDraftBase> void handleCallback(Update update, FlowDefinition<D> def) {
        var cq = update.getCallbackQuery();
        String data = cq.getData();
        long chatId = cq.getMessage().getChatId();
        int messageId = cq.getMessage().getMessageId();

        ui.setPanelId(chatId, messageId);
        ui.ack(cq.getId());

        if (def.isStartCallback(data)) {
            drafts.clear(chatId, def.draftType);
            D d = drafts.get(chatId, def.draftType, def.draftClass);
            d.step = def.startStepId;

            session.setActiveFlow(chatId, def.ns, def.draftType.key());

            FlowContext<D> ctx = ctx(chatId, def, d);
            showStep(ctx, def.startStepId, PanelMode.EDIT);

            drafts.save(chatId, def.draftType, d);
            return;
        }

        D d = drafts.get(chatId, def.draftType, def.draftClass);
        if (d.step == null || d.step.isBlank()) d.step = def.startStepId;

        String stepIdFromCb = parseStepId(def.ns, data);
        String targetStepId = (stepIdFromCb != null) ? stepIdFromCb : d.step;

        FlowContext<D> ctx = ctx(chatId, def, d);
        FlowStep<D> step = def.step(targetStepId);
        if (step == null) {
            // неизвестный шаг -> просто перерисуем текущий
            showStep(ctx, d.step, PanelMode.EDIT);
            drafts.save(chatId, def.draftType, d);
            return;
        }

        StepMove mv = step.onCallback(ctx, data, PanelMode.EDIT);
        applyMoveAndMaybeShow(ctx, mv, PanelMode.EDIT);

        if (mv != null && mv.type() == StepMove.Type.FINISH) {
            drafts.clear(chatId, def.draftType);
            return;
        }

        drafts.save(chatId, def.draftType, d);
    }

    private <D extends OpDraftBase> void handleText(Update update, FlowDefinition<D> def) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        if (text != null && text.startsWith("/")) return;

        D d = drafts.get(chatId, def.draftType, def.draftClass);
        if (d.step == null || d.step.isBlank()) d.step = def.startStepId;

        FlowContext<D> ctx = ctx(chatId, def, d);
        FlowStep<D> step = def.step(d.step);
        if (step == null) return;

        StepMove mv = step.onText(ctx, text, PanelMode.MOVE_DOWN);
        applyMoveAndMaybeShow(ctx, mv, PanelMode.MOVE_DOWN);

        if (mv != null && mv.type() == StepMove.Type.FINISH) {
            drafts.clear(chatId, def.draftType);
            return;
        }

        drafts.save(chatId, def.draftType, d);
    }

    private <D extends OpDraftBase> void handleFile(Update update, FlowDefinition<D> def) {
        long chatId = update.getMessage().getChatId();

        D d = drafts.get(chatId, def.draftType, def.draftClass);
        if (d.step == null || d.step.isBlank()) d.step = def.startStepId;

        FlowContext<D> ctx = ctx(chatId, def, d);
        FlowStep<D> step = def.step(d.step);
        if (step == null) return;

        StepMove mv = step.onFile(ctx, update, PanelMode.MOVE_DOWN);
        applyMoveAndMaybeShow(ctx, mv, PanelMode.MOVE_DOWN);

        if (mv != null && mv.type() == StepMove.Type.FINISH) {
            drafts.clear(chatId, def.draftType);
            return;
        }

        drafts.save(chatId, def.draftType, d);
    }

    private <D extends OpDraftBase> void applyMoveAndMaybeShow(FlowContext<D> ctx, StepMove mv, PanelMode mode) {

        if (mv == null || mv.type() == StepMove.Type.UNHANDLED) {
            showStep(ctx, ctx.d.step, mode);
            return;
        }

        if (mv.type() == StepMove.Type.RENDERED) {
            return; // шаг сам уже показал ошибку/экран
        }

        if (mv.type() == StepMove.Type.STAY) {
            showStep(ctx, ctx.d.step, mode);
            return;
        }

        if (mv.type() == StepMove.Type.GOTO) {
            ctx.d.step = mv.stepId();
            showStep(ctx, ctx.d.step, mode);
            return;
        }

        if (mv.type() == StepMove.Type.FINISH) {
            ctx.d.step = null;
            ctx.session.clearActiveFlow(ctx.chatId);
            ctx.session.setUserState(ctx.chatId, UserState.IDLE);
        }
    }


    private <D extends OpDraftBase> void showStep(FlowContext<D> ctx, String stepId, PanelMode mode) {
        ctx.session.setUserState(ctx.chatId, UserState.FLOW_WAIT_INPUT);

        FlowStep<D> step = ctx.def.step(stepId);
        if (step != null) step.show(ctx, mode);
    }

    private <D extends OpDraftBase> FlowContext<D> ctx(long chatId, FlowDefinition<D> def, D d) {
        return new FlowContext<>(
                chatId,
                ui,
                keyboard,
                session,
                drafts,
                def.draftType,
                def.draftClass,
                def,
                d
        );
    }

    /** callback формата: ns:step:... -> вернёт step */
    private String parseStepId(String ns, String data) {
        if (data == null || ns == null) return null;
        String prefix = ns + ":";
        if (!data.startsWith(prefix)) return null;

        int p1 = data.indexOf(':');
        if (p1 < 0) return null;

        int p2 = data.indexOf(':', p1 + 1);
        if (p2 < 0) return null;

        return data.substring(p1 + 1, p2);
    }

    public <D extends OpDraftBase> void startFromMessage(Update update, FlowDefinition<D> def) {
        if (update == null || !update.hasMessage()) return;

        long chatId = update.getMessage().getChatId();
        int messageId = update.getMessage().getMessageId();

        ui.setPanelId(chatId, messageId);

        drafts.clear(chatId, def.draftType);
        D d = drafts.get(chatId, def.draftType, def.draftClass);
        d.step = def.startStepId;

        session.setActiveFlow(chatId, def.ns, def.draftType.key());

        FlowContext<D> ctx = ctx(chatId, def, d);
        showStep(ctx, def.startStepId, PanelMode.MOVE_DOWN);

        drafts.save(chatId, def.draftType, d);
    }

    private <D extends OpDraftBase> boolean finishStep(long chatId, StepMove mv, FlowDefinition<D> def) {
        if (mv != null && mv.type() == StepMove.Type.FINISH) {
            drafts.clear(chatId, def.draftType);
            session.clearActiveFlow(chatId);
            return true;
        }
        return false;
    }

}
