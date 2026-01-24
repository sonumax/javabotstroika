package com.sonumax2.javabot.bot.flow;

public record StepMove(Type type, String stepId) {

    public enum Type { UNHANDLED, STAY, GOTO, FINISH, RENDERED }

    public static StepMove unhandled() { return new StepMove(Type.UNHANDLED, null); }
    public static StepMove stay() { return new StepMove(Type.STAY, null); }
    public static StepMove go(String stepId) { return new StepMove(Type.GOTO, stepId); }
    public static StepMove finish() { return new StepMove(Type.FINISH, null); }
    public static StepMove rendered() { return new StepMove(Type.RENDERED, null); }

}