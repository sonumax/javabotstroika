package com.sonumax2.javabot.bot.flow;

import com.sonumax2.javabot.domain.draft.OpDraftBase;

public final class FlowNav {
    private FlowNav() {}

    /** Если мы пришли в шаг из Confirm->Edit, то возвращаем переход в Confirm, иначе null. */
    public static StepMove confirmIfNeeded(FlowContext<? extends OpDraftBase> ctx) {
        if (ctx.d.consumeReturnToConfirm()) {
            return StepMove.go(FlowDefinition.STEP_CONFIRM);
        }
        return null;
    }

    /** Переход в stepId, но если надо — сначала в Confirm. */
    public static StepMove goOrConfirm(FlowContext<? extends OpDraftBase> ctx, String stepId) {
        StepMove m = confirmIfNeeded(ctx);
        return (m != null) ? m : StepMove.go(stepId);
    }
}
