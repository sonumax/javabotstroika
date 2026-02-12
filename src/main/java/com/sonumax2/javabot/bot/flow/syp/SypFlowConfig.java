package com.sonumax2.javabot.bot.flow.syp;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.flow.ConfirmSupport;
import com.sonumax2.javabot.bot.flow.FlowContext;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.steps.*;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.DraftType;
import com.sonumax2.javabot.domain.draft.ExpenseLineItem;
import com.sonumax2.javabot.domain.draft.SypDraft;
import com.sonumax2.javabot.domain.operation.DocType;
import com.sonumax2.javabot.domain.operation.PayType;
import com.sonumax2.javabot.domain.operation.service.SypService;
import com.sonumax2.javabot.domain.reference.Counterparty;
import com.sonumax2.javabot.domain.reference.Nomenclature;
import com.sonumax2.javabot.domain.reference.WorkObject;
import com.sonumax2.javabot.domain.reference.service.CounterpartyService;
import com.sonumax2.javabot.domain.reference.service.NomenclatureService;
import com.sonumax2.javabot.domain.reference.service.WorkObjectService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SypFlowConfig {

    public static final String NS = "syp";

    private static final String S_OBJ = "obj";
    private static final String S_OBJ_SEARCH = "obj_search";

    private static final String S_ITEMS = "items";
    private static final String S_ITEM_PICK = "item_pick";
    private static final String S_ITEM_SEARCH = "item_search";
    private static final String S_ITEM_VOL = "item_vol";

    private static final String S_CP = "cp";
    private static final String S_CP_SEARCH = "cp_search";

    private static final String S_PAY = "pay";

    private static final String S_AMOUNT = "amount";
    private static final String S_DATE = "date";

    private static final String S_NOTE = "note";

    private static final String S_DOC = "doc";
    private static final String S_DOC_FILE = "docFile";

    private static final String S_CONFIRM = FlowDefinition.STEP_CONFIRM;

    @Bean
    public FlowDefinition<SypDraft> sypFlow(
            WorkObjectService workObjectService,
            CounterpartyService counterpartyService,
            NomenclatureService nomenclatureService,
            SypService sypService
    ) {
        return new FlowDefinition<>(
                NS,
                DraftType.SYP,
                SypDraft.class,
                S_OBJ
        )
                .addStartCallback(Cb.makeCb(CbParts.ADD_OPR, NS))

                // OBJECT
                .addStep(stepObject(workObjectService))
                .addStep(stepObjectSearch(workObjectService))

                // ITEMS + VOLUME (multi)
                .addStep(stepItems(nomenclatureService))
                .addStep(stepItemPick(nomenclatureService))
                .addStep(stepItemSearch(nomenclatureService))
                .addStep(stepItemVolume(nomenclatureService))

                // COUNTERPARTY
                .addStep(stepCounterparty(counterpartyService))
                .addStep(stepCounterpartySearch(counterpartyService))

                // PAY TYPE
                .addStep(stepPayType())

                // AMOUNT + DATE
                .addStep(stepAmount())
                .addStep(stepDate())

                // NOTE
                .addStep(stepNote())

                // DOC + FILE
                .addStep(stepDocType())
                .addStep(stepDocFile())

                // CONFIRM
                .addStep(stepConfirm(workObjectService, counterpartyService, nomenclatureService, sypService));
    }

    // ---------- steps ----------

    // 1) OBJECT
    private static SelectFromTopStep<SypDraft, WorkObject> stepObject(WorkObjectService workObjectService) {
        return SelectFromTopStep.<SypDraft, WorkObject>builder()
                .id(S_OBJ)
                .askKey("askObject")
                .options(ctx -> workObjectService.recentByChat(ctx.chatId, 8))
                .bind(d -> d.objectId, (d, v) -> d.objectId = v)
                .onTextSaveTo((d, txt) -> d.pendingObjectName = txt)
                .backTo("@opsMenu")
                .nextTo(S_ITEMS)
                .allowSkip(false)
                .textGoesTo(S_OBJ_SEARCH)
                .build();
    }

    private static SearchPickOrCreateRefStep<SypDraft, WorkObject> stepObjectSearch(WorkObjectService workObjectService) {
        return SearchPickOrCreateRefStep.<SypDraft, WorkObject>builder()
                .id(S_OBJ_SEARCH)
                .askKey("object.search.title")
                .pending(d -> d.pendingObjectName, (d, v) -> d.pendingObjectName = v)
                .saveIdTo((d, v) -> d.objectId = v)
                .exact((ctx, text) -> workObjectService.findExact(text))
                .search((ctx, text, lim) -> workObjectService.search(text, lim))
                .create((ctx, text) -> workObjectService.getOrCreate(text, ctx.chatId).getId())
                .backTo(S_OBJ)
                .nextTo(S_ITEMS)
                .limit(8)
                .build();
    }

    // 2) ITEMS (multi)
    private static LineItemsStep<SypDraft> stepItems(NomenclatureService nomenclatureService) {
        return new LineItemsStep<>(
                S_ITEMS,
                "syp.items.title",
                d -> d.items,
                (ctx, nid) -> nomenclatureService.findActiveById(nid).map(Nomenclature::getName).orElse("?"),

                (d, nid) -> d.pendingNomenclatureId = nid,
                (d, name) -> d.pendingNomenclatureName = name,

                S_OBJ,      // back
                S_CP,       // next  <-- ВАЖНО: после материалов идём к поставщику
                S_ITEM_PICK,
                S_ITEM_VOL
        );
    }

    private static SelectFromTopStep<SypDraft, Nomenclature> stepItemPick(NomenclatureService nomenclatureService) {
        return SelectFromTopStep.<SypDraft, Nomenclature>builder()
                .id(S_ITEM_PICK)
                .askKey("syp.item.pick")
                .options(ctx -> nomenclatureService.listActiveForSyp(20)) // режим A: только SYP
                .bind(d -> d.pendingNomenclatureId, (d, v) -> d.pendingNomenclatureId = v)
                .onTextSaveTo((d, txt) -> d.pendingNomenclatureName = txt)
                .backTo(S_ITEMS)
                .nextTo(S_ITEM_VOL)
                .allowSkip(false)
                .textGoesTo(S_ITEM_SEARCH)
                .build();
    }

    private static SearchPickOrCreateRefStep<SypDraft, Nomenclature> stepItemSearch(NomenclatureService nomenclatureService) {
        return SearchPickOrCreateRefStep.<SypDraft, Nomenclature>builder()
                .id(S_ITEM_SEARCH)
                .askKey("syp.item.search.title")
                .pending(d -> d.pendingNomenclatureName, (d, v) -> d.pendingNomenclatureName = v)
                .saveIdTo((d, v) -> d.pendingNomenclatureId = v)
                .exact((ctx, text) -> nomenclatureService.findExact(text))
                .search((ctx, text, lim) -> nomenclatureService.searchForSyp(text, lim)) // режим A
                .create((ctx, text) -> {
                    long id = nomenclatureService.getOrCreate(text, ctx.chatId).getId();
                    return id;
                })
                .backTo(S_ITEM_PICK)
                .nextTo(S_ITEM_VOL)
                .limit(8)
                .build();
    }

    private static LineItemVolumeStep<SypDraft> stepItemVolume(NomenclatureService nomenclatureService) {
        return new LineItemVolumeStep<>(
                S_ITEM_VOL,
                "syp.item.askVolume",
                "decimalInvalid",
                d -> d.pendingNomenclatureId,
                (d, v) -> d.pendingNomenclatureId = v,
                (d, v) -> d.pendingNomenclatureName = v,
                d -> d.items,
                (ctx, nid) -> nomenclatureService.findActiveById(nid).map(Nomenclature::getName).orElse("?"),
                S_ITEM_PICK,
                S_ITEMS
        );
    }

    // 3) COUNTERPARTY
    private static SelectFromTopStep<SypDraft, Counterparty> stepCounterparty(CounterpartyService counterpartyService) {
        return SelectFromTopStep.<SypDraft, Counterparty>builder()
                .id(S_CP)
                .askKey("cp.ask")
                .options(ctx -> counterpartyService.recentByChat(ctx.chatId, 8))
                .bind(d -> d.counterpartyId, (d, v) -> d.counterpartyId = v)
                .onTextSaveTo((d, txt) -> d.pendingCounterpartyName = txt)
                .backTo(S_ITEMS)
                .nextTo(S_PAY)
                .allowSkip(false)
                .textGoesTo(S_CP_SEARCH)
                .build();
    }

    private static SearchPickOrCreateRefStep<SypDraft, Counterparty> stepCounterpartySearch(CounterpartyService counterpartyService) {
        return SearchPickOrCreateRefStep.<SypDraft, Counterparty>builder()
                .id(S_CP_SEARCH)
                .askKey("cp.search.title")
                .pending(d -> d.pendingCounterpartyName, (d, v) -> d.pendingCounterpartyName = v)
                .saveIdTo((d, v) -> d.counterpartyId = v)
                .exact((ctx, text) -> counterpartyService.findExact(text))
                .search((ctx, text, lim) -> counterpartyService.search(text, lim))
                .create((ctx, text) -> counterpartyService.getOrCreate(text, ctx.chatId).getId())
                .backTo(S_CP)
                .nextTo(S_PAY)
                .limit(8)
                .build();
    }

    // 4) PAY TYPE
    private static EnumSelectStep<SypDraft, PayType> stepPayType() {
        return new EnumSelectStep<>(
                S_PAY,
                "syp.askPayType",
                PayType.class,
                v -> v == PayType.CASH ? "payType.cash" : "payType.cashless",
                d -> d.payType,
                (d, v) -> d.payType = v,
                S_CP,
                S_AMOUNT
        );
    }

    // 5) AMOUNT
    private static AmountInputStep<SypDraft> stepAmount() {
        return new AmountInputStep<>(
                S_AMOUNT,
                "askAmount",
                d -> d.amount,
                (d, v) -> d.amount = v,
                S_PAY,
                S_DATE
        );
    }

    // 6) DATE
    private static DateInputStep<SypDraft> stepDate() {
        return new DateInputStep<>(
                S_DATE,
                "askDate",
                d -> d.date,
                (d, v) -> d.date = v,
                S_AMOUNT,
                S_NOTE
        );
    }

    // 7) NOTE
    private static TextInputStep<SypDraft> stepNote() {
        return new TextInputStep<>(
                S_NOTE,
                "syp.askNote",
                "syp.askNote",
                d -> d.note,
                (d, v) -> d.note = v,
                S_DATE,
                S_DOC,
                true,
                S_DOC,
                s -> null
        );
    }

    // 8) DOC + FILE
    private static DocTypeSelectStep<SypDraft> stepDocType() {
        return new DocTypeSelectStep<>(
                S_DOC,
                "receipt.ask",
                d -> d.docType,
                (d, v) -> d.docType = v,
                (d, v) -> { if (v == DocType.NO_RECEIPT) d.docFileId = null; },
                S_NOTE,
                S_DOC_FILE,
                S_CONFIRM
        );
    }

    private static FileInputStep<SypDraft> stepDocFile() {
        return new FileInputStep<>(
                S_DOC_FILE,
                "receipt.askFile",
                d -> d.docFileId,
                (d, v) -> d.docFileId = v,
                S_DOC,
                S_CONFIRM,
                true
        );
    }

    // 9) CONFIRM
    private static ConfirmStep<SypDraft> stepConfirm(
            WorkObjectService workObjectService,
            CounterpartyService counterpartyService,
            NomenclatureService nomenclatureService,
            SypService sypService
    ) {
        return ConfirmStep.<SypDraft>builder()
                .id(S_CONFIRM)
                .render(ctx -> renderConfirm(ctx, workObjectService, counterpartyService, nomenclatureService))
                .editsProvider(ctx -> {
                    List<ConfirmStep.EditBtn> b = new ArrayList<>();
                    b.add(new ConfirmStep.EditBtn("btnEditObject", S_OBJ));
                    b.add(new ConfirmStep.EditBtn("btnEditItems", S_ITEMS));
                    b.add(new ConfirmStep.EditBtn("btnEditCp", S_CP));
                    b.add(new ConfirmStep.EditBtn("btnEditPayType", S_PAY));
                    b.add(new ConfirmStep.EditBtn("btnEditAmount", S_AMOUNT));
                    b.add(new ConfirmStep.EditBtn("btnEditDate", S_DATE));
                    b.add(new ConfirmStep.EditBtn("btnEditNote", S_NOTE));
                    b.add(new ConfirmStep.EditBtn("btnEditDoc", S_DOC));
                    if (ctx.d.docType != null && ctx.d.docType.needsFile()) {
                        boolean hasFile = ctx.d.docFileId != null && !ctx.d.docFileId.isBlank();
                        String key = hasFile ? "btnReplaceFile" : "btnAttachFile";
                        b.add(new ConfirmStep.EditBtn(key, S_DOC_FILE));
                    }
                    return b;
                })
                .allowSave(ctx ->
                        ConfirmSupport.saveAndShowMain(ctx, sypService, "syp.saved")
                )
                .allowCancel(ctx ->
                        ConfirmSupport.cancelAndShowMain(ctx, "cancelled")
                )
                .build();
    }

    private static String renderConfirm(
            FlowContext<SypDraft> ctx,
            WorkObjectService workObjectService,
            CounterpartyService counterpartyService,
            NomenclatureService nomenclatureService
    ) {
        var d = ctx.d;
        String none = ctx.ui().msg(ctx.chatId, "common.none");

        String obj = workObjectService.findActiveById(d.objectId).map(WorkObject::getName).orElse(none);
        String cp  = counterpartyService.findActiveById(d.counterpartyId).map(Counterparty::getName).orElse(none);

        String pay = (d.payType == null)
                ? none
                : ctx.ui().msg(ctx.chatId, d.payType == PayType.CASH ? "payType.cash" : "payType.cashless");

        StringBuilder items = new StringBuilder();
        if (d.items == null || d.items.isEmpty()) {
            items.append(none);
        } else {
            for (int i = 0; i < d.items.size(); i++) {
                ExpenseLineItem it = d.items.get(i);
                String name = (it == null || it.nomenclatureId == null) ? "?" :
                        nomenclatureService.findActiveById(it.nomenclatureId).map(Nomenclature::getName).orElse("?");
                String vol = (it == null || it.volume == null) ? "?" : it.volume.toPlainString();
                items.append(i + 1).append(") ").append(name).append(" — ").append(vol).append("\n");
            }
        }

        String note = (d.note == null || d.note.isBlank()) ? none : d.note;

        DocType doc = (d.docType == null ? DocType.NO_RECEIPT : d.docType);
        String docLabel = switch (doc) {
            case RECEIPT -> ctx.ui().msg(ctx.chatId, "expense.doc.receipt");
            case INVOICE -> ctx.ui().msg(ctx.chatId, "expense.doc.invoice");
            case NO_RECEIPT -> ctx.ui().msg(ctx.chatId, "expense.doc.none");
        };

        String file = (d.docFileId == null || d.docFileId.isBlank()) ? none : "✅";

        return ""
                + ctx.ui().msg(ctx.chatId, "confirm.object") + ": " + obj + "\n"
                + ctx.ui().msg(ctx.chatId, "confirm.items") + ":\n" + items + "\n"
                + ctx.ui().msg(ctx.chatId, "confirm.cp") + ": " + cp + "\n"
                + ctx.ui().msg(ctx.chatId, "confirm.payType") + ": " + pay + "\n"
                + ctx.ui().msg(ctx.chatId, "confirm.amount") + ": " + (d.amount == null ? none : d.amount) + "\n"
                + ctx.ui().msg(ctx.chatId, "confirm.date") + ": " + (d.date == null ? none : d.date) + "\n"
                + ctx.ui().msg(ctx.chatId, "confirm.note") + ": " + note + "\n"
                + ctx.ui().msg(ctx.chatId, "confirm.doc") + ": " + docLabel + "\n"
                + ctx.ui().msg(ctx.chatId, "confirm.file") + ": " + file;
    }
}
