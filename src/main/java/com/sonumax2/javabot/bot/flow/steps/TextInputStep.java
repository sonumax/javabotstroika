package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TextInputStep<D extends OpDraftBase> implements FlowStep<D> {

    public interface Validator {
        /** вернуть null если ок, иначе key ошибки (например "nameInvalid") */
        String validate(String text);
    }

    private final String id;
    private final String askKey;
    private final String invalidKey;

    private final Function<D, String> getter;
    private final BiConsumer<D, String> setter;

    private final String prevStepId;
    private final String nextStepId;

    private final boolean allowSkip;
    private final String skipToStepId;

    private final Validator validator;

    public TextInputStep(
            String id,
            String askKey,
            String invalidKey,
            Function<D, String> getter,
            BiConsumer<D, String> setter,
            String prevStepId,
            String nextStepId,
            Validator validator
    ) {
        this(id, askKey, invalidKey, getter, setter, prevStepId, nextStepId, false, null, validator);
    }

    public TextInputStep(
            String id,
            String askKey,
            String invalidKey,
            Function<D, String> getter,
            BiConsumer<D, String> setter,
            String prevStepId,
            String nextStepId,
            boolean allowSkip,
            String skipToStepId,
            Validator validator
    ) {
        this.id = id;
        this.askKey = askKey;
        this.invalidKey = invalidKey;
        this.getter = getter;
        this.setter = setter;
        this.prevStepId = prevStepId;
        this.nextStepId = nextStepId;
        this.allowSkip = allowSkip;
        this.skipToStepId = skipToStepId;
        this.validator = validator;
    }

    @Override
    public String id() { return id; }

    @Override
    public void show(FlowContext<D> ctx, PanelMode mode) {
        ctx.ui.panelKey(ctx.chatId, mode, askKey, keyboard(ctx, false));
    }

    @Override
    public StepMove onCallback(FlowContext<D> ctx, String data, PanelMode mode) {
        String ns = ctx.def.ns;

        if (FlowCb.is(data, ns, id, "back")) {
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
            return StepMove.go(prevStepId);
        }

        if (allowSkip && FlowCb.is(data, ns, id, "skip")) {
            setter.accept(ctx.d, null);
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
            return StepMove.go(skipToStepId != null ? skipToStepId : nextStepId);
        }

        return StepMove.unhandled();
    }

    @Override
    public StepMove onText(FlowContext<D> ctx, String raw, PanelMode mode) {
        String text = raw == null ? "" : raw.trim();

        String err = (validator == null) ? null : validator.validate(text);
        if (text.isEmpty() || err != null) {
            String key = (err != null) ? err : invalidKey;
            ctx.ui.panelKey(ctx.chatId, mode, key, keyboard(ctx, true));
            return StepMove.rendered();
        }

        setter.accept(ctx.d, text);

        if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
        return StepMove.go(nextStepId);
    }

    private InlineKeyboardMarkup keyboard(FlowContext<D> ctx, boolean isError) {
        String ns = ctx.def.ns;

        InlineKeyboardRow r1;
        if (allowSkip) {
            r1 = new InlineKeyboardRow(
                    btn(ctx, "skip", FlowCb.cb(ns, id, "skip"))
            );
            return InlineKeyboardMarkup.builder()
                    .keyboard(List.of(
                            r1,
                            new InlineKeyboardRow(btn(ctx, "back", FlowCb.cb(ns, id, "back")))
                    ))
                    .build();
        }

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
