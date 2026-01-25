//package com.sonumax2.javabot.arhiv;
//
//import com.sonumax2.javabot.bot.commands.cb.AdvanceCb;
//import com.sonumax2.javabot.bot.flow.FlowContext;
//import com.sonumax2.javabot.bot.flow.FlowStep;
//import com.sonumax2.javabot.bot.flow.StepMove;
//import com.sonumax2.javabot.bot.ui.PanelMode;
//import com.sonumax2.javabot.domain.draft.AdvanceDraft;
//
//public class AdvNoteStep implements FlowStep<AdvanceDraft> {
//
//    @Override
//    public String id() { return "note"; }
//
//    @Override
//    public void show(FlowContext<AdvanceDraft> ctx, PanelMode mode) {
//        String back = ctx.d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.noteBack();
//
//        ctx.ui.panelKey(
//                ctx.chatId,
//                mode,
//                "advance.askNote",
//                ctx.keyboard.skipInline(ctx.chatId, AdvanceCb.noteSkip(), back)
//        );
//    }
//
//    @Override
//    public StepMove onCallback(FlowContext<AdvanceDraft> ctx, String data, PanelMode mode) {
//        if (AdvanceCb.isNoteSkipPick(data)) {
//            ctx.d.note = null;
//            ctx.d.returnToConfirm = false;
//            return StepMove.go("confirm");
//        }
//
//        if (AdvanceCb.isNoteBackPick(data)) {
//            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
//            return StepMove.go("amount");
//        }
//
//        return StepMove.unhandled();
//    }
//
//    @Override
//    public StepMove onText(FlowContext<AdvanceDraft> ctx, String raw, PanelMode mode) {
//        String note = (raw == null) ? null : raw.trim();
//        ctx.d.note = (note == null || note.isEmpty()) ? null : note;
//
//        ctx.d.returnToConfirm = false;
//
//        return StepMove.go("confirm");
//    }
//}
