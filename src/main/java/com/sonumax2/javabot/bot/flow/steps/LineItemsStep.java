package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.ExpenseLineItem;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.*;

public class LineItemsStep<D extends OpDraftBase> implements FlowStep<D> {

    private final String id;
    private final String titleKey;

    private final Function<D, List<ExpenseLineItem>> itemsGetter;

    private final BiFunction<FlowContext<D>, Long, String> nameResolver;

    private final String prevStepId;
    private final String nextStepId;
    private final String addStepId;
    private final BiConsumer<D, Long> pendingPickSetter;
    private final BiConsumer<D, String> pendingPickNameSetter; // можно null
    private final String editStepId;

    public LineItemsStep(
            String id,
            String titleKey,
            Function<D, List<ExpenseLineItem>> itemsGetter,
            BiFunction<FlowContext<D>, Long, String> nameResolver,

            BiConsumer<D, Long> pendingPickSetter,
            BiConsumer<D, String> pendingPickNameSetter,

            String prevStepId,
            String nextStepId,
            String addStepId,
            String editStepId
    ) {
        this.id = id;
        this.titleKey = titleKey == null ? "lineItems.title" : titleKey;
        this.itemsGetter = itemsGetter;
        this.nameResolver = nameResolver;

        this.pendingPickSetter = pendingPickSetter;
        this.pendingPickNameSetter = pendingPickNameSetter;

        this.prevStepId = prevStepId;
        this.nextStepId = nextStepId;
        this.addStepId = addStepId;
        this.editStepId = editStepId;
    }


    @Override public String id() { return id; }

    @Override
    public void show(FlowContext<D> ctx, PanelMode mode) {
        List<ExpenseLineItem> items = safe(itemsGetter.apply(ctx.d));

        StringBuilder sb = new StringBuilder();
        sb.append(ctx.ui.msg(ctx.chatId, titleKey));

        if (items.isEmpty()) {
            sb.append("\n\n").append(ctx.ui.msg(ctx.chatId, "lineItems.empty"));
        } else {
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < items.size(); i++) {
                ExpenseLineItem it = items.get(i);
                String name = nameResolver.apply(ctx, it.nomenclatureId);
                String vol = it.volume == null ? "?" : it.volume.toPlainString();

                if (it.volume != null) total = total.add(it.volume);

                sb.append("\n").append(i + 1).append(") ").append(name).append(" — ").append(vol);
            }

            sb.append("\n\n").append(ctx.ui.msg(ctx.chatId, "lineItems.total", total.toPlainString()));
        }

        ctx.ui.panelText(ctx.chatId, mode, sb.toString(), kb(ctx, items));
    }

    @Override
    public StepMove onCallback(FlowContext<D> ctx, String data, PanelMode mode) {
        String ns = ctx.def.ns;

        if (FlowCb.is(data, ns, id, "back")) {
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
            return StepMove.go(prevStepId);
        }

        if (FlowCb.is(data, ns, id, "add")) {
            return StepMove.go(addStepId);
        }

        if (FlowCb.startsWith(data, ns, id, "edit")) {
            long nid = FlowCb.tailLong(data, ns, id, "edit");
            pendingPickSetter.accept(ctx.d, nid);
            if (pendingPickNameSetter != null) pendingPickNameSetter.accept(ctx.d, null);
            return StepMove.go(editStepId);
        }

        if (FlowCb.is(data, ns, id, "next")) {
            List<ExpenseLineItem> items = safe(itemsGetter.apply(ctx.d));
            if (items.isEmpty()) {
                show(ctx, PanelMode.EDIT);
                return StepMove.rendered();
            }
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
            return StepMove.go(nextStepId);
        }

        if (FlowCb.startsWith(data, ns, id, "rm")) {
            long nid = FlowCb.tailLong(data, ns, id, "rm");
            List<ExpenseLineItem> items = safe(itemsGetter.apply(ctx.d));

            for (int i = 0; i < items.size(); i++) {
                ExpenseLineItem it = items.get(i);
                if (it != null && Objects.equals(it.nomenclatureId, nid)) {
                    items.remove(i);
                    break;
                }
            }

            show(ctx, PanelMode.EDIT);
            return StepMove.rendered();
        }

        return StepMove.unhandled();
    }

    private InlineKeyboardMarkup kb(FlowContext<D> ctx, List<ExpenseLineItem> items) {
        String ns = ctx.def.ns;
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (ExpenseLineItem it : items) {
            if (it == null || it.nomenclatureId == null) continue;
            String name = nameResolver.apply(ctx, it.nomenclatureId);

            InlineKeyboardButton edit = btnText("✏️ " + cut(name, 28), FlowCb.cb(ns, id, "edit", it.nomenclatureId));
            InlineKeyboardButton rm   = btnText("❌", FlowCb.cb(ns, id, "rm", it.nomenclatureId));

            rows.add(new InlineKeyboardRow(edit, rm));
        }

        rows.add(new InlineKeyboardRow(btnKey(ctx, "btnAddLine", FlowCb.cb(ns, id, "add"))));
        rows.add(new InlineKeyboardRow(btnKey(ctx, "btnDone", FlowCb.cb(ns, id, "next"))));
        rows.add(new InlineKeyboardRow(btnKey(ctx, "back", FlowCb.cb(ns, id, "back"))));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton btnKey(FlowContext<D> ctx, String key, String cb) {
        return InlineKeyboardButton.builder().text(ctx.ui.msg(ctx.chatId, key)).callbackData(cb).build();
    }

    private InlineKeyboardButton btnText(String text, String cb) {
        return InlineKeyboardButton.builder().text(text).callbackData(cb).build();
    }

    private static String cut(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "…"; }

    private static <T> List<T> safe(List<T> v) { return v == null ? new ArrayList<>() : v; }
}
