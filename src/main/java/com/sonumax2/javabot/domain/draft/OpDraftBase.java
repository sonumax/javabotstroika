package com.sonumax2.javabot.domain.draft;

/**
 * Общая база для draft-ов, которые ведутся как пошаговый flow.
 */
public abstract class OpDraftBase {
    /** если пользователь редактирует из confirm-экрана */
    public boolean returnToConfirm;

    /** текущий шаг flow (например: "obj", "amount", "date", "doc", "confirm") */
    public String step;

    public boolean isReturnToConfirm() {
        return returnToConfirm;
    }

    /** если мы в режиме редактирования из confirm — сбросить флаг и перейти в confirm */
    public boolean consumeReturnToConfirm() {
        if (!returnToConfirm) return false;
        returnToConfirm = false;
        return true;
    }
}