package com.sonumax2.javabot.bot.flow.confirm;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.domain.operation.DocType;

public final class ConfirmDocUtil {
    private ConfirmDocUtil() {}

    public static String buildDocWithFileMark(FlowContext<?> ctx, DocType docType, String docFileId) {
        DocType dt = (docType == null ? DocType.NO_RECEIPT : docType);

        String docLabel = switch (dt) {
            case RECEIPT -> ctx.ui().msg(ctx.chatId, "expense.doc.receipt");
            case INVOICE -> ctx.ui().msg(ctx.chatId, "expense.doc.invoice");
            case NO_RECEIPT -> ctx.ui().msg(ctx.chatId, "expense.doc.none");
        };

        if (!dt.needsFile()) return docLabel;

        String mark = ctx.ui().msg(
                ctx.chatId,
                (docFileId != null && !docFileId.isBlank()) ? "expense.doc.file.ok" : "expense.doc.file.miss"
        );

        return docLabel + " " + mark;
    }
}
