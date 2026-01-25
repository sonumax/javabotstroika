package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowStep;
import com.sonumax2.javabot.bot.flow.StepMove;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.domain.reference.BaseRefEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SelectFromTopStep<D extends OpDraftBase, T extends BaseRefEntity> implements FlowStep<D> {

    private final String id;
    private final String askKey;

    // было Supplier<List<T>>
    private final Function<FlowContext<D>, List<T>> itemsProvider;

    private final Function<D, Long> getter;
    private final BiConsumer<D, Long> setter;

    private final String prevStepId;
    private final String nextStepId;

    private final String newTextStepId;

    public SelectFromTopStep(
            String id,
            String askKey,
            Function<FlowContext<D>, List<T>> itemsProvider,
            Function<D, Long> getter,
            BiConsumer<D, Long> setter,
            String prevStepId,
            String nextStepId,
            String newTextStepId
    ) {
        this.id = id;
        this.askKey = askKey;
        this.itemsProvider = itemsProvider;
        this.getter = getter;
        this.setter = setter;
        this.prevStepId = prevStepId;
        this.nextStepId = nextStepId;
        this.newTextStepId = newTextStepId;
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

            // спец: вернуться в меню выбора операции
            if ("@opsMenu".equals(prevStepId)) {
                ctx.ui.panelKey(
                        ctx.chatId,
                        mode,
                        "menu.opr",
                        ctx.keyboard.operationsAddMenuInline(ctx.chatId, CbParts.ADD_OPR, CbParts.MENU)
                );
                return StepMove.finish();
            }

            // если это первый шаг, лучше отмена, чем go(null)
            if (prevStepId == null || prevStepId.isBlank()) {
                ctx.ui.panelKey(ctx.chatId, mode, "cancelled", ctx.keyboard.mainMenuInline(ctx.chatId));
                return StepMove.finish();
            }

            return StepMove.go(prevStepId);
        }

        if (FlowCb.is(data, ns, id, "new")) {
            return StepMove.go(newTextStepId);
        }

        if (FlowCb.startsWith(data, ns, id, "pick")) {
            long picked = FlowCb.tailLong(data, ns, id, "pick");
            setter.accept(ctx.d, picked);

            if (ctx.d.consumeReturnToConfirm()) return StepMove.go("confirm");
            return StepMove.go(nextStepId);
        }

        return StepMove.unhandled();
    }

    private InlineKeyboardMarkup kb(FlowContext<D> ctx) {
        String ns = ctx.def.ns;

        List<T> list = itemsProvider.apply(ctx);
        if (list == null) list = List.of();

        List<InlineKeyboardRow> rows = new ArrayList<>();

        // по 2 в ряд
        for (int i = 0; i < list.size(); i += 2) {
            T a = list.get(i);
            if (i + 1 < list.size()) {
                T b = list.get(i + 1);
                rows.add(new InlineKeyboardRow(
                        refBtn(ctx, a.getName(), FlowCb.cb(ns, id, "pick", a.getId())),
                        refBtn(ctx, b.getName(), FlowCb.cb(ns, id, "pick", b.getId()))
                ));
            } else {
                rows.add(new InlineKeyboardRow(
                        refBtn(ctx, a.getName(), FlowCb.cb(ns, id, "pick", a.getId()))
                ));
            }
        }

        rows.add(new InlineKeyboardRow(btn(ctx, "add", FlowCb.cb(ns, id, "new"))));
        rows.add(new InlineKeyboardRow(btn(ctx, "back", FlowCb.cb(ns, id, "back"))));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton btn(FlowContext<D> ctx, String textKey, String cb) {
        return InlineKeyboardButton.builder()
                .text(ctx.ui.msg(ctx.chatId, textKey))
                .callbackData(cb)
                .build();
    }

    private InlineKeyboardButton refBtn(FlowContext<D> ctx, String text, String cb) {
        return InlineKeyboardButton.builder()
                .text(cut(text))
                .callbackData(cb)
                .build();
    }

    private String cut(String s) {
        if (s == null) return "";
        return s.length() <= 50 ? s : s.substring(0, 49) + "…";
    }
}
