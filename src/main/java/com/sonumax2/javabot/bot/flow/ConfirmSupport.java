package com.sonumax2.javabot.bot.flow;

import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.domain.operation.OperationSaver;

public class ConfirmSupport {

    public static <D extends OpDraftBase> void saveAndShowMain(
            FlowContext<D> ctx,
            OperationSaver<D> saver,
            String successKey
    ) {
        long id = saver.save(ctx.d, ctx.chatId);
        ctx.ui().showSaved(ctx.chatId, PanelMode.EDIT, successKey, id);
    }

    public static <D extends OpDraftBase> void cancelAndShowMain(
            FlowContext<D> ctx,
            String cancelKey
    ) {
        ctx.ui().showCancelled(ctx.chatId, PanelMode.EDIT, cancelKey);
    }
}
