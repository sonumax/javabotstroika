package com.sonumax2.javabot.bot.flow.fuel;

import com.sonumax2.javabot.bot.commands.cb.Cb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.flow.*;
import com.sonumax2.javabot.bot.flow.confirm.ConfirmDocUtil;
import com.sonumax2.javabot.bot.flow.confirm.ConfirmField;
import com.sonumax2.javabot.bot.flow.confirm.ConfirmRenderer;
import com.sonumax2.javabot.bot.flow.steps.*;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.DraftType;
import com.sonumax2.javabot.domain.draft.FuelDraft;
import com.sonumax2.javabot.domain.draft.FuelKind;
import com.sonumax2.javabot.domain.operation.DocType;
import com.sonumax2.javabot.domain.operation.service.FuelService;
import com.sonumax2.javabot.domain.reference.Counterparty;
import com.sonumax2.javabot.domain.reference.CounterpartyKind;
import com.sonumax2.javabot.domain.reference.FuelMachineType;
import com.sonumax2.javabot.domain.reference.WorkObject;
import com.sonumax2.javabot.domain.reference.service.CounterpartyService;
import com.sonumax2.javabot.domain.reference.service.FuelMachineTypeService;
import com.sonumax2.javabot.domain.reference.service.WorkObjectService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class FuelFlowConfig {

    public static final String NS = CbParts.FUEL; // "fuel"

    // step ids
    private static final String S_KIND = "kind";

    private static final String S_MT = "mtype";
    private static final String S_MT_SEARCH = "mtype_search";

    private static final String S_OBJ = "obj";
    private static final String S_OBJ_SEARCH = "obj_search";

    private static final String S_DATE = "date";

    private static final String S_CP = "cp";
    private static final String S_CP_SEARCH = "cp_search";

    private static final String S_VOLUME = "vol";
    private static final String S_AMOUNT = "amount";
    private static final String S_NOTE = "note";

    private static final String S_DOC = "doc";
    private static final String S_DOC_FILE = "docFile";

    private static final String S_CONFIRM = FlowDefinition.STEP_CONFIRM;

    @Bean
    public FlowDefinition<FuelDraft> fuelFlow(
            FuelMachineTypeService fuelMachineTypeService,
            WorkObjectService workObjectService,
            CounterpartyService counterpartyService,
            FuelService fuelService
    ) {
        return new FlowDefinition<>(
                NS,
                DraftType.FUEL,
                FuelDraft.class,
                S_KIND
        )
                .addStartCallback(Cb.makeCb(CbParts.ADD_OPR, NS))
                .addStartCommand("/fuel")

                // -------- KIND --------
                .addStep(stepKind())

                // -------- MACHINE TYPE (only for MACHINE) --------
                .addStep(stepMachineTypeTop(fuelMachineTypeService))
                .addStep(stepMachineTypeSearch(fuelMachineTypeService))

                // -------- WORK OBJECT --------
                .addStep(stepObject(workObjectService))
                .addStep(stepObjectSearch(workObjectService))

                // -------- DATE / COUNTERPARTY / VOLUME / AMOUNT --------
                .addStep(stepDate())
                .addStep(stepCounterparty(counterpartyService))
                .addStep(stepCounterpartySearch(counterpartyService))
                .addStep(stepVolume())
                .addStep(stepAmount())

                // -------- NOTE --------
                .addStep(stepNote())

                // -------- DOC TYPE + FILE --------
                .addStep(stepDocType())
                .addStep(stepDocFile())

                // -------- CONFIRM --------
                .addStep(stepConfirm(workObjectService, fuelMachineTypeService, counterpartyService, fuelService));
    }

    // -------------------- steps --------------------

    private static EnumSelectStep<FuelDraft, FuelKind> stepKind() {
        return new EnumSelectStep<>(
                S_KIND,
                "fuel.askKind",
                FuelKind.class,
                k -> (k == FuelKind.MACHINE) ? "fuel.kind.machine" : "fuel.kind.transport",
                d -> d.fuelKind,
                (d, v) -> {
                    d.fuelKind = v;
                    if (v != FuelKind.MACHINE) d.machineTypeId = null;
                },
                "@opsMenu",
                S_OBJ // placeholder
        ) {
            @Override
            public StepMove onCallback(FlowContext<FuelDraft> ctx, String data, PanelMode mode) {
                String ns = ctx.def.ns;

                if (FlowCb.is(data, ns, id(), "back")) {
                    StepMove m = FlowNav.confirmIfNeeded(ctx);
                    if (m != null) return m;
                    return StepMove.go("@opsMenu");
                }

                if (FlowCb.startsWith(data, ns, id(), "set")) {
                    String name = FlowCb.tail(data, ns, id(), "set");
                    try {
                        FuelKind val = FuelKind.valueOf(name);
                        ctx.d.fuelKind = val;
                        if (val != FuelKind.MACHINE) ctx.d.machineTypeId = null;

                        // MACHINE -> сначала тип техники, потом объект
                        return FlowNav.goOrConfirm(ctx, val == FuelKind.MACHINE ? S_MT : S_OBJ);
                    } catch (Exception ignore) {
                        this.show(ctx, mode);
                        return StepMove.rendered();
                    }
                }

                return StepMove.unhandled();
            }
        };
    }

    private static SelectFromTopStep<FuelDraft, FuelMachineType> stepMachineTypeTop(
            FuelMachineTypeService fuelMachineTypeService
    ) {
        return SelectFromTopStep.<FuelDraft, FuelMachineType>builder()
                .id(S_MT)
                .askKey("fuel.machineType.ask")
                .options(ctx -> limit(fuelMachineTypeService.suggestByChat(ctx.chatId, 8), 8))
                .bind(d -> d.machineTypeId, (d, v) -> d.machineTypeId = v)
                .onTextSaveTo((d, txt) -> d.pendingMachineTypeName = txt)
                .backTo(S_KIND)
                .nextTo(S_OBJ)
                .allowSkip(false)
                .textGoesTo(S_MT_SEARCH)
                .build();
    }

    private static SearchPickOrCreateRefStep<FuelDraft, FuelMachineType> stepMachineTypeSearch(
            FuelMachineTypeService fuelMachineTypeService
    ) {
        return SearchPickOrCreateRefStep.<FuelDraft, FuelMachineType>builder()
                .id(S_MT_SEARCH)
                .askKey("fuel.machineType.search")
                .pending(d -> d.pendingMachineTypeName, (d, v) -> d.pendingMachineTypeName = v)
                .saveIdTo((d, v) -> d.machineTypeId = v)
                .exact((ctx, text) -> fuelMachineTypeService.findExact(text))
                .search((ctx, text, lim) -> fuelMachineTypeService.search(text, lim))
                .create((ctx, text) -> fuelMachineTypeService.getOrCreate(text, ctx.chatId).getId())
                .backTo(S_MT)
                .nextTo(S_OBJ)
                .limit(8)
                .build();
    }

    private static SelectFromTopStep<FuelDraft, WorkObject> stepObject(WorkObjectService workObjectService) {
        // prevStepId зависит от ветки: MACHINE -> mtype, TRANSPORT -> kind
        return new SelectFromTopStep<>(
                S_OBJ,
                "fuel.askObject",
                ctx -> workObjectService.suggestByChat(ctx.chatId, 8),
                d -> d.objectId,
                (d, v) -> d.objectId = v,
                (d, txt) -> d.pendingObjectName = txt,
                S_KIND, // placeholder (back обрабатываем вручную ниже)
                S_DATE,
                false,
                S_OBJ_SEARCH
        ) {
            @Override
            public StepMove onCallback(FlowContext<FuelDraft> ctx, String data, PanelMode mode) {
                if (FlowCb.is(data, ctx.def.ns, id(), "back")) {
                    StepMove m = FlowNav.confirmIfNeeded(ctx);
                    if (m != null) return m;
                    return StepMove.go(ctx.d.isMachine() ? S_MT : S_KIND);
                }
                return super.onCallback(ctx, data, mode);
            }
        };
    }

    private static SearchPickOrCreateRefStep<FuelDraft, WorkObject> stepObjectSearch(
            WorkObjectService workObjectService
    ) {
        return SearchPickOrCreateRefStep.<FuelDraft, WorkObject>builder()
                .id(S_OBJ_SEARCH)
                .askKey("fuel.object.search")
                .pending(d -> d.pendingObjectName, (d, v) -> d.pendingObjectName = v)
                .saveIdTo((d, v) -> d.objectId = v)
                .exact((ctx, text) -> workObjectService.findExact(text))
                .search((ctx, text, lim) -> workObjectService.search(text, lim))
                .create((ctx, text) -> workObjectService.getOrCreate(text, ctx.chatId).getId())
                .backTo(S_OBJ)
                .nextTo(S_DATE)
                .limit(8)
                .build();
    }

    private static DateInputStep<FuelDraft> stepDate() {
        return new DateInputStep<>(
                S_DATE,
                "fuel.askDate",
                d -> d.date,
                (d, v) -> d.date = v,
                S_OBJ,
                S_CP
        ) {
            @Override
            public StepMove onCallback(FlowContext<FuelDraft> ctx, String data, PanelMode mode) {
                if (FlowCb.is(data, ctx.def.ns, id(), "back")) {
                    StepMove m = FlowNav.confirmIfNeeded(ctx);
                    if (m != null) return m;
                    return StepMove.go(S_OBJ);
                }
                return super.onCallback(ctx, data, mode);
            }
        };
    }

    private static SelectFromTopStep<FuelDraft, Counterparty> stepCounterparty(
            CounterpartyService counterpartyService
    ) {
        return SelectFromTopStep.<FuelDraft, Counterparty>builder()
                .id(S_CP)
                .askKey("fuel.askCounterparty")
                .options(ctx -> counterpartyService.suggestByChat(ctx.chatId, CounterpartyKind.FUEL, 8))
                .bind(d -> d.counterpartyId, (d, v) -> d.counterpartyId = v)
                .onTextSaveTo((d, txt) -> d.pendingCounterpartyName = txt)
                .backTo(S_DATE)
                .nextTo(S_VOLUME)
                .allowSkip(false)
                .textGoesTo(S_CP_SEARCH)
                .build();
    }

    private static SearchPickOrCreateRefStep<FuelDraft, Counterparty> stepCounterpartySearch(
            CounterpartyService counterpartyService
    ) {
        return SearchPickOrCreateRefStep.<FuelDraft, Counterparty>builder()
                .id(S_CP_SEARCH)
                .askKey("fuel.counterparty.search")
                .pending(d -> d.pendingCounterpartyName, (d, v) -> d.pendingCounterpartyName = v)
                .saveIdTo((d, v) -> d.counterpartyId = v)
                .exact((ctx, text) -> counterpartyService.findExact(CounterpartyKind.FUEL, text))
                .search((ctx, text, lim) -> counterpartyService.search(CounterpartyKind.FUEL, text, lim))
                .create((ctx, text) -> counterpartyService.getOrCreate(text, CounterpartyKind.FUEL, ctx.chatId).getId())
                .backTo(S_CP)
                .nextTo(S_VOLUME)
                .limit(8)
                .build();
    }

    private static DecimalInputStep<FuelDraft> stepVolume() {
        return new DecimalInputStep<>(
                S_VOLUME,
                "fuel.askVolume",
                "fuel.volume.invalid",
                d -> d.volume,
                (d, v) -> d.volume = v,
                S_CP,
                S_AMOUNT,
                false
        );
    }

    private static AmountInputStep<FuelDraft> stepAmount() {
        return new AmountInputStep<>(
                S_AMOUNT,
                "fuel.askAmount",
                d -> d.amount,
                (d, v) -> d.amount = v,
                S_VOLUME,
                S_NOTE
        );
    }

    private static TextInputStep<FuelDraft> stepNote() {
        return new TextInputStep<>(
                S_NOTE,
                "fuel.askNote",
                "noteInvalid",
                d -> d.note,
                (d, v) -> d.note = v,
                S_AMOUNT,
                S_DOC,
                true,
                S_DOC,
                s -> null
        );
    }

    private static DocTypeSelectStep<FuelDraft> stepDocType() {
        return new DocTypeSelectStep<>(
                S_DOC,
                "receipt.ask",
                d -> d.docType,
                (d, v) -> d.docType = v,
                (d, v) -> {
                    if (v == DocType.NO_RECEIPT) d.docFileId = null;
                },
                S_NOTE,
                S_DOC_FILE,
                S_CONFIRM
        );
    }

    private static FileInputStep<FuelDraft> stepDocFile() {
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

    private static ConfirmStep<FuelDraft> stepConfirm(
            WorkObjectService workObjectService,
            FuelMachineTypeService fuelMachineTypeService,
            CounterpartyService counterpartyService,
            FuelService fuelService
    ) {
        return ConfirmStep.<FuelDraft>builder()
                .id(S_CONFIRM)
                .render(ctx -> {

                    String kind = (ctx.d.fuelKind == FuelKind.MACHINE)
                            ? ctx.ui().msg(ctx.chatId, "fuel.kind.machine")
                            : ctx.ui().msg(ctx.chatId, "fuel.kind.transport");

                    String machineType;
                    if (ctx.d.isMachine() && ctx.d.machineTypeId != null) {
                        machineType = fuelMachineTypeService
                                .findName(ctx.d.machineTypeId)
                                .orElse(null);
                    } else {
                        machineType = null;
                    }

                    String objectName = ctx.d.objectId == null
                            ? null
                            : workObjectService.findActiveById(ctx.d.objectId)
                            .map(WorkObject::getName)
                            .orElse(null);

                    String counterpartyName = ctx.d.counterpartyId == null
                            ? null
                            : counterpartyService.findActiveById(ctx.d.counterpartyId)
                            .map(Counterparty::getName)
                            .orElse(null);

                    String doc = ConfirmDocUtil.buildDocWithFileMark(ctx, ctx.d.docType, ctx.d.docFileId);

                    return ConfirmRenderer.render(
                            ctx,
                            k -> ctx.ui().msg(ctx.chatId, k),
                            List.of(
                                    ConfirmField.of("confirm.kind", c -> kind),
                                    ConfirmField.of("confirm.machineType", c -> machineType, c -> c.d.isMachine()),
                                    ConfirmField.of("confirm.object", c -> objectName),
                                    ConfirmField.of("confirm.date", c -> c.d.date == null ? null : c.d.date.toString()),
                                    ConfirmField.of("confirm.cp", c -> counterpartyName),
                                    ConfirmField.of("confirm.volume", c -> c.d.volume == null ? null : c.d.volume.toString()),
                                    ConfirmField.of("confirm.amount", c -> c.d.amount == null ? null : c.d.amount.toString()),
                                    ConfirmField.of("confirm.doc", c -> doc),
                                    ConfirmField.of("confirm.note", c -> (c.d.note == null || c.d.note.isBlank()) ? null : c.d.note)
                            ),
                            "common.none"
                    );
                })
                .editsProvider(ctx -> {
                    List<ConfirmStep.EditBtn> b = new ArrayList<>();
                    b.add(new ConfirmStep.EditBtn("btnEditFuelKind", S_KIND));
                    if (ctx.d.isMachine()) b.add(new ConfirmStep.EditBtn("btnEditMachineType", S_MT));
                    b.add(new ConfirmStep.EditBtn("btnEditObject", S_OBJ));
                    b.add(new ConfirmStep.EditBtn("btnEditDate", S_DATE));
                    b.add(new ConfirmStep.EditBtn("btnEditCp", S_CP));
                    b.add(new ConfirmStep.EditBtn("btnEditVolume", S_VOLUME));
                    b.add(new ConfirmStep.EditBtn("btnEditAmount", S_AMOUNT));

                    b.add(new ConfirmStep.EditBtn("btnEditDoc", S_DOC));
                    if (ctx.d.docType != null && ctx.d.docType.needsFile()) {
                        boolean hasFile = ctx.d.docFileId != null && !ctx.d.docFileId.isBlank();
                        String key = hasFile ? "btnReplaceFile" : "btnAttachFile";
                        b.add(new ConfirmStep.EditBtn(key, S_DOC_FILE));
                    }

                    b.add(new ConfirmStep.EditBtn("btnEditNote", S_NOTE));
                    return b;
                })
                .allowSave(ctx ->
                        ConfirmSupport.saveAndShowMain(ctx, fuelService, "fuel.saved")
                )
                .allowCancel(ctx ->
                        ConfirmSupport.cancelAndShowMain(ctx, "cancelled")
                )
                .build();
    }

    // -------------------- helpers --------------------

    private static <T> List<T> limit(List<T> list, int limit) {
        if (list == null || list.isEmpty()) return List.of();
        if (limit <= 0 || list.size() <= limit) return list;
        return list.subList(0, limit);
    }
}
