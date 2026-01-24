package com.sonumax2.javabot.bot.flow.adv;

import com.sonumax2.javabot.bot.commands.cb.AdvanceCb;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.steps.AmountInputStep;
import com.sonumax2.javabot.bot.flow.steps.ConfirmStep;
import com.sonumax2.javabot.bot.flow.steps.DateInputStep;
import com.sonumax2.javabot.bot.flow.steps.TextInputStep;
import com.sonumax2.javabot.domain.draft.AdvanceDraft;
import com.sonumax2.javabot.domain.draft.DraftType;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AdvanceFlowConfig {

    @Bean
    public FlowDefinition<AdvanceDraft> advanceFlow(OperationRepository operationRepository) {

        return new FlowDefinition<AdvanceDraft>(
                AdvanceCb.NS,
                DraftType.ADVANCE,
                AdvanceDraft.class,
                "date"
        )
                .addStartCallback(AdvanceCb.start())
                .addStartCommand("/advance")
                .addStep(new DateInputStep<>(
                        "date",
                        "advance.askDate",
                        d -> d.date,
                        (d,v) -> d.date = v,
                        "menu",       // или куда назад
                        "amount"
                ))
                .addStep(new AmountInputStep<>(
                        "amount",
                        "advance.askAmount",
                        d -> d.amount,
                        (d,v) -> d.amount = v,
                        "date",
                        "note"
                ))
                .addStep(new TextInputStep<>(
                        "note",
                        "advance.askNote",
                        "noteInvalid",
                        d -> d.note,
                        (d,v) -> d.note = v,
                        "amount",
                        "confirm",
                        s -> null // без валидации
                ))
                .addStep(new ConfirmStep<>(
                        "confirm",
                        ctx -> {
                            var d = ctx.d;
                            String note = (d.note == null || d.note.isBlank())
                                    ? ctx.ui.msg(ctx.chatId, "common.none")
                                    : d.note;

                            return ""
                                    + ctx.ui.msg(ctx.chatId, "adv.confirm.title") + "\n"
                                    + ctx.ui.msg(ctx.chatId, "adv.confirm.date", d.date) + "\n"
                                    + ctx.ui.msg(ctx.chatId, "adv.confirm.amount", d.amount) + "\n"
                                    + ctx.ui.msg(ctx.chatId, "adv.confirm.note", note);
                        },
                        List.of(
                                new ConfirmStep.EditBtn("btnEditDate", "date"),
                                new ConfirmStep.EditBtn("btnEditAmount", "amount"),
                                new ConfirmStep.EditBtn("btnEditNote", "note")
                        ),
                        ctx -> { /* save */ },
                        ctx -> { /* cancel */ }
                ));

    }
}
