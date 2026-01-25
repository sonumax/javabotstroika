//package com.sonumax2.javabot.arhiv;
//
//import com.sonumax2.javabot.bot.commands.cb.AdvanceCb;
//import com.sonumax2.javabot.bot.flow.FlowContext;
//import com.sonumax2.javabot.bot.flow.FlowStep;
//import com.sonumax2.javabot.bot.flow.StepMove;
//import com.sonumax2.javabot.bot.ui.PanelMode;
//import com.sonumax2.javabot.domain.draft.AdvanceDraft;
//import com.sonumax2.javabot.util.InputParseUtils;
//
//import java.math.BigDecimal;
//
//public class AdvAmountStep implements FlowStep<AdvanceDraft> {
//
//    @Override
//    public String id() { return "amount"; }
//
//    @Override
//    public void show(FlowContext<AdvanceDraft> ctx, PanelMode mode) {
//        String back = ctx.d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.amountBack();
//
//        ctx.ui.panelKey(
//                ctx.chatId,
//                mode,
//                "advance.askAmount",
//                ctx.keyboard.backInline(ctx.chatId, back)
//        );
//    }
//
//    @Override
//    public StepMove onCallback(FlowContext<AdvanceDraft> ctx, String data, PanelMode mode) {
//        if (AdvanceCb.isAmountBackPick(data)) {
//            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
//            return StepMove.go("date");
//        }
//        if (AdvanceCb.isAmountErrorBackPick(data)) {
//            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
//            return StepMove.go("amount");
//        }
//        return StepMove.unhandled();
//    }
//
//    @Override
//    public StepMove onText(FlowContext<AdvanceDraft> ctx, String raw, PanelMode mode) {
//        BigDecimal amount = InputParseUtils.parseAmount(raw);
//
//        if (amount == null || amount.signum() <= 0) {
//            String back = ctx.d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.amountBack();
//            ctx.ui.panelKey(
//                    ctx.chatId,
//                    mode,
//                    "amountInvalid",
//                    ctx.keyboard.backInline(ctx.chatId, back)
//            );
//            return StepMove.rendered();
//        }
//
//        ctx.d.amount = amount;
//
//        if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
//
//        return StepMove.go("note");
//    }
//}
