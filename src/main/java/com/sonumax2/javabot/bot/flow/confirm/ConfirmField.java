package com.sonumax2.javabot.bot.flow.confirm;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.domain.draft.OpDraftBase;

import java.util.function.Function;
import java.util.function.Predicate;

public record ConfirmField<D extends OpDraftBase>(
        String labelKey,
        Function<FlowContext<D>, String> value,
        Predicate<FlowContext<D>> showIf,
        boolean multiline
) {
    public static <D extends OpDraftBase> ConfirmField<D> of(String labelKey, Function<FlowContext<D>, String> value) {
        return new ConfirmField<>(labelKey, value, ctx -> true, false);
    }

    public static <D extends OpDraftBase> ConfirmField<D> of(String labelKey, Function<FlowContext<D>, String> value, Predicate<FlowContext<D>> showIf) {
        return new ConfirmField<>(labelKey, value, showIf, false);
    }

    public static <D extends OpDraftBase> ConfirmField<D> multiline(String labelKey, Function<FlowContext<D>, String> value) {
        return new ConfirmField<>(labelKey, value, ctx -> true, true);
    }
}

