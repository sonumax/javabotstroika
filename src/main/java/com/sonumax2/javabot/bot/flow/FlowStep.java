package com.sonumax2.javabot.bot.flow;

import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface FlowStep<D extends OpDraftBase> {
    String id();

    /** показать панель этого шага */
    void show(FlowContext<D> ctx, PanelMode mode);

    /** обработать callback (если это callback этого шага) */
    default StepMove onCallback(FlowContext<D> ctx, String data, PanelMode mode) {
        return StepMove.unhandled();
    }

    /** обработать текст */
    default StepMove onText(FlowContext<D> ctx, String text, PanelMode mode) {
        return StepMove.unhandled();
    }

    /** обработать фото/документ */
    default StepMove onFile(FlowContext<D> ctx, Update update, PanelMode mode) {
        return StepMove.unhandled();
    }
}
