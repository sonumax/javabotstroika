package com.sonumax2.javabot.domain.operation.service;

import com.sonumax2.javabot.domain.operation.Operation;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OperationTimestampsCallback implements BeforeConvertCallback<Operation> {

    @Override
    public Operation onBeforeConvert(Operation op) {
        LocalDateTime now = LocalDateTime.now();

        // createdAt только при первом сохранении
        if (op.getId() == null && op.getCreatedAt() == null) {
            op.setCreatedAt(now);
        }

        // updatedAt всегда
        op.setUpdatedAt(now);

        return op;
    }
}
