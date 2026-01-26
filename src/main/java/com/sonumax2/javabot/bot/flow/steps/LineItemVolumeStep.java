package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.ExpenseLineItem;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.util.InputParseUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.*;

public class LineItemVolumeStep<D extends OpDraftBase> implements FlowStep<D> {

    private final String id;
    private final String askKey;
    private final String invalidKey;

    private final Function<D, Long> pendingIdGetter;
    private final Function<D, java.util.List<ExpenseLineItem>> itemsGetter;

    private final BiConsumer<D, Long> pendingIdSetter;
    private final BiConsumer<D, String> pendingNameSetter;

    private final BiFunction<FlowContext<D>, Long, String> nameResolver;

    private final String prevStepId;
    private final String nextStepId; // обычно возвращаемся в список items

    public LineItemVolumeStep(
            String id,
            String askKey,
            String invalidKey,
            Function<D, Long> pendingIdGetter,
            BiConsumer<D, Long> pendingIdSetter,
            BiConsumer<D, String> pendingNameSetter,
            Function<D, java.util.List<ExpenseLineItem>> itemsGetter,
            BiFunction<FlowContext<D>, Long, String> nameResolver,
            String prevStepId,
            String nextStepId
    ) {
        this.id = id;
        this.askKey = askKey;
        this.invalidKey = invalidKey == null ? "decimalInvalid" : invalidKey;
        this.pendingIdGetter = pendingIdGetter;
        this.pendingIdSetter = pendingIdSetter;
        this.pendingNameSetter = pendingNameSetter;
        this.itemsGetter = itemsGetter;
        this.nameResolver = nameResolver;
        this.prevStepId = prevStepId;
        this.nextStepId = nextStepId;
    }

    @Override public String id() { return id; }

    @Override
    public void show(FlowContext<D> ctx, PanelMode mode) {
        Long nid = pendingIdGetter.apply(ctx.d);
        String name = nid == null ? "?" : nameResolver.apply(ctx, nid);
        ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx), name);
    }

    @Override
    public StepMove onCallback(FlowContext<D> ctx, String data, PanelMode mode) {
        String ns = ctx.def.ns;

        if (FlowCb.is(data, ns, id, "back")) {
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(prevStepId);
        }
        return StepMove.unhandled();
    }

    @Override
    public StepMove onText(FlowContext<D> ctx, String raw, PanelMode mode) {
        BigDecimal v = InputParseUtils.parseDecimal(raw);
        if (v == null || v.signum() <= 0) {
            ctx.ui.panelKey(ctx.chatId, mode, invalidKey, kb(ctx));
            return StepMove.rendered();
        }

        Long nid = pendingIdGetter.apply(ctx.d);
        if (nid == null) {
            return StepMove.go(prevStepId);
        }

        List<ExpenseLineItem> items = itemsGetter.apply(ctx.d);

        boolean updated = false;
        for (ExpenseLineItem it : items) {
            if (it != null && java.util.Objects.equals(it.nomenclatureId, nid)) {
                it.volume = v;
                updated = true;
                break;
            }
        }
        if (!updated) {
            items.add(new ExpenseLineItem(nid, v));
        }

        pendingIdSetter.accept(ctx.d, null);
        pendingNameSetter.accept(ctx.d, null);

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
