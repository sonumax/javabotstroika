package com.sonumax2.javabot.bot.commands.ops;

import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.cb.ExpenseCb;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.DraftType;
import com.sonumax2.javabot.domain.draft.ExpenseDraft;
import com.sonumax2.javabot.domain.draft.service.DraftService;
import com.sonumax2.javabot.domain.operation.DocType;
import com.sonumax2.javabot.domain.operation.service.ExpenseSaveService;
import com.sonumax2.javabot.domain.operation.service.ExpenseService;
import com.sonumax2.javabot.domain.reference.Counterparty;
import com.sonumax2.javabot.domain.reference.Nomenclature;
import com.sonumax2.javabot.domain.reference.WorkObject;
import com.sonumax2.javabot.domain.reference.service.CounterpartyService;
import com.sonumax2.javabot.domain.reference.service.NomenclatureService;
import com.sonumax2.javabot.domain.reference.service.WorkObjectService;
import com.sonumax2.javabot.domain.session.UserState;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import com.sonumax2.javabot.util.DateParseResult;
import com.sonumax2.javabot.util.InputParseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Order(50)
@Component
public class ExpenseCommand implements Command {

    private static final Logger log = LoggerFactory.getLogger(ExpenseCommand.class);

    private static final String PREVIEW_MENU = CbParts.ADD_OPR;
    private static final DraftType DRAFT_TYPE = DraftType.EXPENSE;

    private static final EnumSet<UserState> FLOW_STATES = EnumSet.of(
            UserState.EXPENSE_WAIT_OBJECT,
            UserState.EXPENSE_WAIT_OBJECT_TEXT,
            UserState.EXPENSE_WAIT_NOMENCLATURE_PICK,
            UserState.EXPENSE_WAIT_NEW_NOMENCLATURE_TEXT,
            UserState.EXPENSE_WAIT_NOMENCLATURE_SUGGEST,
            UserState.EXPENSE_WAIT_CP_PICK,
            UserState.EXPENSE_WAIT_CP_SUGGEST,
            UserState.EXPENSE_WAIT_CP_TEXT,
            UserState.EXPENSE_WAIT_AMOUNT,
            UserState.EXPENSE_WAIT_DATE,
            UserState.EXPENSE_WAIT_DATE_TEXT,
            UserState.EXPENSE_WAIT_DOC_TYPE,
            UserState.EXPENSE_WAIT_DOC_FILE,
            UserState.EXPENSE_WAIT_NOTE,
            UserState.EXPENSE_CONFIRM
    );

    private final KeyboardService keyboardService;
    private final UserSessionService userSessionService;
    private final DraftService draftService;
    private final BotUi ui;
    private final WorkObjectService workObjectService;
    private final NomenclatureService nomenclatureService;
    private final CounterpartyService counterpartyService;
    private final ExpenseService expenseService;
    private final ExpenseSaveService expenseSaveService;

    public ExpenseCommand(
            KeyboardService keyboardService,
            UserSessionService userSessionService,
            DraftService draftService,
            BotUi ui,
            WorkObjectService workObjectService,
            NomenclatureService nomenclatureService, CounterpartyService counterpartyService, ExpenseService expenseService, ExpenseSaveService expenseSaveService
    ) {
        this.keyboardService = keyboardService;
        this.userSessionService = userSessionService;
        this.draftService = draftService;
        this.ui = ui;
        this.workObjectService = workObjectService;
        this.nomenclatureService = nomenclatureService;
        this.counterpartyService = counterpartyService;
        this.expenseService = expenseService;
        this.expenseSaveService = expenseSaveService;
    }

    @Override
    public boolean canHandle(Update update) {
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            String data = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            UserState st = userSessionService.getUserState(chatId);

            if (ExpenseCb.isStartPick(data)) return true;
            return data.startsWith(ExpenseCb.NS) && FLOW_STATES.contains(st);
        }

