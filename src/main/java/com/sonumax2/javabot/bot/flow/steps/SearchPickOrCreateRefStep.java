package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.domain.reference.BaseRefEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.*;
import java.util.function.*;

public class SearchPickOrCreateRefStep<D extends OpDraftBase, T extends BaseRefEntity> implements FlowStep<D> {

    private final String id;
    private final String askKey;

    private final Function<D, String> pendingGetter;
    private final BiConsumer<D, String> pendingSetter;

    private final Function<D, Long> idGetter;
    private final BiConsumer<D, Long> idSetter;

    private final BiFunction<FlowContext<D>, String, Optional<T>> exactFinder;
    private final TriFunction<FlowContext<D>, String, Integer, List<T>> searcher;
    private final BiFunction<FlowContext<D>, String, Long> creator;

    private final String prevStepId;
    private final String nextStepId;

    private final int limit;

    public SearchPickOrCreateRefStep(
            String id,
            String askKey,
            Function<D, String> pendingGetter,
            BiConsumer<D, String> pendingSetter,
            Function<D, Long> idGetter,
            BiConsumer<D, Long> idSetter,
            BiFunction<FlowContext<D>, String, Optional<T>> exactFinder,
            TriFunction<FlowContext<D>, String, Integer, List<T>> searcher,
            BiFunction<FlowContext<D>, String, Long> creator,
            String prevStepId,
            String nextStepId,
            int limit
    ) {
        this.id = id;
        this.askKey = askKey;
        this.pendingGetter = pendingGetter;
        this.pendingSetter = pendingSetter;
        this.idGetter = idGetter;
        this.idSetter = idSetter;
        this.exactFinder = exactFinder;
        this.searcher = searcher;
        this.creator = creator;
        this.prevStepId = prevStepId;
        this.nextStepId = nextStepId;
        this.limit = limit;
    }

    @Override public String id() { return id; }

    @Override
    public void show(FlowContext<D> ctx, PanelMode mode) {
        ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx, List.of()));
    }

    @Override
    public StepMove onCallback(FlowContext<D> ctx, String data, PanelMode mode) {
        String ns = ctx.def.ns;

        if (FlowCb.is(data, ns, id, "back")) {
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(prevStepId);
        }

        if (FlowCb.startsWith(data, ns, id, "pick")) {
            long picked = FlowCb.tailLong(data, ns, id, "pick");
            idSetter.accept(ctx.d, picked);
            pendingSetter.accept(ctx.d, null);

            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(nextStepId);
        }

        if (FlowCb.is(data, ns, id, "create")) {
            String pending = safe(pendingGetter.apply(ctx.d));
            if (pending.isBlank()) {
                ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx, List.of()));
                return StepMove.rendered();
            }
            Long newId = creator.apply(ctx, pending);
            idSetter.accept(ctx.d, newId);
            pendingSetter.accept(ctx.d, null);

            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(nextStepId);
        }

        return StepMove.unhandled();
    }

    @Override
    public StepMove onText(FlowContext<D> ctx, String raw, PanelMode mode) {
        String input = safe(raw).trim();
        if (input.isBlank()) {
            ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx, List.of()));
            return StepMove.rendered();
        }

        // 1) точное совпадение -> сразу дальше
        Optional<T> exact = exactFinder.apply(ctx, input);
        if (exact.isPresent() && exact.get().getId() != null) {
            idSetter.accept(ctx.d, exact.get().getId());
            pendingSetter.accept(ctx.d, null);

            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(nextStepId);
        }

        // 2) нет точного -> показываем похожие + кнопка создать
        pendingSetter.accept(ctx.d, input);
        List<T> found = searcher.apply(ctx, input, limit);
        ctx.ui.panelText(ctx.chatId, mode, renderText(ctx, input, found), kb(ctx, found));
        return StepMove.rendered();
    }

    private String renderText(FlowContext<D> ctx, String input, List<T> found) {
        String base = ctx.ui.msg(ctx.chatId, askKey);
        if (found == null || found.isEmpty()) {
            return base + "\n\n" + "Не нашёл совпадений. Можно создать: «" + input + "»";
        }
        return base + "\n\n" + "Похожие варианты (или создай «" + input + "»):";
    }

    private InlineKeyboardMarkup kb(FlowContext<D> ctx, List<T> found) {
        String ns = ctx.def.ns;
        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (found != null) {
            for (T t : found) {
                rows.add(new InlineKeyboardRow(btnText(cut(t.getName(), 50), FlowCb.cb(ns, id, "pick", t.getId()))));
            }
        }

        String pending = safe(pendingGetter.apply(ctx.d));
        if (!pending.isBlank()) {
            rows.add(new InlineKeyboardRow(btnText("➕ Создать «" + cut(pending, 30) + "»", FlowCb.cb(ns, id, "create"))));
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

    private InlineKeyboardButton btnText(String text, String cb) {
        return InlineKeyboardButton.builder().text(text).callbackData(cb).build();
    }

    private String safe(String s) { return s == null ? "" : s; }
    private String cut(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "…"; }

    @FunctionalInterface
    public interface TriFunction<A,B,C,R> { R apply(A a, B b, C c); }
}
