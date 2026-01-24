package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.util.DateParseResult;
import com.sonumax2.javabot.util.InputParseUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DateInputStep<D extends OpDraftBase> implements FlowStep<D> {

    private final String id;
    private final String askKey;
    private final String invalidKey = "dateInvalid";
    private final String futureKey = "dateInFuture";

    private final Function<D, LocalDate> getter;
    private final BiConsumer<D, LocalDate> setter;

    private final String prevStepId;
    private final String nextStepId;

    public DateInputStep(
            String id,
            String askKey,
            Function<D, LocalDate> getter,
            BiConsumer<D, LocalDate> setter,
            String prevStepId,
            String nextStepId
    ) {
        this.id = id;
        this.askKey = askKey;
        this.getter = getter;
        this.setter = setter;
        this.prevStepId = prevStepId;
        this.nextStepId = nextStepId;
    }

    @Override
    public String id() { return id; }

    @Override
    public void show(FlowContext<D> ctx, PanelMode mode) {
        ctx.ui.panelKey(ctx.chatId, mode, askKey, kb(ctx));
    }

    @Override
    public StepMove onCallback(FlowContext<D> ctx, String data, PanelMode mode) {
        String ns = ctx.def.ns;

        if (FlowCb.is(data, ns, id, "back")) {
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(prevStepId);
        }

        if (FlowCb.is(data, ns, id, "today")) {
            setter.accept(ctx.d, LocalDate.now());
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(nextStepId);
        }

        if (FlowCb.is(data, ns, id, "yesterday")) {
            setter.accept(ctx.d, LocalDate.now().minusDays(1));
            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(nextStepId);
        }

        if (FlowCb.startsWith(data, ns, id, "pick")) {
            try {
                String iso = FlowCb.tail(data, ns, id, "pick");
                LocalDate d = LocalDate.parse(iso);
                setter.accept(ctx.d, d);
                if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
                return StepMove.go(nextStepId);
            } catch (Exception ignore) {
                ctx.ui.panelKey(ctx.chatId, mode, invalidKey, kb(ctx));
                return StepMove.rendered();
            }
        }

        return StepMove.unhandled();
    }

    @Override
    public StepMove onText(FlowContext<D> ctx, String raw, PanelMode mode) {
        DateParseResult res = InputParseUtils.parseSmartDate(raw, LocalDate.now());

        if (res.error != DateParseResult.Error.NONE) {
            ctx.ui.panelKey(ctx.chatId, mode, invalidKey, kb(ctx));
            return StepMove.rendered();
        }
        if (res.date.isAfter(LocalDate.now())) {
            ctx.ui.panelKey(ctx.chatId, mode, futureKey, kb(ctx));
            return StepMove.rendered();
        }

        setter.accept(ctx.d, res.date);
        if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
        return StepMove.go(nextStepId);
    }

    private InlineKeyboardMarkup kb(FlowContext<D> ctx) {
        String ns = ctx.def.ns;

        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(new InlineKeyboardRow(
                btn(ctx, "date.today", FlowCb.cb(ns, id, "today")),
                btn(ctx, "date.yesterday", FlowCb.cb(ns, id, "yesterday"))
        ));

        // если хочешь календарь — делай pick:YYYY-MM-DD кнопками снаружи или отдельным step-ом.
        // Тут минимально.

        rows.add(new InlineKeyboardRow(btn(ctx, "back", FlowCb.cb(ns, id, "back"))));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton btn(FlowContext<D> ctx, String textKey, String cb) {
        return InlineKeyboardButton.builder()
                .text(ctx.ui.msg(ctx.chatId, textKey))
                .callbackData(cb)
                .build();
    }
}
