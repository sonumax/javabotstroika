package com.sonumax2.javabot.bot.flow.exp;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.steps.*;
import com.sonumax2.javabot.domain.draft.DraftType;
import com.sonumax2.javabot.domain.draft.ExpenseDraft;
import com.sonumax2.javabot.domain.operation.DocType;
import com.sonumax2.javabot.domain.operation.service.ExpenseService;
import com.sonumax2.javabot.domain.reference.BaseRefEntity;
import com.sonumax2.javabot.domain.reference.Counterparty;
import com.sonumax2.javabot.domain.reference.Nomenclature;
import com.sonumax2.javabot.domain.reference.WorkObject;
import com.sonumax2.javabot.domain.reference.service.CounterpartyService;
import com.sonumax2.javabot.domain.reference.service.NomenclatureService;
import com.sonumax2.javabot.domain.reference.service.WorkObjectService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class ExpenseFlowConfig {

    public static final String NS = "exp";

    @Bean
    public FlowDefinition<ExpenseDraft> expenseFlow(
            WorkObjectService workObjectService,
            NomenclatureService nomenclatureService,
            CounterpartyService counterpartyService,
            ExpenseService expenseService
    ) {
        return new FlowDefinition<>(
                NS,
                DraftType.EXPENSE,
                ExpenseDraft.class,
                "obj"
        )
                .addStartCallback(Cb.makeCb(CbParts.ADD_OPR, NS))
                .addStartCommand("/expense")

                // -------- OBJECT --------
                .addStep(new SelectFromTopStep<>(
                        "obj",
                        "askObject",
                        ctx -> mergeById(
                                workObjectService.recentByChat(ctx.chatId, 8),
                                workObjectService.listActiveTop50(),
                                8
                        ),
                        d -> d.objectId,
                        (d, v) -> d.objectId = v,
                        (d, txt) -> d.pendingObjectName = txt,
                        "@opsMenu",
                        "item",
                        false,
                        "obj_search"
                ))
                .addStep(new SearchPickOrCreateRefStep<>(
                        "obj_search",
                        "object.search.title",
                        d -> d.pendingObjectName,
                        (d, v) -> d.pendingObjectName = v,
                        (d, v) -> d.objectId = v,
                        (ctx, text) -> workObjectService.findExact(text),
                        (ctx, text, lim) -> workObjectService.search(text, lim),
                        (ctx, text) -> workObjectService.getOrCreate(text, ctx.chatId).getId(),
                        "obj",
                        "item",
                        8
                ))

                // -------- NOMENCLATURE --------
                .addStep(new SelectFromTopStep<>(
                        "item",
                        "nomenclature.choseOrAdd",
                        ctx -> {
                            List<Nomenclature> byOps = nomenclatureService.loadActiveInOrder(
                                    expenseService.suggestNomenclatureIds(ctx.chatId, 8)
                            );
                            if (byOps.size() >= 8) return byOps;

                            return mergeById(
                                    byOps,
                                    mergeById(
                                            nomenclatureService.recentByChat(ctx.chatId, 8),
                                            nomenclatureService.listActiveTop50(),
                                            8
                                    ),
                                    8
                            );
                        },
                        d -> d.nomenclatureId,
                        (d, v) -> d.nomenclatureId = v,
                        (d, txt) -> d.pendingNomenclatureName = txt,
                        "obj",
                        "cp",
                        false,
                        "item_search"
                ))
                .addStep(new SearchPickOrCreateRefStep<>(
                        "item_search",
                        "nomenclature.search.title",
                        d -> d.pendingNomenclatureName,
                        (d, v) -> d.pendingNomenclatureName = v,
                        (d, v) -> d.nomenclatureId = v,
                        (ctx, text) -> nomenclatureService.findExact(text),
                        (ctx, text, lim) -> nomenclatureService.search(text, lim),
                        (ctx, text) -> nomenclatureService.getOrCreate(text, ctx.chatId).getId(),
                        "item",
                        "cp",
                        8
                ))

                // -------- COUNTERPARTY --------
                .addStep(
                        SelectFromTopStep.<ExpenseDraft, Counterparty>builder()
                                .id("cp")
                                .askKey("counterparty.search.title")
                                .options(ctx -> {
                                    if (ctx.d.nomenclatureId != null) {
                                        return expenseService.suggestCounterparty(ctx.chatId, ctx.d.nomenclatureId, 8);
                                    }
                                    return mergeById(
                                            counterpartyService.recentByChat(ctx.chatId, 8),
                                            counterpartyService.listActiveTop50(),
                                            8
                                    );
                                })
                                .bind(d -> d.counterpartyId, (d, v) -> d.counterpartyId = v)
                                .onTextSaveTo((d, txt) -> d.pendingCounterpartyName = txt)
                                .backTo("item")
                                .nextTo("amount")
                                .allowSkip(true)
                                .textGoesTo("cp_search")
                                .build()
                )
                .addStep(
                        SearchPickOrCreateRefStep.<ExpenseDraft, Counterparty>builder()
                                .id("cp_search")
                                .askKey("counterparty.search.title")
                                .pending(d -> d.pendingCounterpartyName, (d, v) -> d.pendingCounterpartyName = v)
                                .saveIdTo((d, v) -> d.counterpartyId = v)
                                .exact((ctx, text) -> counterpartyService.findExact(ctx.d.counterpartyKind, text))
                                .search((ctx, text, lim) -> counterpartyService.search(ctx.d.counterpartyKind, text, lim))
                                .create((ctx, text) -> counterpartyService.getOrCreate(text, ctx.d.counterpartyKind, ctx.chatId).getId())
                                .backTo("cp")
                                .nextTo("amount")
                                .limit(8)
                                .build()
                )


                // -------- AMOUNT / DATE --------
                .addStep(new AmountInputStep<>(
                        "amount",
                        "expense.askAmount",
                        d -> d.amount,
                        (d, v) -> d.amount = v,
                        "cp",
                        "date"
                ))
                .addStep(new DateInputStep<>(
                        "date",
                        "expense.askDate",
                        d -> d.date,
                        (d, v) -> d.date = v,
                        "amount",
                        "doc"
                ))

                // -------- DOC TYPE + FILE --------
                .addStep(new DocTypeSelectStep<>(
                        "doc",
                        "receipt.ask",
                        d -> d.docType,
                        (d, v) -> d.docType = v,
                        (d, v) -> {
                            if (v == DocType.NO_RECEIPT) d.docFileId = null;
                        },
                        "date",
                        "docFile",
                        "note"
                ))
                .addStep(new FileInputStep<>(
                        "docFile",
                        "receipt.askFile",
                        d -> d.docFileId,
                        (d, v) -> d.docFileId = v,
                        "doc",
                        "note",
                        true
                ))

                // -------- NOTE --------
                .addStep(new TextInputStep<>(
                        "note",
                        "expense.askNote",
                        "expense.askNote",
                        d -> d.note,
                        (d, v) -> d.note = v,
                        "doc",
                        "confirm",
                        true,
                        "confirm",
                        s -> null
                ))

                // -------- CONFIRM --------
                .addStep(new ConfirmStep<>(
                        "confirm",
                        ctx -> renderConfirm(ctx, workObjectService, nomenclatureService, counterpartyService),
                        ctx -> {
                            List<ConfirmStep.EditBtn> b = new ArrayList<>();
                            b.add(new ConfirmStep.EditBtn("btnEditObject", "obj"));
                            b.add(new ConfirmStep.EditBtn("btnEditItem", "item"));
                            b.add(new ConfirmStep.EditBtn("btnEditCp", "cp"));
                            b.add(new ConfirmStep.EditBtn("btnEditAmount", "amount"));
                            b.add(new ConfirmStep.EditBtn("btnEditDate", "date"));
                            b.add(new ConfirmStep.EditBtn("btnEditDoc", "doc"));
                            if (ctx.d.docType != null && ctx.d.docType.needsFile()) {
                                b.add(new ConfirmStep.EditBtn("btnAttachFile", "docFile")); // или свой key
                            }
                            b.add(new ConfirmStep.EditBtn("btnEditNote", "note"));
                            return b;
                        },
                        ctx -> {
                            var d = ctx.d;

                            expenseService.saveExpense(
                                    ctx.chatId,
                                    d.objectId,
                                    d.nomenclatureId,
                                    d.counterpartyId,
                                    d.docType,
                                    d.amount,
                                    d.date,
                                    d.note,
                                    d.docFileId
                            );

                            ctx.ui.panelKey(
                                    ctx.chatId,
                                    com.sonumax2.javabot.bot.ui.PanelMode.EDIT,
                                    "expense.saved",
                                    ctx.keyboard.mainMenuInline(ctx.chatId),
                                    ctx.session.displayName(ctx.chatId),
                                    d.amount,
                                    d.date
                            );
                        },
                        ctx -> ctx.ui.panelKey(
                                ctx.chatId,
                                com.sonumax2.javabot.bot.ui.PanelMode.EDIT,
                                "cancelled",
                                ctx.keyboard.mainMenuInline(ctx.chatId)
                        )
                ));
    }

    private static String renderConfirm(
            com.sonumax2.javabot.bot.flow.FlowContext<ExpenseDraft> ctx,
            WorkObjectService workObjectService,
            NomenclatureService nomenclatureService,
            CounterpartyService counterpartyService
    ) {
        var d = ctx.d;

        String none = ctx.ui.msg(ctx.chatId, "common.none");

        String obj = workObjectService.findActiveById(d.objectId).map(WorkObject::getName).orElse(none);
        String item = nomenclatureService.findActiveById(d.nomenclatureId).map(Nomenclature::getName).orElse(none);
        String cp = (d.counterpartyId == null)
                ? none
                : counterpartyService.findActiveById(d.counterpartyId).map(Counterparty::getName).orElse(none);

        String note = (d.note == null || d.note.isBlank()) ? none : d.note;

        String docLabel = switch (d.docType == null ? DocType.NO_RECEIPT : d.docType) {
            case RECEIPT -> ctx.ui.msg(ctx.chatId, "expense.doc.receipt");
            case INVOICE -> ctx.ui.msg(ctx.chatId, "expense.doc.invoice");
            case NO_RECEIPT -> ctx.ui.msg(ctx.chatId, "expense.doc.none");
        };

        String fileMark = "";
        DocType dt = (d.docType == null ? DocType.NO_RECEIPT : d.docType);
        if (dt.needsFile()) {
            fileMark = " " + ctx.ui.msg(ctx.chatId,
                    (d.docFileId != null && !d.docFileId.isBlank()) ? "expense.doc.file.ok" : "expense.doc.file.miss"
            );
        }

        String doc = docLabel + fileMark;

        return ctx.ui.msg(
                ctx.chatId,
                "expense.confirm",
                obj,
                item,
                cp,
                d.amount,
                d.date,
                doc,
                note
        );
    }

    private static <T extends BaseRefEntity> List<T> mergeById(List<T> a, List<T> b, int limit) {
        if (limit <= 0) return List.of();

        LinkedHashMap<Long, T> map = new LinkedHashMap<>();

        if (a != null) {
            for (T x : a) {
                if (x == null || x.getId() == null) continue;
                map.putIfAbsent(x.getId(), x);
                if (map.size() >= limit) return new ArrayList<>(map.values());
            }
        }

        if (b != null) {
            for (T x : b) {
                if (x == null || x.getId() == null) continue;
                map.putIfAbsent(x.getId(), x);
                if (map.size() >= limit) break;
            }
        }

        return new ArrayList<>(map.values());
    }
}
