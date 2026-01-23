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
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
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
            UserState.EXPENSE_WAIT_NOTE,
            UserState.EXPENSE_WAIT_PHOTO,
            UserState.EXPENSE_CONFIRM
    );

    private final KeyboardService keyboardService;
    private final UserSessionService userSessionService;
    private final DraftService draftService;
    private final OperationRepository operationRepository;
    private final BotUi ui;
    private final WorkObjectService workObjectService;
    private final NomenclatureService nomenclatureService;
    private final CounterpartyService counterpartyService;
    private final ExpenseService expenseService;

    public ExpenseCommand(
            KeyboardService keyboardService,
            UserSessionService userSessionService,
            DraftService draftService,
            OperationRepository operationRepository,
            BotUi ui,
            WorkObjectService workObjectService,
            NomenclatureService nomenclatureService, CounterpartyService counterpartyService, ExpenseService expenseService
    ) {
        this.keyboardService = keyboardService;
        this.userSessionService = userSessionService;
        this.draftService = draftService;
        this.operationRepository = operationRepository;
        this.ui = ui;
        this.workObjectService = workObjectService;
        this.nomenclatureService = nomenclatureService;
        this.counterpartyService = counterpartyService;
        this.expenseService = expenseService;
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

            if (update.getMessage().hasPhoto()) {
                return st == UserState.EXPENSE_WAIT_PHOTO;
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

        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            handlePhoto(update);
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
                showChoseNomenclaturePanel(chatId, mode);
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
                showChoseNomenclaturePanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isSearchBackNomenclaturePick(data)) {
                showChoseNomenclaturePanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isCreateNomenclaturePick(data)) {
                ExpenseDraft d = draft(chatId);
                if (d.pendingNomenclatureName != null && !d.pendingNomenclatureName.isBlank()) {
                    createNewNomenclature(chatId, d.pendingNomenclatureName);
                }
                showChoseCounterpartyPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isNomenclaturePick(data)) {
                saveNomenclature(chatId, ExpenseCb.pickNomenclatureId(data));
                showChoseCounterpartyPanel(chatId, mode);
                return;
            }
        }

        if (ExpenseCb.isCounterparty(data)) {
            if (ExpenseCb.isCounterpartyBackPick(data)) {
                showChoseNomenclaturePanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isCounterpartySkipPick(data)) {
                saveCounterparty(chatId, null);
                showEnterAmountPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isCounterpartyNewPick(data)) {
                showNewCounterpartyPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isNewCounterpartyBackPick(data)) {
                showChoseCounterpartyPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isSearchBackCounterpartyPick(data)) {
                showChoseCounterpartyPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isCreateCounterpartyPick(data)) {
                ExpenseDraft d = draft(chatId);
                if (d.pendingCounterpartyName != null && !d.pendingCounterpartyName.isBlank()) {
                    createNewCounterparty(chatId, d.pendingCounterpartyName);
                }
                showEnterAmountPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isCounterpartyPick(data)) {
                saveCounterparty(chatId, ExpenseCb.pickCounterpartyId(data));
                showEnterAmountPanel(chatId, mode);
                return;
            }
        }

        if (ExpenseCb.isAmountBackPick(data)
                || ExpenseCb.isAmountErrorBackPick(data)) {
            showChoseCounterpartyPanel(chatId, mode);
            return;
        }

        if (ExpenseCb.isDate(data)) {
            if (ExpenseCb.isDateBackPick(data)) {
                showEnterAmountPanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isDateManualBackPick(data)
                    || ExpenseCb.isDateErrorBackPick(data)){
                showChooseDatePanel(chatId, mode);
                return;
            }

            if (ExpenseCb.isDateManualPick(data)) {
                showEnterDatePanel(chatId, mode);
                return;
            }

            LocalDate date = LocalDate.now();
            if (ExpenseCb.isDateYesterdayPick(data)) date = date.minusDays(1);

            ExpenseDraft d = draft(chatId);
            d.date = date;
            draftService.save(chatId, DRAFT_TYPE, d);

//            if (d.returnToConfirm) {
//                setConfirm(chatId, false);
//                showConfirmPanel(chatId, mode);
//                return;
//            }

            showEnterNotePanel(chatId, mode);
            return;
        }

        if (ExpenseCb.isNoteBackPick(data)) {
            showChooseDatePanel(chatId, mode);
            return;
        }

        if (ExpenseCb.isNoteSkipPick(data)) {
            ExpenseDraft d = draft(chatId);
            d.note = null;
            draftService.save(chatId, DRAFT_TYPE, d);

            if (d.returnToConfirm) {
                setConfirm(chatId, false);
            }

//            showReceiptPanel(chatId, mode);
            return;
        }



        if (ExpenseCb.isReceipt(data)) {
            if (ExpenseCb.isReceiptBackPick(data)) {
                // вернуться на предыдущий шаг
                return;
            }
            ExpenseDraft d = draft(chatId);
            d.receiptType = com.sonumax2.javabot.domain.operation.ReceiptType.valueOf(ExpenseCb.receiptType(data));
            draftService.save(chatId, DRAFT_TYPE, d);

            // дальше -> confirm или следующий шаг
            return;
        }


    }

    private void showReceiptMenu(long chatId, int messageId) {
        // добавь новый стейт, если хочешь: EXPENSE_WAIT_RECEIPT
        ui.editKey(chatId, messageId, "receipt.ask",
                keyboardService.receiptInline(chatId, ExpenseCb.receiptPrefix(), /*back*/ ExpenseCb.noteBack()));
    }


    private void handleText(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        UserState st = userSessionService.getUserState(chatId);
        PanelMode mode = PanelMode.MOVE_DOWN;

        switch (st) {
            case EXPENSE_WAIT_OBJECT_TEXT -> createNewObjectAndShowNextMenu(chatId, text, mode);
            case EXPENSE_WAIT_NOMENCLATURE_PICK, EXPENSE_WAIT_NOMENCLATURE_SUGGEST -> searchOrSelectNomenclaturePanel(chatId, text, mode);
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

    private void handlePhoto(Update update) {
        long chatId = update.getMessage().getChatId();
        var photos = update.getMessage().getPhoto();
        if (photos == null || photos.isEmpty()) return;

        ExpenseDraft d = draft(chatId);
        d.photoFileId = photos.get(photos.size() - 1).getFileId();
        draftService.save(chatId, DRAFT_TYPE, d);

        // TODO: дальше — confirm/сохранение операции
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
        showChoseNomenclaturePanel(chatId, mode);
    }

    private void showChoseNomenclaturePanel(long chatId, PanelMode mode) {
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
            showChoseCounterpartyPanel(chatId, mode);
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
        showChoseCounterpartyPanel(chatId, mode);
    }

    private void createNewNomenclature(long chatId, String text) {
        Nomenclature nomenclature = nomenclatureService.getOrCreate(text, chatId);
        saveNomenclature(chatId, nomenclature.getId());
    }

    private void showChoseCounterpartyPanel(long chatId, PanelMode mode) {
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
            showEnterAmountPanel(chatId, mode);
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

        ExpenseDraft d = draft(chatId);
        d.amount = amount;

        boolean toConfirm = d.returnToConfirm;
        d.returnToConfirm = false;
        draftService.save(chatId, DRAFT_TYPE, d);

        if (toConfirm) {
//            showConfirmPanel(chatId, mode);
        } else {
            showChooseDatePanel(chatId, mode);
        }
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
        ExpenseDraft d = draft(chatId);

        if (res.error != DateParseResult.Error.NONE || res.date.isAfter(LocalDate.now())) {
            String keyMessage = res.error == DateParseResult.Error.NONE ? "dateInFuture" : "dateInvalid";
            showErrorDatePanel(chatId, keyMessage, mode);
            return;
        }

        d.date = res.date;
        boolean toConfirm = d.returnToConfirm;
        d.returnToConfirm = false;

        draftService.save(chatId, DRAFT_TYPE, d);

        if (toConfirm) {
//            showConfirmPanel(chatId, mode);
        } else {
            showEnterNotePanel(chatId, mode);
        }
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
        ExpenseDraft d = draft(chatId);
        d.note = normalizeNote(raw);

        if (d.returnToConfirm) {
            d.returnToConfirm = false;
        }

        draftService.save(chatId, DRAFT_TYPE, d);
//        showReceiptPanel(chatId, mode);
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

    private void setConfirm(long chatId, boolean confirm) {
        ExpenseDraft d = draft(chatId);
        d.returnToConfirm = confirm;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void refreshCurrentPanel(long chatId, PanelMode mode) {
        UserState st = userSessionService.getUserState(chatId);

        switch (st) {
            case EXPENSE_WAIT_OBJECT -> showChoseObjectPanel(chatId, mode);

            default -> { /* ничего */ }
        }
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
