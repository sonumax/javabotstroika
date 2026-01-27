package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
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

    private static final String KEY_REF_SEARCH_NONE  = "ref.search.none";
    private static final String KEY_REF_SEARCH_FOUND = "ref.search.found";
    private static final String KEY_REF_CREATE_BTN   = "ref.createBtn";
    private static final String KEY_REF_SEARCH_EXACT = "ref.search.exact";

    private final String id;
    private final String askKey;

    private final Function<D, String> pendingGetter;
    private final BiConsumer<D, String> pendingSetter;

    private final BiConsumer<D, Long> idSetter;

    private final BiFunction<FlowContext<D>, String, Optional<T>> exactFinder;
    private final TriFunction<FlowContext<D>, String, Integer, List<T>> searcher;
    private final BiFunction<FlowContext<D>, String, Long> creator;

    private final String backStepId;
    private final String nextStepId;
    private final int limit;

    public SearchPickOrCreateRefStep(
            String id,
            String askKey,
            Function<D, String> pendingGetter,
            BiConsumer<D, String> pendingSetter,
            BiConsumer<D, Long> idSetter,
            BiFunction<FlowContext<D>, String, Optional<T>> exactFinder,
            TriFunction<FlowContext<D>, String, Integer, List<T>> searcher,
            BiFunction<FlowContext<D>, String, Long> creator,
            String backStepId,
            String nextStepId,
            int limit
    ) {
        this.id = id;
        this.askKey = askKey;
        this.pendingGetter = pendingGetter;
        this.pendingSetter = pendingSetter;
        this.idSetter = idSetter;
        this.exactFinder = exactFinder;
        this.searcher = searcher;
        this.creator = creator;
        this.backStepId = backStepId;
        this.nextStepId = nextStepId;
        this.limit = limit;
    }

    @Override public String id() { return id; }

    @Override
    public void show(FlowContext<D> ctx, PanelMode mode) {
        String pending = safe(pendingGetter.apply(ctx.d)).trim();
        if (pending.isBlank()) {
            ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx, List.of(), null));
            return;
        }

        Optional<T> exact = exactFinder.apply(ctx, pending);
        Long exactId = exact.map(BaseRefEntity::getId).orElse(null);

        List<T> found = searcher.apply(ctx, pending, limit);
        List<T> merged = mergeExactFirst(exact.orElse(null), found, limit);

        ctx.ui.panelText(
                ctx.chatId,
                mode,
                renderText(ctx, pending, merged, exactId != null),
                kb(ctx, merged, exactId)
        );
    }

    @Override
    public StepMove onText(FlowContext<D> ctx, String raw, PanelMode mode) {
        String input = safe(raw).trim();
        if (input.isBlank()) {
            pendingSetter.accept(ctx.d, null);
            ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx, List.of(), null));
            return StepMove.rendered();
        }

        // 1) точное совпадение -> сразу дальше
        Optional<T> exact = exactFinder.apply(ctx, input);
        if (exact.isPresent() && exact.get().getId() != null) {
            idSetter.accept(ctx.d, exact.get().getId());
            pendingSetter.accept(ctx.d, null);

            if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
            return StepMove.go(nextStepId);
        }

        // 2) нет точного -> показываем похожие + кнопка создать
        pendingSetter.accept(ctx.d, input);
        List<T> found = searcher.apply(ctx, input, limit);
        ctx.ui.panelText(ctx.chatId, mode, renderText(ctx, input, found, false), kb(ctx, found, null));
        return StepMove.rendered();
    }

    @Override
    public StepMove onCallback(FlowContext<D> ctx, String data, PanelMode mode) {
        String ns = ctx.def.ns;

        if (FlowCb.is(data, ns, id, "back")) {
            // важно: чистим pending, иначе при следующем заходе будет “старый поиск”
            pendingSetter.accept(ctx.d, null);
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
            return StepMove.go(backStepId);
        }

        if (FlowCb.startsWith(data, ns, id, "pick")) {
            long picked = FlowCb.tailLong(data, ns, id, "pick");
            idSetter.accept(ctx.d, picked);
            pendingSetter.accept(ctx.d, null);

            if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
            return StepMove.go(nextStepId);
        }

        if (FlowCb.is(data, ns, id, "create")) {
            String pending = safe(pendingGetter.apply(ctx.d)).trim();
            if (pending.isBlank()) {
                ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx, List.of(), null));
                return StepMove.rendered();
            }

            Long newId = creator.apply(ctx, pending);
            idSetter.accept(ctx.d, newId);
            pendingSetter.accept(ctx.d, null);

            if (ctx.d.consumeReturnToConfirm()) return StepMove.go(FlowDefinition.STEP_CONFIRM);
            return StepMove.go(nextStepId);
        }

        return StepMove.unhandled();
    }

    private String renderText(FlowContext<D> ctx, String input, List<T> found, boolean hasExact) {
        String base = ctx.ui.msg(ctx.chatId, askKey);

        if (found == null || found.isEmpty()) {
            return base + "\n" + ctx.ui.msg(ctx.chatId, KEY_REF_SEARCH_NONE, input);
        }

        if (hasExact) {
            if (found.size() == 1) {
                return base + "\n" + ctx.ui.msg(ctx.chatId, KEY_REF_SEARCH_EXACT);
            }
            return base + "\n" + ctx.ui.msg(ctx.chatId, KEY_REF_SEARCH_EXACT)
                    + "\n" + ctx.ui.msg(ctx.chatId, KEY_REF_SEARCH_FOUND, input);
        }

        return base + "\n" + ctx.ui.msg(ctx.chatId, KEY_REF_SEARCH_FOUND, input);
    }


    private InlineKeyboardMarkup kb(FlowContext<D> ctx, List<T> found, Long exactId) {
        String ns = ctx.def.ns;
        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (found != null) {
            for (T t : found) {
                if (t == null || t.getId() == null) continue;

                String name = cut(safe(t.getName()), 50);
                if (exactId != null && exactId.equals(t.getId())) {
                    name = "✅ " + name;
                }

                rows.add(new InlineKeyboardRow(
                        btnText(name, FlowCb.cb(ns, id, "pick", t.getId()))
                ));
            }
        }

        String pending = safe(pendingGetter.apply(ctx.d)).trim();

        if (!pending.isBlank() && exactId == null) {
            rows.add(new InlineKeyboardRow(
                    btnText(ctx.ui.msg(ctx.chatId, KEY_REF_CREATE_BTN, cut(pending, 30)), FlowCb.cb(ns, id, "create"))
            ));
        }

        rows.add(new InlineKeyboardRow(btnKey(ctx, "back", FlowCb.cb(ns, id, "back"))));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }


    private InlineKeyboardButton btnKey(FlowContext<D> ctx, String textKey, String cb) {
        return InlineKeyboardButton.builder()
                .text(ctx.ui.msg(ctx.chatId, textKey))
                .callbackData(cb)
                .build();
    }

    private InlineKeyboardButton btnText(String text, String cb) {
        return InlineKeyboardButton.builder().text(text).callbackData(cb).build();
    }

    private List<T> mergeExactFirst(T exact, List<T> found, int limit) {
        if (limit <= 0) return List.of();

        LinkedHashMap<Long, T> map = new LinkedHashMap<>();

        if (exact != null && exact.getId() != null) {
            map.put(exact.getId(), exact);
        }

        if (found != null) {
            for (T t : found) {
                if (t == null || t.getId() == null) continue;
                map.putIfAbsent(t.getId(), t);
                if (map.size() >= limit) break;
            }
        }

        return new ArrayList<>(map.values());
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String cut(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "…"; }

    @FunctionalInterface
    public interface TriFunction<A, B, C, R> { R apply(A a, B b, C c); }

    public static <D extends OpDraftBase, T extends BaseRefEntity> Builder<D, T> builder() {
        return new Builder<>();
    }

    public static class Builder<D extends OpDraftBase, T extends BaseRefEntity> {
        private String id;
        private String askKey;

        private Function<D, String> pendingGetter;
        private BiConsumer<D, String> pendingSetter;

        private BiConsumer<D, Long> idSetter;

        private BiFunction<FlowContext<D>, String, Optional<T>> exactFinder;
        private TriFunction<FlowContext<D>, String, Integer, List<T>> searcher;
        private BiFunction<FlowContext<D>, String, Long> creator;

        private String backStepId;
        private String nextStepId;
        private int limit = 8;

        public Builder<D, T> id(String id) { this.id = id; return this; }
        public Builder<D, T> askKey(String askKey) { this.askKey = askKey; return this; }

        public Builder<D, T> pending(Function<D, String> getter, BiConsumer<D, String> setter) {
            this.pendingGetter = getter;
            this.pendingSetter = setter;
            return this;
        }

        public Builder<D, T> saveIdTo(BiConsumer<D, Long> idSetter) { this.idSetter = idSetter; return this; }

        public Builder<D, T> exact(BiFunction<FlowContext<D>, String, Optional<T>> exactFinder) { this.exactFinder = exactFinder; return this; }
        public Builder<D, T> search(TriFunction<FlowContext<D>, String, Integer, List<T>> searcher) { this.searcher = searcher; return this; }
        public Builder<D, T> create(BiFunction<FlowContext<D>, String, Long> creator) { this.creator = creator; return this; }

        public Builder<D, T> backTo(String stepId) { this.backStepId = stepId; return this; }
        public Builder<D, T> nextTo(String stepId) { this.nextStepId = stepId; return this; }
        public Builder<D, T> limit(int limit) { this.limit = limit; return this; }

        public SearchPickOrCreateRefStep<D, T> build() {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(askKey, "askKey");
            Objects.requireNonNull(pendingGetter, "pendingGetter");
            Objects.requireNonNull(pendingSetter, "pendingSetter");
            Objects.requireNonNull(idSetter, "idSetter");
            Objects.requireNonNull(exactFinder, "exactFinder");
            Objects.requireNonNull(searcher, "searcher");
            Objects.requireNonNull(creator, "creator");
            Objects.requireNonNull(backStepId, "backStepId");
            Objects.requireNonNull(nextStepId, "nextStepId");

            return new SearchPickOrCreateRefStep<>(
                    id, askKey,
                    pendingGetter, pendingSetter,
                    idSetter,
                    exactFinder, searcher, creator,
                    backStepId, nextStepId,
                    limit
            );
        }
    }

}
