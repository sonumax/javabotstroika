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
import java.util.function.Consumer;
import java.util.function.Function;

public class ConfirmStep<D extends OpDraftBase> implements FlowStep<D> {

    public record EditBtn(String textKey, String stepId) {}

    private final String id; // обычно "confirm"
    private final Function<FlowContext<D>, String> render; // <-- главное отличие
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

        for (EditBtn e : list) {
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text(ctx.ui.msg(ctx.chatId, e.textKey()))
                            .callbackData(FlowCb.cb(ns, id, "edit", e.stepId()))
                            .build()
            ));
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }
}
