//package com.sonumax2.javabot.arhiv;
//
//import com.sonumax2.javabot.bot.commands.cb.AdvanceCb;
//import com.sonumax2.javabot.bot.flow.FlowContext;
//import com.sonumax2.javabot.bot.flow.FlowStep;
//import com.sonumax2.javabot.bot.flow.StepMove;
//import com.sonumax2.javabot.bot.ui.PanelMode;
//import com.sonumax2.javabot.domain.draft.AdvanceDraft;
//
//import java.time.LocalDate;
//
//public class AdvDateStep implements FlowStep<AdvanceDraft> {
//
//    private static final String PREVIEW_MENU = "addOpr"; // CbParts.ADD_OPR
//
//    @Override
//    public String id() { return "date"; }
//
//    @Override
//    public void show(FlowContext<AdvanceDraft> ctx, PanelMode mode) {
//        String back = ctx.d.returnToConfirm ? AdvanceCb.confirmBack() : PREVIEW_MENU;
//
//        ctx.ui.panelKey(
//                ctx.chatId,
//                mode,
//                "advance.askDate",
//                ctx.keyboard.datePickerInline(ctx.chatId, AdvanceCb.NS, back)
//        );
//    }
//
//    @Override
//    public StepMove onCallback(FlowContext<AdvanceDraft> ctx, String data, PanelMode mode) {
//
//        // manual enter
//        if (AdvanceCb.isDateManualPick(data)) {
//            return StepMove.go("dateText");
//        }
//
//        // back from manual enter -> back to picker
//        if (AdvanceCb.isDateManualBackPick(data)) {
//            return StepMove.go("date");
//        }
//
//        // back from error -> back to manual input
//        if (AdvanceCb.isDateErrorBackPick(data)) {
//            return StepMove.go("dateText");
//        }
//
//        // yesterday shortcut
//        if (AdvanceCb.isDateYesterdayPick(data)) {
//            ctx.d.date = LocalDate.now().minusDays(1);
//            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
//            return StepMove.go("amount");
//        }
//
//        // today picked
//        if (AdvanceCb.isDate(data)) {
//            try {
//                ctx.d.date = LocalDate.now();
//                if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
//                return StepMove.go("amount");
//            } catch (Exception e) {
//                // если прилетела кривая дата — уйдём в ручной ввод
//                return StepMove.go("dateText");
//            }
//        }
//
//        return StepMove.unhandled();
//    }
//}
