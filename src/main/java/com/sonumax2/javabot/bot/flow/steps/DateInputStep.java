package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.util.DateParseResult;
import com.sonumax2.javabot.util.InputParseUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DateInputStep<D extends OpDraftBase> implements FlowStep<D> {

    private static final String INVALID_KEY = "dateInvalid";
    private static final String FUTURE_KEY = "dateInFuture";

    private final String id;
    private final String askKey;

    private final Function<D, LocalDate> getter; // пока не используешь, но ок — пригодится для подсветки выбранного
    private final BiConsumer<D, LocalDate> setter;

    private final String prevStepId;
    private final String nextStepId;

    public DateInputStep(
            String id,
            String askKey,
            Function<D, LocalDate> getter,
            BiConsumer<D, LocalDate> setter,
            String prevStepId,
            String nextStepId
    ) {
        this.id = id;
        this.askKey = askKey;
        this.getter = getter;
        this.setter = setter;
        this.prevStepId = prevStepId;
        this.nextStepId = nextStepId;
    }

    @Override
    public String id() { return id; }

    @Override
    public void show(FlowContext<D> ctx, PanelMode mode) {
        ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx));
    }

    @Override
    public StepMove onCallback(FlowContext<D> ctx, String data, PanelMode mode) {
        String ns = ctx.def.ns;
        LocalDate today = LocalDate.now(ctx.session.getZoneId(ctx.chatId));

        if (FlowCb.is(data, ns, id, "back")) {
            return onBack(ctx, mode);
        }

        if (FlowCb.is(data, ns, id, "today")) {
            return applyAndNext(ctx, today);
        }

        if (FlowCb.is(data, ns, id, "yesterday")) {
            return applyAndNext(ctx, today.minusDays(1));
        }

        if (FlowCb.startsWith(data, ns, id, "pick")) {
            try {
                String iso = FlowCb.tail(data, ns, id, "pick");
                LocalDate picked = LocalDate.parse(iso);

                if (picked.isAfter(today)) return render(ctx, mode, FUTURE_KEY);
                return applyAndNext(ctx, picked);
            } catch (Exception ignore) {
                return render(ctx, mode, INVALID_KEY);
            }
        }

        return StepMove.unhandled();
    }

    @Override
    public StepMove onText(FlowContext<D> ctx, String raw, PanelMode mode) {
        LocalDate today = LocalDate.now(ctx.session.getZoneId(ctx.chatId));
        DateParseResult res = InputParseUtils.parseSmartDate(raw, today);

        if (res.error != DateParseResult.Error.NONE) {
            return render(ctx, mode, INVALID_KEY);
        }
        if (res.date.isAfter(today)) {
            return render(ctx, mode, FUTURE_KEY);
        }

        return applyAndNext(ctx, res.date);
    }

    private StepMove onBack(FlowContext<D> ctx, PanelMode mode) {
        if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);

        // назад с первого шага = отмена
        if (prevStepId == null || prevStepId.isBlank()) {
            ctx.ui.panelKey(ctx.chatId, mode, "cancelled", ctx.keyboard.mainMenuInline(ctx.chatId));
            return StepMove.finish();
        }

        return StepMove.go(prevStepId);
    }

    private StepMove applyAndNext(FlowContext<D> ctx, LocalDate date) {
        setter.accept(ctx.d, date);
        if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
        return StepMove.go(nextStepId);
    }

    private StepMove render(FlowContext<D> ctx, PanelMode mode, String msgKey) {
        ctx.ui.panelKey(ctx.chatId, mode, msgKey, kb(ctx));
        return StepMove.rendered();
    }

    private InlineKeyboardMarkup kb(FlowContext<D> ctx) {
        String ns = ctx.def.ns;

        InlineKeyboardRow row1 = new InlineKeyboardRow(
                btn(ctx, "date.today", FlowCb.cb(ns, id, "today")),
                btn(ctx, "date.yesterday", FlowCb.cb(ns, id, "yesterday"))
        );
        InlineKeyboardRow row2 = new InlineKeyboardRow(
                btn(ctx, "back", FlowCb.cb(ns, id, "back"))
        );

        return InlineKeyboardMarkup.builder().keyboard(List.of(row1, row2)).build();
    }

    private InlineKeyboardButton btn(FlowContext<D> ctx, String textKey, String cb) {
        return InlineKeyboardButton.builder()
                .text(ctx.ui.msg(ctx.chatId, textKey))
                .callbackData(cb)
                .build();
    }
}
