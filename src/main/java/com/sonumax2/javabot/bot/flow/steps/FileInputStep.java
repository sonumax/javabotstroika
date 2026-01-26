package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class FileInputStep<D extends OpDraftBase> implements FlowStep<D> {

    private final String id;
    private final String askKey;

    private final Function<D, String> getter;
    private final BiConsumer<D, String> setter;

    private final String prevStepId;
    private final String nextStepId;

    private final boolean allowSkip;

    public FileInputStep(
            String id,
            String askKey,
            Function<D, String> getter,
            BiConsumer<D, String> setter,
            String prevStepId,
            String nextStepId,
            boolean allowSkip
    ) {
        this.id = id;
        this.askKey = askKey;
        this.getter = getter;
        this.setter = setter;
        this.prevStepId = prevStepId;
        this.nextStepId = nextStepId;
        this.allowSkip = allowSkip;
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

            if (prevStepId == null || prevStepId.isBlank()) {
                ctx.ui.panelKey(ctx.chatId, mode, "cancelled", ctx.keyboard.mainMenuInline(ctx.chatId));
                return StepMove.finish();
            }
            return StepMove.go(prevStepId);
        }

        if (allowSkip && FlowCb.is(data, ns, id, "skip")) {
            setter.accept(ctx.d, null);
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(nextStepId);
        }

        return StepMove.unhandled();
    }

    @Override
    public StepMove onFile(FlowContext<D> ctx, Update update, PanelMode mode) {
        String fileId = extractFileId(update);
        if (fileId == null || fileId.isBlank()) return StepMove.unhandled();

        setter.accept(ctx.d, fileId);

        if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
        return StepMove.go(nextStepId);
    }

    private InlineKeyboardMarkup kb(FlowContext<D> ctx) {
        String ns = ctx.def.ns;

        InlineKeyboardRow row1;
        if (allowSkip) {
            row1 = new InlineKeyboardRow(
                    btn(ctx, "skip", FlowCb.cb(ns, id, "skip")),
                    btn(ctx, "back", FlowCb.cb(ns, id, "back"))
            );
        } else {
            row1 = new InlineKeyboardRow(
                    btn(ctx, "back", FlowCb.cb(ns, id, "back"))
            );
        }

        return InlineKeyboardMarkup.builder().keyboard(List.of(row1)).build();
    }

    private InlineKeyboardButton btn(FlowContext<D> ctx, String textKey, String cb) {
        return InlineKeyboardButton.builder()
                .text(ctx.ui.msg(ctx.chatId, textKey))
                .callbackData(cb)
                .build();
    }

    private String extractFileId(Update update) {
        if (update == null || !update.hasMessage()) return null;

        var msg = update.getMessage();

        if (msg.hasDocument() && msg.getDocument() != null) {
            return msg.getDocument().getFileId();
        }

        if (msg.hasPhoto() && msg.getPhoto() != null && !msg.getPhoto().isEmpty()) {
            // берём самое большое фото
            PhotoSize best = msg.getPhoto().stream()
                    .max(Comparator.comparingInt(p -> (p.getFileSize() != null ? p.getFileSize() : 0)))
                    .orElse(null);
            return best != null ? best.getFileId() : null;
        }

        return null;
    }
}
