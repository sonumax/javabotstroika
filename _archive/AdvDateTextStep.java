//package com.sonumax2.javabot.arhiv;
//
//import com.sonumax2.javabot.bot.commands.cb.AdvanceCb;
//import com.sonumax2.javabot.bot.flow.FlowContext;
//import com.sonumax2.javabot.bot.flow.FlowStep;
//import com.sonumax2.javabot.bot.flow.StepMove;
//import com.sonumax2.javabot.bot.ui.PanelMode;
//import com.sonumax2.javabot.domain.draft.AdvanceDraft;
//import com.sonumax2.javabot.util.DateParseResult;
//import com.sonumax2.javabot.util.InputParseUtils;
//
//import java.time.LocalDate;
//
//public class AdvDateTextStep implements FlowStep<AdvanceDraft> {
//
//    @Override
//    public String id() { return "dateText"; }
//
//    @Override
//    public void show(FlowContext<AdvanceDraft> ctx, PanelMode mode) {
//        ctx.ui.panelKey(
//                ctx.chatId,
//                mode,
//                "advance.enterDate",
//                ctx.keyboard.backInline(ctx.chatId, AdvanceCb.manualDateBack())
//        );
//    }
//
//    @Override
//    public StepMove onText(FlowContext<AdvanceDraft> ctx, String raw, PanelMode mode) {
//        DateParseResult res = InputParseUtils.parseSmartDate(raw, LocalDate.now());
//
//        if (res.error != DateParseResult.Error.NONE || res.date.isAfter(LocalDate.now())) {
//            String key = (res.error == DateParseResult.Error.NONE) ? "dateInFuture" : "dateInvalid";
//            ctx.ui.panelKey(
//                    ctx.chatId,
//                    mode,
//                    key,
//                    ctx.keyboard.backInline(ctx.chatId, AdvanceCb.errorDateBack())
//            );
//            return StepMove.rendered();
//        }
//
//        ctx.d.date = res.date;
//        if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
//
//        return StepMove.go("amount");
//    }
//}
