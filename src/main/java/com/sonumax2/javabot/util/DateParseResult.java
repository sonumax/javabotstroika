package com.sonumax2.javabot.util;

import java.time.LocalDate;

public class DateParseResult {
    public final LocalDate date;
    public final Error error;

    private DateParseResult(LocalDate date, Error error) {
        this.date = date;
        this.error = error;
    }

    public static DateParseResult ok(LocalDate date) {
        return new DateParseResult(date, Error.NONE);
    }

    public static DateParseResult err(Error error) {
        return new DateParseResult(null, error);
    }

    public enum Error {
        NONE,
        INVALID_FORMAT,
        INVALID_DATE,
        NEED_MONTH
    }
}
