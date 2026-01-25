package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.ExpenseDraft;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.domain.operation.DocType;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DocTypeSelectStep<D extends OpDraftBase> implements FlowStep<D> {

    private final String id;
    private final String askKey;

    private final Function<D, DocType> getter;
    private final BiConsumer<D, DocType> setter;
    private final BiConsumer<D, DocType> afterSet;

    private final String prevStepId;
    private final String nextNeedsFile;
    private final String nextNoFile;

    public DocTypeSelectStep(
            String id,
            String askKey,
            Function<D, DocType> getter,
            BiConsumer<D, DocType> setter,
            BiConsumer<D, DocType> afterSet,
            String prevStepId,
            String nextNeedsFile,
            String nextNoFile
    ) {
        this.id = id;
        this.askKey = askKey;
        this.getter = getter;
        this.setter = setter;
        this.afterSet = afterSet;
        this.prevStepId = prevStepId;
        this.nextNeedsFile = nextNeedsFile;
        this.nextNoFile = nextNoFile;
    }

    @Override public String id() { return id; }

    @Override
    public void show(FlowContext<D> ctx, PanelMode mode) {
        ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx));
    }

    @Override
    public StepMove onCallback(FlowContext<D> ctx, String data, PanelMode mode) {
        String ns = ctx.def.ns;

        if (FlowCb.is(data, ns, id, "back")) {
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(prevStepId);
        }

        if (FlowCb.startsWith(data, ns, id, "set")) {
            String name = FlowCb.tail(data, ns, id, "set");
            try {
                DocType v = DocType.valueOf(name);
                setter.accept(ctx.d, v);
                if (afterSet != null) afterSet.accept(ctx.d, v);

                if (v == DocType.NO_RECEIPT) {
                    if (ctx.d instanceof ExpenseDraft ed) {
                        ed.docFileId = null;
                    }
                }

                if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
                return StepMove.go(v.needsFile() ? nextNeedsFile : nextNoFile);

            } catch (Exception ignore) {
                ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx));
                return StepMove.rendered();
            }
        }

        return StepMove.unhandled();
    }

    private InlineKeyboardMarkup kb(FlowContext<D> ctx) {
        String ns = ctx.def.ns;

        InlineKeyboardRow r1 = new InlineKeyboardRow(btn(ctx, "receipt.has", FlowCb.cb(ns, id, "set", DocType.RECEIPT.name())));
        InlineKeyboardRow r2 = new InlineKeyboardRow(btn(ctx, "receipt.invoice", FlowCb.cb(ns, id, "set", DocType.INVOICE.name())));
        InlineKeyboardRow r3 = new InlineKeyboardRow(btn(ctx, "receipt.none", FlowCb.cb(ns, id, "set", DocType.NO_RECEIPT.name())));
        InlineKeyboardRow r4 = new InlineKeyboardRow(btn(ctx, "back", FlowCb.cb(ns, id, "back")));

        return InlineKeyboardMarkup.builder().keyboard(List.of(r1, r2, r3, r4)).build();
    }

    private InlineKeyboardButton btn(FlowContext<D> ctx, String textKey, String cb) {
        return InlineKeyboardButton.builder()
                .text(ctx.ui.msg(ctx.chatId, textKey))
                .callbackData(cb)
                .build();
    }
}