        if (update.hasMessage()) {
            long chatId = update.getMessage().getChatId();
            UserState st = userSessionService.getUserState(chatId);

            if (update.getMessage().hasPhoto() || update.getMessage().hasDocument()) {
                return st == UserState.EXPENSE_WAIT_DOC_FILE;
            }

            if (update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                if (text != null && text.startsWith("/")) return false;
                return FLOW_STATES.contains(st);
            }
        }

        return false;
    }

    @Override
    public void handle(Update update) {
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            handleCallback(update);
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            handleText(update);
            return;
        }

        if (update.hasMessage() && (update.getMessage().hasPhoto() || update.getMessage().hasDocument())) {
            handleDocFile(update);
        }
    }

    private void handleCallback(Update update) {
        var cq = update.getCallbackQuery();
        String data = cq.getData();
        long chatId = cq.getMessage().getChatId();
        int messageId = cq.getMessage().getMessageId();
        PanelMode mode = PanelMode.EDIT;

        ui.setPanelId(chatId, messageId);
        ui.ack(cq.getId());

        if (ExpenseCb.isStartPick(data)) {
            draftService.clear(chatId, DRAFT_TYPE);
            showChoseObjectPanel(chatId, mode);
            return;
        }

        if (ExpenseCb.isObject(data)) {
            if (ExpenseCb.isObjectNewPick(data)) {
                showNewObjectPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isNewObjectBackPick(data)) {
                showChoseObjectPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isObjectPick(data)) {
                saveObject(chatId, ExpenseCb.pickObjectId(data));
                afterObject(chatId, mode);
                return;
            }
        }

        if (ExpenseCb.isNomenclature(data)) {
            if (ExpenseCb.isNomenclatureBackPick(data)) {
                showChoseObjectPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isNomenclatureNewPick(data)) {
                showCreateNewNomenclaturePanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isNewNomenclatureBackPick(data)) {
                showChooseNomenclaturePanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isSearchBackNomenclaturePick(data)) {
                showChooseNomenclaturePanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isCreateNomenclaturePick(data)) {
                ExpenseDraft d = draft(chatId);
                if (d.pendingNomenclatureName != null && !d.pendingNomenclatureName.isBlank()) {
                    createNewNomenclature(chatId, d.pendingNomenclatureName);
                }

                if (goConfirmIfNeeded(chatId, mode)) return;

                showChooseCounterpartyPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isNomenclaturePick(data)) {
                saveNomenclature(chatId, ExpenseCb.pickNomenclatureId(data));
                afterNomenclature(chatId, mode);
                return;
            }
        }

        if (ExpenseCb.isCounterparty(data)) {
            if (ExpenseCb.isCounterpartySkipPick(data)) {
                saveCounterparty(chatId, null);
                afterCounterparty(chatId, mode);
                return;
            }

            if (ExpenseCb.isCounterpartyNewPick(data)) {
                showNewCounterpartyPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isNewCounterpartyBackPick(data)) {
                showChooseCounterpartyPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isSearchBackCounterpartyPick(data)) {
                showChooseCounterpartyPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isCreateCounterpartyPick(data)) {
                ExpenseDraft d = draft(chatId);
                if (d.pendingCounterpartyName != null && !d.pendingCounterpartyName.isBlank()) {
                    createNewCounterparty(chatId, d.pendingCounterpartyName);
                }

                if (goConfirmIfNeeded(chatId, mode)) return;

                showEnterAmountPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isCounterpartyPick(data)) {
                saveCounterparty(chatId, ExpenseCb.pickCounterpartyId(data));
                afterCounterparty(chatId, mode);
                return;
            }
        }

        if (ExpenseCb.isAmountBackPick(data)
                || ExpenseCb.isAmountErrorBackPick(data)) {
            showChooseCounterpartyPanel(chatId, mode);
            return;
        }

        if (ExpenseCb.isDate(data)) {
            if (ExpenseCb.isDateBackPick(data)) {
                showEnterAmountPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isDateManualBackPick(data)
                    || ExpenseCb.isDateErrorBackPick(data)) {
                showChooseDatePanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isDateManualPick(data)) {
                showEnterDatePanel(chatId, mode);
                return;
            }

            LocalDate date = LocalDate.now();
            if (ExpenseCb.isDateYesterdayPick(data)) date = date.minusDays(1);


            saveDate(chatId, date);
            afterDate(chatId, mode);
            return;
        }

        if (ExpenseCb.isDoc(data)) {
            if (ExpenseCb.isDocBackPick(data)) {
                showChooseDatePanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isDocFileBackPick(data)) {
                showChooseDocTypePanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isDocFileSkipPick(data)) {
                saveDocFileId(chatId, null);
                afterDocFile(chatId, mode);
                return;
            }

            saveDocType(chatId, DocType.valueOf(ExpenseCb.docType(data)));
            saveDocFileId(chatId, null);
            afterDocType(chatId, mode);
            return;
        }

        if (ExpenseCb.isNoteBackPick(data)) {
            ExpenseDraft d = draft(chatId);

            if (d.docType == null || d.docType == DocType.NO_RECEIPT) {
                showChooseDocTypePanel(chatId, mode);
            } else {
                showAttachDocFilePanel(chatId, mode);
            }
            return;
        }

        if (ExpenseCb.isNoteSkipPick(data)) {
            saveNote(chatId, null);
            showConfirmPanel(chatId, mode);
            return;
        }

        if (ExpenseCb.isConfirmPick(data)) {
            if (ExpenseCb.isConfirmSavePick(data)) {
                saveExpense(chatId, mode);
                return;
            }
            if (ExpenseCb.isConfirmEditObjectPick(data)) {
                setConfirm(chatId, true);
                showChoseObjectPanel(chatId, mode);
                return;
            }
            if (ExpenseCb.isConfirmEditItemPick(data)) {
                setConfirm(chatId, true);
                showChooseNomenclaturePanel(chatId, mode);
                return;
            }
            if (ExpenseCb.isConfirmEditCpPick(data)) {
                setConfirm(chatId, true);
                showChooseCounterpartyPanel(chatId, mode);
                return;
            }
            if (ExpenseCb.isConfirmEditAmountPick(data)) {
                setConfirm(chatId, true);
                showEnterAmountPanel(chatId, mode);
                return;
            }
            if (ExpenseCb.isConfirmEditDatePick(data)) {
                setConfirm(chatId, true);
                showChooseDatePanel(chatId, mode);
                return;
            }
            if (ExpenseCb.isConfirmEditDocPick(data)) {
                setConfirm(chatId, true);
                showChooseDocTypePanel(chatId, mode);
                return;
            }
            if (ExpenseCb.isConfirmAttachFilePick(data)) {
                setConfirm(chatId, true);

                ExpenseDraft d = draft(chatId);
                if (d.docType == null || d.docType == DocType.NO_RECEIPT) {
                    showChooseDocTypePanel(chatId, mode);
                } else {
                    showAttachDocFilePanel(chatId, mode);
                }
                return;
            }
            if (ExpenseCb.isConfirmEditNotePick(data)) {
                setConfirm(chatId, true);
                showEnterNotePanel(chatId, mode);
                return;
            }
            if (ExpenseCb.isConfirmCancelPick(data)) {
                goToMainMenu(chatId, "cancelled", mode);
                return;
            }
            if (ExpenseCb.isConfirmBackPick(data)) {
                showConfirmPanel(chatId, mode);
                return;
            }
        }
    }

    private void handleText(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        UserState st = userSessionService.getUserState(chatId);
        PanelMode mode = PanelMode.MOVE_DOWN;

        switch (st) {
            case EXPENSE_WAIT_OBJECT_TEXT -> createNewObjectAndShowNextMenu(chatId, text, mode);
            case EXPENSE_WAIT_NOMENCLATURE_PICK, EXPENSE_WAIT_NOMENCLATURE_SUGGEST ->
                    searchOrSelectNomenclaturePanel(chatId, text, mode);
            case EXPENSE_WAIT_NEW_NOMENCLATURE_TEXT -> createNewNomenclatureAndShowNextMenu(chatId, text, mode);
            case EXPENSE_WAIT_CP_PICK, EXPENSE_WAIT_CP_SUGGEST -> selectOrSearchCounterparty(chatId, text, mode);
            case EXPENSE_WAIT_CP_TEXT -> createNewCounterpartyAndShowNextMenu(chatId, text, mode);
            case EXPENSE_WAIT_AMOUNT -> checkAndSaveAmount(chatId, text, mode);
            case EXPENSE_WAIT_DATE_TEXT -> checkAndSaveDate(chatId, text, mode);
            case EXPENSE_WAIT_NOTE -> checkAndSaveNote(chatId, text, mode);

            default -> {
                refreshCurrentPanel(chatId, mode);
            }
        }
    }

    private void handleDocFile(Update update) {
        PanelMode mode = PanelMode.MOVE_DOWN;
        long chatId = update.getMessage().getChatId();

        String fileId = extractFileId(update);

        if (fileId == null) {
            ExpenseDraft d = draft(chatId);
            ui.panelKey(
                    chatId,
                    mode,
                    "docFile.invalid",
                    keyboardService.backInline(chatId, ExpenseCb.docFileSkip())
            );
            draftService.save(chatId, DRAFT_TYPE, d);
            return;
        }

        saveDocFileId(chatId, fileId);
        afterDocFile(chatId, mode);
    }

    private void showChoseObjectPanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_OBJECT);

        List<WorkObject> objects = take(workObjectService.listActiveTop50(), 8);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : PREVIEW_MENU;

        ui.panelKey(
                chatId,
                mode,
                "askObject",
                keyboardService.listTwoInOneInline(
                        chatId,
                        objects,
                        ExpenseCb.pickObject(),
                        ExpenseCb.newObject(),
                        backCallback
                )
        );
    }

    private void showNewObjectPanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_OBJECT_TEXT);
        ui.panelKey(
                chatId,
                mode,
                "object.askName",
                keyboardService.backInline(chatId, ExpenseCb.newObjectBack())
        );
    }

    private void createNewObjectAndShowNextMenu(long chatId, String text, PanelMode mode) {
        WorkObject wo = workObjectService.getOrCreate(text, chatId);
        saveObject(chatId, wo.getId());
        afterObject(chatId, mode);
    }

    private void showChooseNomenclaturePanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_NOMENCLATURE_PICK);

        List<Nomenclature> menu = buildNomenclatureMenu(chatId, 8);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.nomenclatureBack();

        ui.panelKey(
                chatId,
                mode,
                "nomenclature.choseOrAdd",
                keyboardService.listTwoInOneInline(
                        chatId,
                        menu,
                        ExpenseCb.pickNomenclature(),
                        ExpenseCb.newNomenclature(),
                        backCallback
                )
        );
    }

    private void showCreateNewNomenclaturePanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_NEW_NOMENCLATURE_TEXT);
        ui.panelKey(
                chatId,
                mode,
                "nomenclature.askNew",
                keyboardService.backInline(chatId, ExpenseCb.newNomenclatureBack())
        );
    }

    private void searchOrSelectNomenclaturePanel(long chatId, String text, PanelMode mode) {
        Optional<Nomenclature> nomenclature = nomenclatureService.findExact(text);

        if (nomenclature.isPresent()) {
            saveNomenclature(chatId, nomenclature.get().getId());
            afterNomenclature(chatId, mode);
            return;
        }
        showSimpleOrAskCreateNomenclaturePanel(chatId, text, mode);
    }

    private void showSimpleOrAskCreateNomenclaturePanel(long chatId, String text, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_NOMENCLATURE_SUGGEST);

        ExpenseDraft d = draft(chatId);
        d.pendingNomenclatureName = text;
        List<Nomenclature> nomenclatureList = nomenclatureService.searchSimple(text);
        draftService.save(chatId, DRAFT_TYPE, d);

        ui.panelKey(
                chatId,
                mode,
                "nomenclature.choseOrCreate",
                keyboardService.listTwoInOneInline(
                        chatId,
                        nomenclatureList,
                        ExpenseCb.pickNomenclature(),
                        ExpenseCb.createNomenclature(),
                        ExpenseCb.searchBackNomenclature()
                ),
                text
        );
    }

    private void createNewNomenclatureAndShowNextMenu(long chatId, String text, PanelMode mode) {
        createNewNomenclature(chatId, text);

        if (goConfirmIfNeeded(chatId, mode)) return;

        showChooseCounterpartyPanel(chatId, mode);
    }

    private void createNewNomenclature(long chatId, String text) {
        Nomenclature nomenclature = nomenclatureService.getOrCreate(text, chatId);
        saveNomenclature(chatId, nomenclature.getId());
    }

    private void showChooseCounterpartyPanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_CP_PICK);

        List<Counterparty> menu = buildCounterpartyMenu(chatId, 8);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.counterpartyBack();

        ui.panelKey(
                chatId,
                mode,
                "counterparty.choseOrCreate",
                keyboardService.listTwoInOneInlineWithSkip(
                        chatId,
                        menu,
                        ExpenseCb.pickCounterparty(),
                        ExpenseCb.newCounterparty(),
                        ExpenseCb.skipCounterparty(),
                        backCallback
                )
        );
    }

    private void showNewCounterpartyPanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_CP_TEXT);
        ui.panelKey(
                chatId,
                mode,
                "counterparty.askNew",
                keyboardService.backInline(chatId, ExpenseCb.newCounterpartyBack())
        );
    }

    private void createNewCounterpartyAndShowNextMenu(long chatId, String text, PanelMode mode) {
        createNewCounterparty(chatId, text);

        if (goConfirmIfNeeded(chatId, mode)) return;

        showEnterAmountPanel(chatId, mode);
    }

    private void createNewCounterparty(long chatId, String text) {
        Counterparty cp = counterpartyService.getOrCreate(text, chatId);
        saveCounterparty(chatId, cp.getId());
    }

    private void selectOrSearchCounterparty(long chatId, String text, PanelMode mode) {
        Optional<Counterparty> cp = counterpartyService.findExact(text);

        if (cp.isPresent()) {
            saveCounterparty(chatId, cp.get().getId());
            afterCounterparty(chatId, mode);
            return;
        }
        showSimpleOrAskCreateCounterpartyPanel(chatId, text, mode);
    }

    private void showSimpleOrAskCreateCounterpartyPanel(long chatId, String text, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_CP_SUGGEST);
        ExpenseDraft d = draft(chatId);

        d.pendingCounterpartyName = text;
        List<Counterparty> counterpartyList = counterpartyService.searchSimple(text);
        draftService.save(chatId, DRAFT_TYPE, d);

        ui.panelKey(
                chatId,
                mode,
                "counterparty.choseOrCreate",
                keyboardService.listTwoInOneInline(
                        chatId,
                        counterpartyList,
                        ExpenseCb.pickCounterparty(),
                        ExpenseCb.createCounterparty(),
                        ExpenseCb.searchBackCounterparty()
                ),
                text
        );
    }

    private void showEnterAmountPanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_AMOUNT);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.amountBack();

        ui.panelKey(
                chatId,
                mode,
                "expense.askAmount",
                keyboardService.backInline(chatId, backCallback)
        );
    }

    private void checkAndSaveAmount(long chatId, String raw, PanelMode mode) {
        BigDecimal amount = InputParseUtils.parseAmount(raw);

        if (amount == null || amount.signum() <= 0) {
            showErrorAmountPanel(chatId, mode);
            return;
        }

        saveAmount(chatId, amount);
        afterAmount(chatId, mode);
    }

    private void showErrorAmountPanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_AMOUNT);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.amountBack();

        ui.panelKey(
                chatId,
                mode,
                "amountInvalid",
                keyboardService.backInline(chatId, backCallback)
        );
    }

    private void showChooseDatePanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_DATE);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.dateBack();

        ui.panelKey(
                chatId,
                mode,
                "expense.askDate",
                keyboardService.datePickerInline(chatId, ExpenseCb.NS, backCallback)
        );
    }

    private void showEnterDatePanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_DATE_TEXT);
        ui.panelKey(
                chatId,
                mode,
                "expense.enterDate",
                keyboardService.backInline(chatId, ExpenseCb.manualDateBack())
        );
    }

    private void checkAndSaveDate(long chatId, String raw, PanelMode mode) {
        DateParseResult res = InputParseUtils.parseSmartDate(raw, LocalDate.now());

        if (res.error != DateParseResult.Error.NONE || res.date.isAfter(LocalDate.now())) {
            String keyMessage = res.error == DateParseResult.Error.NONE ? "dateInFuture" : "dateInvalid";
            showErrorDatePanel(chatId, keyMessage, mode);
            return;
        }

        saveDate(chatId, res.date);
        afterDate(chatId, mode);
    }

    private void showErrorDatePanel(long chatId, String keyMessage, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_DATE_TEXT);
        ui.panelKey(
                chatId,
                mode,
                keyMessage,
                keyboardService.backInline(chatId, ExpenseCb.errorDateBack())
        );
    }

    private void showChooseDocTypePanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_DOC_TYPE);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.docBack();

        ui.panelKey(
                chatId,
                mode,
                "receipt.ask",
                keyboardService.receiptInline(chatId, ExpenseCb.docPrefix(), backCallback)
        );
    }

    private void showAttachDocFilePanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_DOC_FILE);

        ExpenseDraft d = draft(chatId);
        String back = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.docFileBack();

        ui.panelKey(
                chatId,
                mode,
                "receipt.askFile",
                keyboardService.backSkipInline(chatId, back, ExpenseCb.docFileSkip())
        );
    }

    private void showEnterNotePanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_NOTE);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.noteBack();

        ui.panelKey(
                chatId,
                mode,
                "expense.askNote",
                keyboardService.skipInline(chatId, ExpenseCb.noteSkip(), backCallback)
        );
    }

    private void checkAndSaveNote(long chatId, String raw, PanelMode mode) {
        saveNote(chatId, normalizeNote(raw));
        showConfirmPanel(chatId, mode);
    }

    private void showConfirmPanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_CONFIRM);

        ExpenseDraft d = draft(chatId);

        String obj = workObjectService.findActiveById(d.objectId)
                .map(WorkObject::getName)
                .orElse("—");

        String item = nomenclatureService.findActiveById(d.nomenclatureId)
                .map(Nomenclature::getName)
                .orElse("—");

        String cp = counterpartyService.findActiveById(d.counterpartyId)
                .map(Counterparty::getName)
                .orElse("—");

        String doc = "—";
        if (d.docType != null) {
            String key = switch (d.docType) {
                case RECEIPT -> "expense.doc.receipt";
                case INVOICE -> "expense.doc.invoice";
                case NO_RECEIPT -> "expense.doc.none";
            };

            doc = ui.msg(chatId, key);

            if (d.docType != DocType.NO_RECEIPT) {
                String fileKey = (d.docFileId != null)
                        ? "expense.doc.file.ok"
                        : "expense.doc.file.miss";
                doc += " " + ui.msg(chatId, fileKey);
            }
        }

        boolean showAttach = d.docType != null && d.docType != DocType.NO_RECEIPT;

        String attachKey = (d.docFileId == null || d.docFileId.isBlank())
                ? "btnAttachFile"
                : "btnReplaceFile";

        ui.panelKey(
                chatId,
                mode,
                "expense.confirm",
                keyboardService.confirmExpenseInline(chatId, ExpenseCb.NS, showAttach, attachKey),
                obj,
                item,
                cp,
                d.amount,
                d.date,
                doc,
                (d.note == null ? "—" : d.note)
        );
    }

    private void afterObject(long chatId, PanelMode mode) {
        if (goConfirmIfNeeded(chatId, mode)) return;
        showChooseNomenclaturePanel(chatId, mode);
    }

    private void afterNomenclature(long chatId, PanelMode mode) {
        if (goConfirmIfNeeded(chatId, mode)) return;
        showChooseCounterpartyPanel(chatId, mode);
    }

    private void afterCounterparty(long chatId, PanelMode mode) {
        if (goConfirmIfNeeded(chatId, mode)) return;
        showEnterAmountPanel(chatId, mode);
    }

    private void afterAmount(long chatId, PanelMode mode) {
        if (goConfirmIfNeeded(chatId, mode)) return;
        showChooseDatePanel(chatId, mode);
    }

    private void afterDate(long chatId, PanelMode mode) {
        if (goConfirmIfNeeded(chatId, mode)) return;
        showChooseDocTypePanel(chatId, mode);
    }

    private void afterDocType(long chatId, PanelMode mode) {
        if (goConfirmIfNeeded(chatId, mode)) return;

        ExpenseDraft d = draft(chatId);
        if (d.docType != null && d.docType.needsFile()) {
            showAttachDocFilePanel(chatId, mode);
        } else {
            showEnterNotePanel(chatId, mode);
        }
    }

    private void afterDocFile(long chatId, PanelMode mode) {
        if (goConfirmIfNeeded(chatId, mode)) return;
        showEnterNotePanel(chatId, mode);
    }


    private void saveExpense(long chatId, PanelMode mode) {
        ExpenseDraft d = draft(chatId);

        // ---- mandatory checks ----
        if (d.objectId == null) {
            setConfirm(chatId, true);
            showChoseObjectPanel(chatId, mode);
            return;
        }
        if (d.nomenclatureId == null) {
            setConfirm(chatId, true);
            showChooseNomenclaturePanel(chatId, mode);
            return;
        }
        if (d.amount == null) {
            setConfirm(chatId, true);
            showEnterAmountPanel(chatId, mode);
            return;
        }
        if (d.date == null) {
            setConfirm(chatId, true);
            showChooseDatePanel(chatId, mode);
            return;
        }
        if (d.docType == null) {
            setConfirm(chatId, true);
            showChooseDocTypePanel(chatId, mode);
            return;
        }

        expenseSaveService.saveExpense(
                chatId,
                d.objectId,
                d.nomenclatureId,
                d.counterpartyId,
                d.docType,
                d.amount,
                d.date,
                d.note,
                d.docFileId
        );

        String name = userSessionService.displayName(chatId);

        goToMainMenu(chatId, "expense.saved", mode, name, d.amount, d.date);
    }

    private void goToMainMenu(long chatId, String keyMessage, PanelMode mode, Object... args) {
        draftService.clear(chatId, DRAFT_TYPE);
        userSessionService.setUserState(chatId, UserState.IDLE);

        ui.panelKey(
                chatId,
                mode,
                keyMessage,
                keyboardService.mainMenuInline(chatId),
                args
        );
    }

    private void saveObject(long chatId, Long objectId) {
        ExpenseDraft d = draft(chatId);
        d.objectId = objectId;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void saveNomenclature(long chatId, Long nomenclatureId) {
        ExpenseDraft d = draft(chatId);
        d.nomenclatureId = nomenclatureId;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void saveCounterparty(long chatId, Long counterpartyId) {
        ExpenseDraft d = draft(chatId);
        d.counterpartyId = counterpartyId;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void saveAmount(long chatId, BigDecimal amount) {
        ExpenseDraft d = draft(chatId);
        d.amount = amount;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void saveDate(long chatId, LocalDate date) {
        ExpenseDraft d = draft(chatId);
        d.date = date;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void saveDocType(long chatId, DocType docType) {
        ExpenseDraft d = draft(chatId);
        d.docType = docType;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void saveDocFileId(long chatId, String docFileId) {
        ExpenseDraft d = draft(chatId);
        d.docFileId = docFileId;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void saveNote(long chatId, String note) {
        ExpenseDraft d = draft(chatId);
        d.note = note;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private boolean goConfirmIfNeeded(long chatId, PanelMode mode) {
        ExpenseDraft d = draft(chatId);

        if (!d.returnToConfirm) return false;

        setConfirm(chatId, false);
        showConfirmPanel(chatId, mode);
        return true;
    }

    private void setConfirm(long chatId, boolean confirm) {
        ExpenseDraft d = draft(chatId);
        d.returnToConfirm = confirm;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void refreshCurrentPanel(long chatId, PanelMode mode) {
        UserState st = userSessionService.getUserState(chatId);

        switch (st) {
            case EXPENSE_WAIT_OBJECT -> showChoseObjectPanel(chatId, mode);
            case EXPENSE_WAIT_DOC_TYPE -> showChooseDocTypePanel(chatId, mode);
            case EXPENSE_WAIT_DOC_FILE -> showAttachDocFilePanel(chatId, mode);
            case EXPENSE_WAIT_DATE -> showChooseDatePanel(chatId, mode);
            case EXPENSE_CONFIRM -> showConfirmPanel(chatId, mode);

            default -> { /* ничего */ }
        }
    }

    private String extractFileId(Update update) {
        if (!update.hasMessage()) return null;

        var msg = update.getMessage();

        if (msg.hasDocument()) {
            return msg.getDocument().getFileId();
        }

        if (msg.hasPhoto() && msg.getPhoto() != null && !msg.getPhoto().isEmpty()) {
            // выбираем самое “тяжёлое” фото (обычно самое большое по размеру)
            String bestId = null;
            int bestSize = -1;

            for (var p : msg.getPhoto()) {
                Integer sz = p.getFileSize();
                int size = (sz != null ? sz : 0);
                if (size > bestSize) {
                    bestSize = size;
                    bestId = p.getFileId();
                }
            }
            return bestId;
        }

        if (msg.hasText()) return msg.getText();

        return null;
    }


    /* -------------------- menu builders -------------------- */

    private List<Nomenclature> buildNomenclatureMenu(long chatId, int limit) {
        List<Long> ids = expenseService.suggestNomenclatureIds(chatId, limit);

        List<Nomenclature> suggested = nomenclatureService.loadActiveInOrder(ids);
        if (suggested.size() >= limit) return suggested;

        List<Nomenclature> top50 = nomenclatureService.listActiveTop50();
        Set<Long> used = new HashSet<>();
        for (Nomenclature n : suggested) used.add(n.getId());

        var out = new java.util.ArrayList<Nomenclature>(limit);
        out.addAll(suggested);

        for (Nomenclature n : top50) {
            if (n != null && used.add(n.getId())) {
                out.add(n);
                if (out.size() >= limit) break;
            }
        }

        return out;
    }

    private List<Counterparty> buildCounterpartyMenu(long chatId, int limit) {
        ExpenseDraft d = draft(chatId);

        List<Counterparty> suggested = List.of();
        if (d.nomenclatureId != null) {
            suggested = expenseService.suggestCounterparty(chatId, d.nomenclatureId, limit);
        }

        List<Counterparty> top50 = counterpartyService.listActiveTop50();
        Set<Long> used = new HashSet<>();
        var out = new java.util.ArrayList<Counterparty>(limit);

        for (Counterparty cp : suggested) {
            if (cp != null && used.add(cp.getId())) {
                out.add(cp);
                if (out.size() >= limit) return out;
            }
        }
        for (Counterparty cp : top50) {
            if (cp != null && used.add(cp.getId())) {
                out.add(cp);
                if (out.size() >= limit) break;
            }
        }
        return out;
    }

    private static <T> List<T> take(List<T> list, int limit) {
        if (list == null) return List.of();
        if (list.size() <= limit) return list;
        return list.subList(0, limit);
    }

    private String normalizeNote(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        return s.isBlank() ? null : s;
    }

    private ExpenseDraft draft(long chatId) {
        return draftService.get(chatId, DRAFT_TYPE, ExpenseDraft.class);
    }

    @Override
    public String getCommand() {
        return CommandName.EXPENSE.getName();
    }
}
