//package com.sonumax2.javabot.arhiv;
//
//import com.sonumax2.javabot.bot.commands.cb.AdvanceCb;
//import com.sonumax2.javabot.bot.flow.FlowContext;
//import com.sonumax2.javabot.bot.flow.FlowStep;
//import com.sonumax2.javabot.bot.flow.StepMove;
//import com.sonumax2.javabot.bot.ui.PanelMode;
//import com.sonumax2.javabot.domain.draft.AdvanceDraft;
//import com.sonumax2.javabot.domain.operation.Operation;
//import com.sonumax2.javabot.domain.operation.OperationType;
//import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
//
//import java.time.LocalDateTime;
//
//public class AdvConfirmStep implements FlowStep<AdvanceDraft> {
//
//    private final OperationRepository operationRepository;
//
//    public AdvConfirmStep(OperationRepository operationRepository) {
//        this.operationRepository = operationRepository;
//    }
//
//    @Override
//    public String id() { return "confirm"; }
//
//    @Override
//    public void show(FlowContext<AdvanceDraft> ctx, PanelMode mode) {
//        AdvanceDraft d = ctx.d;
//
//        ctx.ui.panelKey(
//                ctx.chatId,
//                mode,
//                "adv.confirm",
//                ctx.keyboard.confirmInline(ctx.chatId, AdvanceCb.NS),
//                d.amount,
//                d.date,
//                (d.note == null ? "—" : d.note)
//        );
//    }
//
//    @Override
//    public StepMove onCallback(FlowContext<AdvanceDraft> ctx, String data, PanelMode mode) {
//
//        if (!AdvanceCb.isConfirmPick(data)) return StepMove.unhandled();
//        String action = AdvanceCb.getConfirmAction(data);
//
//        switch (action) {
//            case "editDate" -> { ctx.d.returnToConfirm = true; return StepMove.go("date"); }
//            case "editAmount" -> { ctx.d.returnToConfirm = true; return StepMove.go("amount"); }
//            case "editNote" -> { ctx.d.returnToConfirm = true; return StepMove.go("note"); }
//            case "back" -> { return StepMove.stay(); }
//
//            case "cancel" -> {
//                ctx.drafts.clear(ctx.chatId, ctx.draftType);
//                ctx.d.step = null;
//                ctx.ui.panelKey(ctx.chatId, mode, "cancelled", ctx.keyboard.mainMenuInline(ctx.chatId));
//                return StepMove.finish();
//            }
//
//            case "save" -> {
//                AdvanceDraft d = ctx.d;
//
//                if (d.date == null) return StepMove.go("date");
//                if (d.amount == null) return StepMove.go("amount");
//
//                Operation op = new Operation();
//                op.setChatId(ctx.chatId);
//                op.setOpType(OperationType.ADVANCE);
//                op.setOpDate(d.date);
//                op.setAmount(d.amount);
//                op.setNote(d.note);
//                op.setCreatedAt(LocalDateTime.now());
//
//                operationRepository.save(op);
//
//                String name = ctx.session.displayName(ctx.chatId);
//
//                ctx.d.step = null;
//
//                ctx.ui.panelKey(
//                        ctx.chatId,
//                        mode,
//                        "advance.saved",
//                        ctx.keyboard.mainMenuInline(ctx.chatId),
//                        name, op.getAmount(), op.getOpDate()
//                );
//                return StepMove.finish();
//            }
//        }
//
//        return StepMove.unhandled();
//    }
//}
