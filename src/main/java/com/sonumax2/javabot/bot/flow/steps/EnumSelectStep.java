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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class EnumSelectStep<D extends OpDraftBase, E extends Enum<E>> implements FlowStep<D> {

    private final String id;
    private final String askKey;

    private final Class<E> enumClass;
    private final Function<E, String> labelKey; // ключ локализации для кнопки

    private final Function<D, E> getter;
    private final BiConsumer<D, E> setter;

    private final String prevStepId;
    private final String nextStepId;

    public EnumSelectStep(
            String id,
            String askKey,
            Class<E> enumClass,
            Function<E, String> labelKey,
            Function<D, E> getter,
            BiConsumer<D, E> setter,
            String prevStepId,
            String nextStepId
    ) {
        this.id = id;
        this.askKey = askKey;
        this.enumClass = enumClass;
        this.labelKey = labelKey;
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

        if (FlowCb.startsWith(data, ns, id, "set")) {
            String name = FlowCb.tail(data, ns, id, "set");
            try {
                E val = Enum.valueOf(enumClass, name);
                setter.accept(ctx.d, val);
                if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
                return StepMove.go(nextStepId);
            } catch (Exception ignore) {
                // просто перерисуем
                ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx));
                return StepMove.rendered();
            }
        }

        return StepMove.unhandled();
    }

    private InlineKeyboardMarkup kb(FlowContext<D> ctx) {
        String ns = ctx.def.ns;
        List<InlineKeyboardRow> rows = new ArrayList<>();

        E[] vals = enumClass.getEnumConstants();
        for (E v : vals) {
            rows.add(new InlineKeyboardRow(
                    btn(ctx, labelKey.apply(v), FlowCb.cb(ns, id, "set", v.name()))
            ));
        }

        rows.add(new InlineKeyboardRow(btn(ctx, "back", FlowCb.cb(ns, id, "back"))));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton btn(FlowContext<D> ctx, String textKey, String cb) {
        return InlineKeyboardButton.builder()
                .text(ctx.ui.msg(ctx.chatId, textKey))
                .callbackData(cb)
                .build();
    }
}
