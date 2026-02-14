package com.sonumax2.javabot.bot.flow.adv;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.confirm.ConfirmField;
import com.sonumax2.javabot.bot.flow.confirm.ConfirmRenderer;
import com.sonumax2.javabot.bot.flow.steps.AmountInputStep;
import com.sonumax2.javabot.bot.flow.steps.ConfirmStep;
import com.sonumax2.javabot.bot.flow.steps.DateInputStep;
import com.sonumax2.javabot.bot.flow.steps.TextInputStep;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.AdvanceDraft;
import com.sonumax2.javabot.domain.draft.DraftType;
import com.sonumax2.javabot.domain.operation.Operation;
import com.sonumax2.javabot.domain.operation.OperationType;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import com.sonumax2.javabot.domain.reference.WorkObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class AdvanceFlowConfig {

    private static final String NS = CbParts.ADV; // "adv"

    @Bean
    public FlowDefinition<AdvanceDraft> advanceFlow(OperationRepository operationRepository) {

        return new FlowDefinition<>(
                NS,
                DraftType.ADVANCE,
                AdvanceDraft.class,
                "date"
        )
                .addStartCallback(Cb.makeCb(CbParts.ADD_OPR, NS))
                .addStartCommand("/advance")

                .addStep(new DateInputStep<>(
                        "date",
                        "advance.askDate",
                        d -> d.date,
                        (d, v) -> d.date = v,
                        null,
                        "amount"
                ))

                .addStep(new AmountInputStep<>(
                        "amount",
                        "advance.askAmount",
                        d -> d.amount,
                        (d, v) -> d.amount = v,
                        "date",
                        "note"
                ))

                .addStep(new TextInputStep<>(
                        "note",
                        "advance.askNote",
                        "noteInvalid",
                        d -> d.note,
                        (d, v) -> d.note = v,
                        "amount",
                        FlowDefinition.STEP_CONFIRM,
                        true,
                        FlowDefinition.STEP_CONFIRM,
                        s -> null
                ))

                .addStep(new ConfirmStep<>(
                        FlowDefinition.STEP_CONFIRM,
                        ctx -> ConfirmRenderer.render(
                                ctx,
                                k -> ctx.ui().msg(ctx.chatId, k),
                                List.of(
                                        ConfirmField.of("confirm.date",
                                                c -> c.d.date == null ? null : c.d.date.toString()
                                        ),
                                        ConfirmField.of("confirm.amount",
                                                c -> c.d.amount == null ? null : c.d.amount.toString()
                                                ),
                                        ConfirmField.of("confirm.note",
                                                c -> (c.d.note == null || c.d.note.isBlank()) ? null : c.d.note
                                        )
                                ),
                                "common.none"
                        ),
                        List.of(
                                new ConfirmStep.EditBtn("btnEditDate", "date"),
                                new ConfirmStep.EditBtn("btnEditAmount", "amount"),
                                new ConfirmStep.EditBtn("btnEditNote", "note")
                        ),
                        ctx -> {
                            var d = ctx.d;

                            Operation op = new Operation();
                            op.setChatId(ctx.chatId);
                            op.setOpType(OperationType.ADVANCE);
                            op.setOpDate(d.date);
                            op.setAmount(d.amount);
                            op.setNote(d.note);

                            operationRepository.save(op);

                            ctx.ui().panelKey(
                                    ctx.chatId,
                                    PanelMode.EDIT,
                                    "advance.saved",
                                    ctx.keyboard().mainMenuInline(ctx.chatId),
                                    ctx.session().displayName(ctx.chatId),
                                    d.amount,
                                    d.date
                            );
                        },
                        ctx -> {
                            ctx.ui().panelKey(
                                    ctx.chatId,
                                    PanelMode.EDIT,
                                    "cancelled",
                                    ctx.keyboard().mainMenuInline(ctx.chatId)
                            );
                        }
                ));
    }
}
