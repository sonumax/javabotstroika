package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.util.InputParseUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class AmountInputStep<D extends OpDraftBase> implements FlowStep<D> {

    private final String id;
    private final String askKey;
    private final String invalidKey = "amountInvalid";

    private final Function<D, BigDecimal> getter;
    private final BiConsumer<D, BigDecimal> setter;

    private final String prevStepId;
    private final String nextStepId;

    public AmountInputStep(
            String id,
            String askKey,
            Function<D, BigDecimal> getter,
            BiConsumer<D, BigDecimal> setter,
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

        if (FlowCb.is(data, ns, id, "back")) {
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
            return StepMove.go(prevStepId);
        }

        return StepMove.unhandled();
    }

    @Override
    public StepMove onText(FlowContext<D> ctx, String raw, PanelMode mode) {
        BigDecimal amount = InputParseUtils.parseAmount(raw);

        if (amount == null || amount.signum() <= 0) {
            ctx.ui.panelKey(ctx.chatId, mode, invalidKey, kb(ctx));
            return StepMove.rendered();
        }

        setter.accept(ctx.d, amount);

        if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
        return StepMove.go(nextStepId);
    }

    private InlineKeyboardMarkup kb(FlowContext<D> ctx) {
        String ns = ctx.def.ns;
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(btn(ctx, "back", FlowCb.cb(ns, id, "back")))))
                .build();
    }

    private InlineKeyboardButton btn(FlowContext<D> ctx, String textKey, String cb) {
        return InlineKeyboardButton.builder()
                .text(ctx.ui.msg(ctx.chatId, textKey))
                .callbackData(cb)
                .build();
    }
}
