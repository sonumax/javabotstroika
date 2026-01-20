package com.sonumax2.javabot.commands.helper;

public enum CommandName {
    ABOUT("ABOUT_COMMAND"),
    LANGUAGE("LANGUAGE_COMMAND"),
    HELP("HELP_COMMAND"),
    START("START_COMMAND"),
    ADD_OPERATION("ADD_OPERATION");

    private final String name;

    CommandName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
