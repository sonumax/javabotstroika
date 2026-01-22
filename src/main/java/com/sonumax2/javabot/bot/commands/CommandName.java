package com.sonumax2.javabot.bot.commands;

public enum CommandName {
    ABOUT("about"),
    LANGUAGE("language"),
    HELP("help"),
    START("start"),
    ADD_OPERATION("addOperation"),
    UNKNOWN("unknown"),
    ADVANCE("advance"),
    EXPENSE("expense");

    private final String name;

    CommandName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
