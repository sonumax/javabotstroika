package com.sonumax2.javabot.bot.flow.confirm;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.domain.draft.OpDraftBase;

import java.util.List;
import java.util.function.Function;

public final class ConfirmRenderer {

    private ConfirmRenderer() {}

    public static <D extends OpDraftBase> String render(
            FlowContext<D> ctx,
            Function<String, String> msg,
            List<ConfirmField<D>> fields,
            String emptyValueKey // например "common.none"
    ) {
        StringBuilder sb = new StringBuilder();

        for (ConfirmField<D> f : fields) {
            if (!f.showIf().test(ctx)) continue;

            String label = msg.apply(f.labelKey());
            String v = f.value().apply(ctx);
            if (v == null || v.isBlank()) v = msg.apply(emptyValueKey);

            if (f.multiline()) {
                sb.append(label).append(":\n").append(v).append("\n");
            } else {
                sb.append(label).append(": ").append(v).append("\n");
            }
        }

        // убираем последний перенос
        if (!sb.isEmpty()) sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
