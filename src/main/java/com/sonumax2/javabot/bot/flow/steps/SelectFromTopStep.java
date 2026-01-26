package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.domain.reference.BaseRefEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SelectFromTopStep<D extends OpDraftBase, T extends BaseRefEntity> implements FlowStep<D> {

    private final String id;
    private final String askKey;

    private final Function<FlowContext<D>, List<T>> itemsProvider;
    private final BiConsumer<D, String> textToAddSetter;

    private final Function<D, Long> getter;
    private final BiConsumer<D, Long> setter;

    private final String prevStepId;
    private final String nextStepId;
    private final boolean allowSkip;

    private final String newTextStepId;

    public SelectFromTopStep(
            String id,
            String askKey,
            Function<FlowContext<D>, List<T>> itemsProvider,
            Function<D, Long> getter,
            BiConsumer<D, Long> setter,
            BiConsumer<D, String> textToAddSetter,
            String prevStepId,
            String nextStepId,
            boolean allowSkip,
            String newTextStepId
    ) {
        this.id = id;
        this.askKey = askKey;
        this.itemsProvider = itemsProvider;
        this.getter = getter;
        this.setter = setter;
        this.textToAddSetter = textToAddSetter;
        this.prevStepId = prevStepId;
        this.nextStepId = nextStepId;
        this.allowSkip = allowSkip;
        this.newTextStepId = newTextStepId;
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
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");

            // спец: вернуться в меню выбора операции
            if ("@opsMenu".equals(prevStepId)) {
                ctx.ui.panelKey(
                        ctx.chatId,
                        mode,
                        "operation.text",
                        ctx.keyboard.operationsAddMenuInline(ctx.chatId, CbParts.ADD_OPR, CbParts.MENU)
                );
                return StepMove.finish();
            }

            // если это первый шаг, лучше отмена, чем go(null)
            if (prevStepId == null || prevStepId.isBlank()) {
                ctx.ui.panelKey(ctx.chatId, mode, "cancelled", ctx.keyboard.mainMenuInline(ctx.chatId));
                return StepMove.finish();
            }

            return StepMove.go(prevStepId);
        }

        if (FlowCb.is(data, ns, id, "new")) {
            return StepMove.go(newTextStepId);
        }

        if (FlowCb.startsWith(data, ns, id, "pick")) {
            long picked = FlowCb.tailLong(data, ns, id, "pick");
            setter.accept(ctx.d, picked);

            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(nextStepId);
        }

        if (FlowCb.is(data, ns, id, "skip")) {
            setter.accept(ctx.d, null);

            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(nextStepId);
        }

        return StepMove.unhandled();
    }

    @Override
    public StepMove onText(FlowContext<D> ctx, String raw, PanelMode mode) {
        String input = raw == null ? "" : raw.trim();
        if (input.isBlank()) return StepMove.unhandled();

        if (textToAddSetter != null) {
            textToAddSetter.accept(ctx.d, input); // сохранили текст для следующего шага
        }

        if (newTextStepId != null && !newTextStepId.isBlank() && textToAddSetter != null) {
            return StepMove.go(newTextStepId);
        }

        return StepMove.unhandled();
    }


    private InlineKeyboardMarkup kb(FlowContext<D> ctx) {
        String ns = ctx.def.ns;

        List<T> list = itemsProvider.apply(ctx);
        if (list == null) list = List.of();

        List<InlineKeyboardRow> rows = new ArrayList<>();

        // по 2 в ряд
        for (int i = 0; i < list.size(); i += 2) {
            T a = list.get(i);
            if (i + 1 < list.size()) {
                T b = list.get(i + 1);
                rows.add(new InlineKeyboardRow(
                        refBtn(ctx, a.getName(), FlowCb.cb(ns, id, "pick", a.getId())),
                        refBtn(ctx, b.getName(), FlowCb.cb(ns, id, "pick", b.getId()))
                ));
            } else {
                rows.add(new InlineKeyboardRow(
                        refBtn(ctx, a.getName(), FlowCb.cb(ns, id, "pick", a.getId()))
                ));
            }
        }

        rows.add(new InlineKeyboardRow(btn(ctx, "add", FlowCb.cb(ns, id, "new"))));
        if (allowSkip) {
            rows.add(new InlineKeyboardRow(btn(ctx, "skip", FlowCb.cb(ns, id, "skip"))));
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

    private InlineKeyboardButton refBtn(FlowContext<D> ctx, String text, String cb) {
        return InlineKeyboardButton.builder()
                .text(cut(text))
                .callbackData(cb)
                .build();
    }

    private String cut(String s) {
        if (s == null) return "";
        return s.length() <= 50 ? s : s.substring(0, 49) + "…";
    }

    public static <D extends OpDraftBase, T extends BaseRefEntity> Builder<D, T> builder() {
        return new Builder<>();
    }

    public static class Builder<D extends OpDraftBase, T extends BaseRefEntity> {
        private String id;
        private String askKey;

        private Function<FlowContext<D>, List<T>> itemsProvider;

        private Function<D, Long> getter;
        private BiConsumer<D, Long> setter;

        private BiConsumer<D, String> textToAddSetter;

        private String prevStepId;
        private String nextStepId;

        private boolean allowSkip;
        private String newTextStepId;

        public Builder<D, T> id(String id) { this.id = id; return this; }
        public Builder<D, T> askKey(String askKey) { this.askKey = askKey; return this; }

        public Builder<D, T> options(Function<FlowContext<D>, List<T>> supplier) { this.itemsProvider = supplier; return this; }

        public Builder<D, T> bind(Function<D, Long> getter, BiConsumer<D, Long> setter) {
            this.getter = getter;
            this.setter = setter;
            return this;
        }

        public Builder<D, T> onTextSaveTo(BiConsumer<D, String> textToAddSetter) {
            this.textToAddSetter = textToAddSetter;
            return this;
        }

        public Builder<D, T> backTo(String stepId) { this.prevStepId = stepId; return this; }
        public Builder<D, T> nextTo(String stepId) { this.nextStepId = stepId; return this; }

        public Builder<D, T> allowSkip(boolean allowSkip) { this.allowSkip = allowSkip; return this; }

        public Builder<D, T> textGoesTo(String newTextStepId) { this.newTextStepId = newTextStepId; return this; }

        public SelectFromTopStep<D, T> build() {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(askKey, "askKey");
            Objects.requireNonNull(itemsProvider, "supplier");
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(setter, "setter");
            Objects.requireNonNull(prevStepId, "backStepId");
            Objects.requireNonNull(nextStepId, "nextStepId");

            return new SelectFromTopStep<>(
                    id,
                    askKey,
                    itemsProvider,
                    getter,
                    setter,
                    textToAddSetter,
                    prevStepId,
                    nextStepId,
                    allowSkip,
                    newTextStepId
            );
        }
    }

}
