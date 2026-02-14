package com.sonumax2.javabot.bot.flow.exp;

import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.confirm.ConfirmDocUtil;
import com.sonumax2.javabot.bot.flow.confirm.ConfirmField;
import com.sonumax2.javabot.bot.flow.confirm.ConfirmRenderer;
import com.sonumax2.javabot.bot.flow.steps.*;
import com.sonumax2.javabot.bot.ui.PanelMode;
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

    // step ids
    private static final String S_OBJ = "obj";
    private static final String S_OBJ_SEARCH = "obj_search";

    private static final String S_ITEM = "item";
    private static final String S_ITEM_SEARCH = "item_search";

    private static final String S_CP = "cp";
    private static final String S_CP_SEARCH = "cp_search";

    private static final String S_AMOUNT = "amount";
    private static final String S_DATE = "date";

    private static final String S_DOC = "doc";
    private static final String S_DOC_FILE = "docFile";

    private static final String S_NOTE = "note";
    private static final String S_CONFIRM = FlowDefinition.STEP_CONFIRM;

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
                S_OBJ
        )
                .addStartCallback(Cb.makeCb(CbParts.ADD_OPR, NS))
                .addStartCommand("/expense")

                // -------- OBJECT --------
                .addStep(stepObject(workObjectService))
                .addStep(stepObjectSearch(workObjectService))

                // -------- NOMENCLATURE --------
                .addStep(stepItem(nomenclatureService, expenseService))
                .addStep(stepItemSearch(nomenclatureService))

                // -------- COUNTERPARTY --------
                .addStep(stepCounterparty(counterpartyService, expenseService))
                .addStep(stepCounterpartySearch(counterpartyService))

                // -------- AMOUNT / DATE --------
                .addStep(stepAmount())
                .addStep(stepDate())

                // -------- DOC TYPE + FILE --------
                .addStep(stepDocType())
                .addStep(stepDocFile())

                // -------- NOTE --------
                .addStep(stepNote())

                // -------- CONFIRM --------
                .addStep(stepConfirm(workObjectService, nomenclatureService, counterpartyService, expenseService));
    }

    // -------------------- steps --------------------

    private static SelectFromTopStep<ExpenseDraft, WorkObject> stepObject(WorkObjectService workObjectService) {
        return SelectFromTopStep.<ExpenseDraft, WorkObject>builder()
                .id(S_OBJ)
                .askKey("askObject")
                .options(ctx -> workObjectService.suggestByChat(ctx.chatId, 8))
                .bind(d -> d.objectId, (d, v) -> d.objectId = v)
                .onTextSaveTo((d, txt) -> d.pendingObjectName = txt)
                .backTo("@opsMenu")
                .nextTo(S_ITEM)
                .allowSkip(false)
                .textGoesTo(S_OBJ_SEARCH)
                .build();
    }

    private static SearchPickOrCreateRefStep<ExpenseDraft, WorkObject> stepObjectSearch(WorkObjectService workObjectService) {
        return SearchPickOrCreateRefStep.<ExpenseDraft, WorkObject>builder()
                .id(S_OBJ_SEARCH)
                .askKey("object.search.title")
                .pending(d -> d.pendingObjectName, (d, v) -> d.pendingObjectName = v)
                .saveIdTo((d, v) -> d.objectId = v)
                .exact((ctx, text) -> workObjectService.findExact(text))
                .search((ctx, text, lim) -> workObjectService.search(text, lim))
                .create((ctx, text) -> workObjectService.getOrCreate(text, ctx.chatId).getId())
                .backTo(S_OBJ)
                .nextTo(S_ITEM)
                .limit(8)
                .build();
    }

    private static SelectFromTopStep<ExpenseDraft, Nomenclature> stepItem(
            NomenclatureService nomenclatureService,
            ExpenseService expenseService
    ) {
        return SelectFromTopStep.<ExpenseDraft, Nomenclature>builder()
                .id(S_ITEM)
                .askKey("nomenclature.choseOrAdd")
                .options(ctx -> {
                    List<Nomenclature> byOps = nomenclatureService.loadActiveInOrder(
                            expenseService.suggestNomenclatureIds(ctx.chatId, 8)
                    );
                    if (byOps.size() >= 8) return byOps;

                    return mergeById(
                            byOps,
                            nomenclatureService.suggestByChat(ctx.chatId, 8),
                            8
                    );
                })
                .bind(d -> d.nomenclatureId, (d, v) -> d.nomenclatureId = v)
                .onTextSaveTo((d, txt) -> d.pendingNomenclatureName = txt)
                .backTo(S_OBJ)
                .nextTo(S_CP)
                .allowSkip(false)
                .textGoesTo(S_ITEM_SEARCH)
                .build();
    }

    private static SearchPickOrCreateRefStep<ExpenseDraft, Nomenclature> stepItemSearch(NomenclatureService nomenclatureService) {
        return SearchPickOrCreateRefStep.<ExpenseDraft, Nomenclature>builder()
                .id(S_ITEM_SEARCH)
                .askKey("nomenclature.search.title")
                .pending(d -> d.pendingNomenclatureName, (d, v) -> d.pendingNomenclatureName = v)
                .saveIdTo((d, v) -> d.nomenclatureId = v)
                .exact((ctx, text) -> nomenclatureService.findExact(text))
                .search((ctx, text, lim) -> nomenclatureService.search(text, lim))
                .create((ctx, text) -> nomenclatureService.getOrCreate(text, ctx.chatId).getId())
                .backTo(S_ITEM)
                .nextTo(S_CP)
                .limit(8)
                .build();
    }

    private static SelectFromTopStep<ExpenseDraft, Counterparty> stepCounterparty(
            CounterpartyService counterpartyService,
            ExpenseService expenseService
    ) {
        return SelectFromTopStep.<ExpenseDraft, Counterparty>builder()
                .id(S_CP)
                .askKey("expense.askCounterparty")
                .options(ctx -> {
                    if (ctx.d.nomenclatureId != null) {
                        return expenseService.suggestCounterparty(ctx.chatId, ctx.d.nomenclatureId, 8);
                    }
                    return counterpartyService.suggestByChat(ctx.chatId, 8);
                })
                .bind(d -> d.counterpartyId, (d, v) -> d.counterpartyId = v)
                .onTextSaveTo((d, txt) -> d.pendingCounterpartyName = txt)
                .backTo(S_ITEM)
                .nextTo(S_AMOUNT)
                .allowSkip(true)
                .textGoesTo(S_CP_SEARCH)
                .build();
    }

    private static SearchPickOrCreateRefStep<ExpenseDraft, Counterparty> stepCounterpartySearch(
            CounterpartyService counterpartyService
    ) {
        return SearchPickOrCreateRefStep.<ExpenseDraft, Counterparty>builder()
                .id(S_CP_SEARCH)
                .askKey("expense.askCounterparty")
                .pending(d -> d.pendingCounterpartyName, (d, v) -> d.pendingCounterpartyName = v)
                .saveIdTo((d, v) -> d.counterpartyId = v)
                .exact((ctx, text) -> counterpartyService.findExact(ctx.d.counterpartyKind, text))
                .search((ctx, text, lim) -> counterpartyService.search(ctx.d.counterpartyKind, text, lim))
                .create((ctx, text) -> counterpartyService.getOrCreate(text, ctx.d.counterpartyKind, ctx.chatId).getId())
                .backTo(S_CP)
                .nextTo(S_AMOUNT)
                .limit(8)
                .build();
    }

    private static AmountInputStep<ExpenseDraft> stepAmount() {
        return new AmountInputStep<>(
                S_AMOUNT,
                "expense.askAmount",
                d -> d.amount,
                (d, v) -> d.amount = v,
                S_CP,
                S_DATE
        );
    }

    private static DateInputStep<ExpenseDraft> stepDate() {
        return new DateInputStep<>(
                S_DATE,
                "expense.askDate",
                d -> d.date,
                (d, v) -> d.date = v,
                S_AMOUNT,
                S_DOC
        );
    }

    private static DocTypeSelectStep<ExpenseDraft> stepDocType() {
        return new DocTypeSelectStep<>(
                S_DOC,
                "receipt.ask",
                d -> d.docType,
                (d, v) -> d.docType = v,
                (d, v) -> {
                    if (v == DocType.NO_RECEIPT) d.docFileId = null;
                },
                S_DATE,
                S_DOC_FILE,
                S_NOTE
        );
    }

    private static FileInputStep<ExpenseDraft> stepDocFile() {
        return new FileInputStep<>(
                S_DOC_FILE,
                "receipt.askFile",
                d -> d.docFileId,
                (d, v) -> d.docFileId = v,
                S_DOC,
                S_NOTE,
                true
        );
    }

    private static TextInputStep<ExpenseDraft> stepNote() {
        return new TextInputStep<>(
                S_NOTE,
                "expense.askNote",
                "noteInvalid",
                d -> d.note,
                (d, v) -> d.note = v,
                S_DOC,
                S_CONFIRM,
                true,
                S_CONFIRM,
                s -> null
        );
    }

    private static ConfirmStep<ExpenseDraft> stepConfirm(
            WorkObjectService workObjectService,
            NomenclatureService nomenclatureService,
            CounterpartyService counterpartyService,
            ExpenseService expenseService
    ) {
        return ConfirmStep.<ExpenseDraft>builder()
            .id(S_CONFIRM)
                .render(ctx -> {
                    String obj = ctx.d.objectId == null
                            ? null
                            : workObjectService.findActiveById(ctx.d.objectId)
                            .map(WorkObject::getName)
                            .orElse(null);

                    String item = ctx.d.nomenclatureId == null
                            ? null
                            : nomenclatureService.findActiveById(ctx.d.nomenclatureId)
                            .map(Nomenclature::getName)
                            .orElse(null);

                    String cp = ctx.d.counterpartyId == null
                            ? null
                            : counterpartyService.findActiveById(ctx.d.counterpartyId)
                            .map(Counterparty::getName)
                            .orElse(null);

                    String doc = ConfirmDocUtil.buildDocWithFileMark(ctx, ctx.d.docType, ctx.d.docFileId);

                    return ConfirmRenderer.render(
                            ctx,
                            k -> ctx.ui().msg(ctx.chatId, k),
                            List.of(
                                    ConfirmField.of("confirm.object", c -> obj),
                                    ConfirmField.of("confirm.item",   c -> item),
                                    ConfirmField.of("confirm.cp",     c -> cp),
                                    ConfirmField.of("confirm.amount", c -> c.d.amount == null ? null : c.d.amount.toString()),
                                    ConfirmField.of("confirm.date",   c -> c.d.date == null ? null : c.d.date.toString()),
                                    ConfirmField.of("confirm.doc",    c -> doc),
                                    ConfirmField.of("confirm.note",   c -> (c.d.note == null || c.d.note.isBlank()) ? null : c.d.note)
                            ),
                            "common.none"
                    );
                })

                .editsProvider(ctx -> {
                List<ConfirmStep.EditBtn> b = new ArrayList<>();
                b.add(new ConfirmStep.EditBtn("btnEditObject", S_OBJ));
                b.add(new ConfirmStep.EditBtn("btnEditItem", S_ITEM));
                b.add(new ConfirmStep.EditBtn("btnEditCp", S_CP));
                b.add(new ConfirmStep.EditBtn("btnEditAmount", S_AMOUNT));
                b.add(new ConfirmStep.EditBtn("btnEditDate", S_DATE));
                b.add(new ConfirmStep.EditBtn("btnEditDoc", S_DOC));
                if (ctx.d.docType != null && ctx.d.docType.needsFile()) {
                    boolean hasFile = ctx.d.docFileId != null && !ctx.d.docFileId.isBlank();
                    String key = hasFile ? "btnReplaceFile" : "btnAttachFile";
                    b.add(new ConfirmStep.EditBtn(key, S_DOC_FILE));
                }
                b.add(new ConfirmStep.EditBtn("btnEditNote", S_NOTE));
                return b;
            })
            .allowSave(ctx -> {
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

                ctx.ui().panelKey(
                        ctx.chatId,
                        PanelMode.EDIT,
                        "expense.saved",
                        ctx.keyboard().mainMenuInline(ctx.chatId),
                        ctx.session().displayName(ctx.chatId),
                        d.amount,
                        d.date
                );
            })
            .allowCancel(ctx -> ctx.ui().panelKey(
                    ctx.chatId,
                    PanelMode.EDIT,
                    "cancelled",
                    ctx.keyboard().mainMenuInline(ctx.chatId)
            )).build();
    }

    // -------------------- helpers --------------------

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
