package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConfirmStep<D extends OpDraftBase> implements FlowStep<D> {

    public record EditBtn(String textKey, String stepId) {}

    private final String id;
    private final Function<FlowContext<D>, String> render;
    private final List<EditBtn> edits;
    private final Function<FlowContext<D>, List<EditBtn>> editsProvider;

    private final Consumer<FlowContext<D>> onSave;
    private final Consumer<FlowContext<D>> onCancel;

    public ConfirmStep(
            String id,
            Function<FlowContext<D>, String> render,
            List<EditBtn> edits,
            Consumer<FlowContext<D>> onSave,
            Consumer<FlowContext<D>> onCancel
    ) {
        this.id = id;
        this.render = render;
        this.edits = edits;
        this.onSave = onSave;
        this.onCancel = onCancel;
        this.editsProvider = null;
    }
    public ConfirmStep(String id,
                       Function<FlowContext<D>, String> render,
                       Function<FlowContext<D>, List<EditBtn>> editsProvider,
                       Consumer<FlowContext<D>> onSave,
                       Consumer<FlowContext<D>> onCancel) {
        this.id = id;
        this.render = render;
        this.edits = List.of();
        this.editsProvider = editsProvider;
        this.onSave = onSave;
        this.onCancel = onCancel;
    }

    @Override public String id() { return id; }

    @Override
    public void show(FlowContext<D> ctx, PanelMode mode) {
        ctx.ui.panelText(ctx.chatId, mode, render.apply(ctx), kb(ctx));
    }

    @Override
    public StepMove onCallback(FlowContext<D> ctx, String data, PanelMode mode) {
        String ns = ctx.def.ns;

        if (FlowCb.is(data, ns, id, "back")) return StepMove.stay();

        if (FlowCb.is(data, ns, id, "cancel")) {
            if (onCancel != null) onCancel.accept(ctx);
            return StepMove.finish();
        }

        if (FlowCb.is(data, ns, id, "save")) {
            if (onSave != null) onSave.accept(ctx);
            return StepMove.finish();
        }

        if (FlowCb.startsWith(data, ns, id, "edit")) {
            String stepId = FlowCb.tail(data, ns, id, "edit");
            ctx.d.returnToConfirm = true;
            return StepMove.go(stepId);
        }

        return StepMove.unhandled();
    }

    private InlineKeyboardMarkup kb(FlowContext<D> ctx) {
        String ns = ctx.def.ns;
        List<InlineKeyboardRow> rows = new ArrayList<>();

        InlineKeyboardButton save = InlineKeyboardButton.builder()
                .text(ctx.ui.msg(ctx.chatId, "btnSave"))
                .callbackData(FlowCb.cb(ns, id, "save"))
                .build();

        InlineKeyboardButton cancel = InlineKeyboardButton.builder()
                .text(ctx.ui.msg(ctx.chatId, "btnCancel"))
                .callbackData(FlowCb.cb(ns, id, "cancel"))
                .build();

        rows.add(new InlineKeyboardRow(save, cancel));

        List<EditBtn> list = (editsProvider != null) ? editsProvider.apply(ctx) : edits;

        List<InlineKeyboardButton> buf = new ArrayList<>(2);

        for (EditBtn e : list) {
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(ctx.ui.msg(ctx.chatId, e.textKey()))
                    .callbackData(FlowCb.cb(ns, id, "edit", e.stepId()))
                    .build();

            buf.add(btn);

            if (buf.size() == 2) {
                rows.add(new InlineKeyboardRow(buf.get(0), buf.get(1)));
                buf.clear();
            }
        }

        if (!buf.isEmpty()) {
            rows.add(new InlineKeyboardRow(buf.get(0)));
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static <D extends OpDraftBase> ConfirmStep.Builder<D> builder() {
        return new Builder<>();
    }

    public static class Builder<D extends OpDraftBase> {
        private String id;
        private Function<FlowContext<D>, String> render;
        private List<EditBtn> edits;
        private Function<FlowContext<D>, List<EditBtn>> editsProvider;
        private Consumer<FlowContext<D>> onSave;
        private Consumer<FlowContext<D>> onCancel;

        public Builder<D> id(String id) { this.id = id; return this; }
        public Builder<D> render(Function<FlowContext<D>, String> render) { this.render = render; return this; }

        public Builder<D> edits(List<EditBtn> edits) { this.edits = edits; return this; }
        public Builder<D> editsProvider(Function<FlowContext<D>, List<EditBtn>> editsProvider) { this.editsProvider = editsProvider; return this; }

        public Builder<D> allowSave(Consumer<FlowContext<D>> onSave) { this.onSave = onSave; return this; }
        public Builder<D> allowCancel(Consumer<FlowContext<D>> onCancel) { this.onCancel = onCancel; return this; }

        public ConfirmStep<D> build() {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(render, "render");
            Objects.requireNonNull(onSave, "onSave");
            Objects.requireNonNull(onCancel, "onCancel");

            if (edits != null && editsProvider != null) {
                throw new IllegalStateException("Provide either edits or editsProvider, not both");
            }

            if (editsProvider != null) {
                return new ConfirmStep<>(id, render, editsProvider, onSave, onCancel);
            }

            Objects.requireNonNull(edits, "edits");
            return new ConfirmStep<>(id, render, edits, onSave, onCancel);
        }
    }
}
