package com.sonumax2.javabot.domain.operation;

public interface OperationSaver<D> {
    long save(D draft, long chatId);
}
