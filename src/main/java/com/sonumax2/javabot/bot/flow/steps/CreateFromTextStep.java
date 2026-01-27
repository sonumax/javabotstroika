package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class CreateFromTextStep<D extends OpDraftBase> implements FlowStep<D> {

    private final String id;
    private final String askKey;

    private final BiFunction<FlowContext<D>, String, Long> createId;
    private final BiConsumer<D, Long> setId;

    private final String prevStepId;
    private final String nextStepId;

    public CreateFromTextStep(
            String id,
            String askKey,
            BiFunction<FlowContext<D>, String, Long> createId,
            BiConsumer<D, Long> setId,
            String prevStepId,
            String nextStepId
    ) {
        this.id = id;
        this.askKey = askKey;
        this.createId = createId;
        this.setId = setId;
        this.prevStepId = prevStepId;
        this.nextStepId = nextStepId;
    }

    @Override public String id() { return id; }

    @Override
    public void show(FlowContext<D> ctx, PanelMode mode) {
        ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx));
    }

    @Override
    public StepMove onCallback(FlowContext<D> ctx, String data, PanelMode mode) {
        String ns = ctx.def.ns;

        if (FlowCb.is(data, ns, id, "back")) {
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
            return StepMove.go(prevStepId);
        }
        return StepMove.unhandled();
    }

    @Override
    public StepMove onText(FlowContext<D> ctx, String raw, PanelMode mode) {
        String text = raw == null ? "" : raw.trim();
        if (text.isBlank()) {
            ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx));
            return StepMove.rendered();
        }

        try {
            Long id = createId.apply(ctx, text);
            if (id == null) {
                ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx));
                return StepMove.rendered();
            }

            setId.accept(ctx.d, id);

            if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
            return StepMove.go(nextStepId);

        } catch (Exception e) {
            ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx));
            return StepMove.rendered();
        }
    }

    private InlineKeyboardMarkup kb(FlowContext<D> ctx) {
        String ns = ctx.def.ns;
        InlineKeyboardButton back = InlineKeyboardButton.builder()
                .text(ctx.ui.msg(ctx.chatId, "back"))
                .callbackData(FlowCb.cb(ns, id, "back"))
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(back)))
                .build();
    }
}
