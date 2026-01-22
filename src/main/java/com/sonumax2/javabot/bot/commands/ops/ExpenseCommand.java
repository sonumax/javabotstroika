package com.sonumax2.javabot.bot.commands.ops;

import com.sonumax2.javabot.domain.draft.DraftType;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.cb.ExpenseCb;
import com.sonumax2.javabot.domain.operation.service.ExpenseService;
import com.sonumax2.javabot.domain.reference.service.CounterpartyService;
import com.sonumax2.javabot.domain.reference.service.NomenclatureService;
import com.sonumax2.javabot.domain.reference.service.WorkObjectService;
import com.sonumax2.javabot.domain.draft.ExpenseDraft;
import com.sonumax2.javabot.domain.reference.Counterparty;
import com.sonumax2.javabot.domain.reference.Nomenclature;
import com.sonumax2.javabot.domain.reference.WorkObject;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import com.sonumax2.javabot.domain.session.UserState;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.domain.draft.service.DraftService;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;

@Order(50)
@Component
public class ExpenseCommand implements Command {

    private static final Logger log = LoggerFactory.getLogger(ExpenseCommand.class);

    private static final String PREVIEW_MENU = CbParts.ADD_OPR;
    private static final DraftType DRAFT_TYPE = DraftType.EXPENSE;

    private static final EnumSet<UserState> TEXT_STATES = EnumSet.of(
            UserState.EXPENSE_WAIT_OBJECT_TEXT,
            UserState.EXPENSE_WAIT_NOMENCLATURE_PICK,
            UserState.EXPENSE_WAIT_NEW_NOMENCLATURE_TEXT,
            UserState.EXPENSE_WAIT_CP_TEXT,
            UserState.EXPENSE_WAIT_CP_PICK,
            UserState.EXPENSE_WAIT_AMOUNT,
            UserState.EXPENSE_WAIT_DATE_TEXT,
            UserState.EXPENSE_WAIT_NOTE
    );

    private static final EnumSet<UserState> FLOW_STATES = EnumSet.of(
            UserState.EXPENSE_WAIT_OBJECT,
            UserState.EXPENSE_WAIT_OBJECT_TEXT,
            UserState.EXPENSE_WAIT_NOMENCLATURE_PICK,
            UserState.EXPENSE_WAIT_NEW_NOMENCLATURE_TEXT,
            UserState.EXPENSE_WAIT_NOMENCLATURE_SUGGEST,
            UserState.EXPENSE_WAIT_CP_PICK,
            UserState.EXPENSE_WAIT_CP_TEXT,
            UserState.EXPENSE_WAIT_CP_KIND,
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

            if (update.getMessage().hasText()) {
                return TEXT_STATES.contains(st);
            }
            if (update.getMessage().hasPhoto()) {
                return st == UserState.EXPENSE_WAIT_PHOTO;
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

        ui.ack(cq.getId());

        //Start
        if (ExpenseCb.isStartPick(data)) {
            draftService.clear(chatId, DRAFT_TYPE);

            ExpenseDraft d = draft(chatId);
            d.panelMessageId = messageId;
            draftService.save(chatId, DRAFT_TYPE, d);

            showChoseObjectMenuEditMessage(chatId, messageId);
            return;
        }

        if (ExpenseCb.isObject(data)) {
            if (ExpenseCb.isObjectNewPick(data)) {
                showNewObjectMenuEditMessage(chatId, messageId);
                return;
            }

            if (ExpenseCb.isNewObjectBackPick(data)) {
                showChoseObjectMenuEditMessage(chatId, messageId);
                return;
            }

            if (ExpenseCb.isObjectPick(data)) {
                ExpenseDraft d = draft(chatId);
                d.objectId = ExpenseCb.pickObjectId(data);
                draftService.save(chatId, DRAFT_TYPE, d);

                showChoseNomenclatureMenuEditMessage(chatId, d.panelMessageId);
                return;
            }
        }

        if (ExpenseCb.isNomenclature(data)) {
            if (ExpenseCb.isNomenclatureBackPick(data)) {
                showChoseObjectMenuEditMessage(chatId, messageId);
                return;
            }

            if (ExpenseCb.isNomenclatureNewPick(data)) {
                showCreateNewNomenclatureMenuEditMessage(chatId, messageId);
                return;
            }

            if (ExpenseCb.isNewNomenclatureBackPick(data)) {
                showChoseNomenclatureMenuEditMessage(chatId, messageId);
                return;
            }

            if (ExpenseCb.isSearchBackNomenclaturePick(data)) {
                showChoseNomenclatureMenuEditMessage(chatId, messageId);
                return;
            }

            if (ExpenseCb.isCreateNomenclaturePick(data)) {
                ExpenseDraft d = draft(chatId);
                if (d.pendingNomenclatureName != null && !d.pendingNomenclatureName.isBlank()) {
                    createNewNomenclature(chatId, d.pendingNomenclatureName);
                }
                showChoseCounterpartyMenuEditMessage(chatId, d.panelMessageId);
                return;
            }

            if (ExpenseCb.isNomenclaturePick(data)) {
                ExpenseDraft d = draft(chatId);
                d.nomenclatureId = ExpenseCb.pickNomenclatureId(data);
                draftService.save(chatId, DRAFT_TYPE, d);

                showChoseCounterpartyMenuEditMessage(chatId, d.panelMessageId);
                return;
            }
        }

        if (ExpenseCb.isCounterparty(data)) {
            if (ExpenseCb.isCounterpartyBackPick(data)) {
                showChoseNomenclatureMenuEditMessage(chatId, messageId);
                return;
            }

            if (ExpenseCb.isCounterpartySkipPick(data)) {
                showEnterAmountMenuEditKey(chatId, messageId);
                return;
            }

            if (ExpenseCb.isNewCounterpartyBackPick(data)) {
                showChoseCounterpartyMenuEditMessage(chatId, messageId);
                return;
            }

            if (ExpenseCb.isCounterpartyNewPick(data)) {
                showNewCounterpartyMenuEditMessage(chatId, messageId);
                return;
            }

            if (ExpenseCb.isCounterpartyPick(data)) {
                ExpenseDraft d = draft(chatId);
                d.counterpartyId = ExpenseCb.pickCounterpartyId(data);
                draftService.save(chatId, DRAFT_TYPE, d);

                showEnterAmountMenuEditKey(chatId, d.panelMessageId);
                return;
            }

            if ("exp:cnp:search:back".equals(data)) {
                showChoseNomenclatureMenuEditMessage(chatId, messageId);
                return;
            }
        }

        if (ExpenseCb.isAmountBackPick(data)) {
            showChoseCounterpartyMenuEditMessage(chatId, messageId);
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

    private void showChoseObjectMenuEditMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_OBJECT);

        List<WorkObject> objects = take(workObjectService.listActiveTop50(), 8);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : PREVIEW_MENU;

        ui.editKey(
                chatId,
                messageId,
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

    private void showNewObjectMenuEditMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_OBJECT_TEXT);
        ui.editKey(
                chatId,
                messageId,
                "object.askName",
                keyboardService.backInline(chatId, ExpenseCb.newObjectBack())
        );
    }

    private void showChoseNomenclatureMenuEditMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_NOMENCLATURE_PICK);

        List<Nomenclature> menu = buildNomenclatureMenu(chatId, 8);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.nomenclatureBack();

        ui.editKey(
                chatId,
                messageId,
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

    private void showCreateNewNomenclatureMenuEditMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_NEW_NOMENCLATURE_TEXT);
        ui.editKey(
                chatId,
                messageId,
                "nomenclature.askNew",
                keyboardService.backInline(chatId, ExpenseCb.newNomenclatureBack())
        );
    }

    private void showChoseCounterpartyMenuEditMessage(long chatId, Integer messageId) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_CP_PICK);

        List<Counterparty> menu = buildCounterpartyMenu(chatId, 8);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.counterpartyBack();

        ui.editKey(
                chatId,
                messageId,
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

    private void showNewCounterpartyMenuEditMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_CP_TEXT);
        ui.editKey(
                chatId,
                messageId,
                "counterparty.askNew",
                keyboardService.backInline(chatId, ExpenseCb.newCounterpartyBack())
        );
    }

    private void showEnterAmountMenuEditKey(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_AMOUNT);
        ui.editKey(
                chatId,
                messageId,
                "askAmount",
                keyboardService.backInline(chatId, ExpenseCb.amountBack())
        );
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

        switch (st) {
            case EXPENSE_WAIT_OBJECT_TEXT -> createNewObjectAndShowNextMenu(chatId, text);
            case EXPENSE_WAIT_NOMENCLATURE_PICK -> selectOrSearchNomenclature(chatId, text);
            case EXPENSE_WAIT_NEW_NOMENCLATURE_TEXT -> createNewNomenclatureAndShowNextMenu(chatId, text);
            case EXPENSE_WAIT_CP_PICK -> selectOrSearchCounterparty(chatId, text);
            case EXPENSE_WAIT_CP_TEXT -> createNewCounterpartyAndShowNextMenu(chatId, text);

//            case ADVANCE_WAIT_DATE_TEXT -> onDateFromText(chatId, text);
//            case ADVANCE_WAIT_AMOUNT -> onAmountFromText(chatId, text);
//            case ADVANCE_WAIT_NOTE -> onNoteText(chatId, text);
            default -> {
                // ignore
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

    private void createNewObjectAndShowNextMenu(long chatId, String text) {
        createNewObject(chatId, text);
        showChoseNomenclatureMenuNewMessage(chatId, draft(chatId).panelMessageId);
    }

    private void createNewObject(long chatId, String text) {
        ExpenseDraft d = draft(chatId);
        WorkObject wo = workObjectService.getOrCreate(text, chatId);
        d.objectId = wo.getId();
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void showChoseNomenclatureMenuNewMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_NOMENCLATURE_PICK);

        List<Nomenclature> menu = buildNomenclatureMenu(chatId, 8);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.nomenclatureBack();

        d.panelMessageId = ui.replacePanelKey(
                chatId,
                messageId,
                "nomenclature.choseOrCreate",
                keyboardService.listTwoInOneInline(
                        chatId,
                        menu,
                        ExpenseCb.pickNomenclature(),
                        ExpenseCb.newNomenclature(),
                        backCallback
                )
        );

        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void createNewNomenclature(long chatId, String text) {
        ExpenseDraft d = draft(chatId);
        Nomenclature nomenclature = nomenclatureService.getOrCreate(text, chatId);
        d.nomenclatureId = nomenclature.getId();
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void selectOrSearchNomenclature(long chatId, String text) {
        ExpenseDraft d = draft(chatId);
        Optional<Nomenclature> nomenclature = nomenclatureService.findExact(text);

        if (nomenclature.isPresent()) {
            d.nomenclatureId = nomenclature.get().getId();
            draftService.save(chatId, DRAFT_TYPE, d);
            showChoseCounterpartyMenuNewMessage(chatId);
            return;
        }

        showSimpleOrAskCreateNomenclatureMenuNewMessage(chatId, text);
    }

    private void showSimpleOrAskCreateNomenclatureMenuNewMessage(long chatId, String text) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_NOMENCLATURE_SUGGEST);
        ExpenseDraft d = draft(chatId);

        d.pendingNomenclatureName = text;
        List<Nomenclature> nomenclatureList = nomenclatureService.searchSimple(text);

        d.panelMessageId = ui.replacePanelKey(
                chatId,
                d.panelMessageId,
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

        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void createNewNomenclatureAndShowNextMenu(long chatId, String text) {
        createNewNomenclature(chatId, text);
        showChoseCounterpartyMenuNewMessage(chatId);
    }

    private void showChoseCounterpartyMenuNewMessage(long chatId) {
        ExpenseDraft d = draft(chatId);
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_CP_PICK);

        List<Counterparty> menu = buildCounterpartyMenu(chatId, 8);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.counterpartyBack();

        d.panelMessageId = ui.movePanelDownKey(
                chatId,
                d.panelMessageId,
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

        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void createNewCounterpartyAndShowNextMenu(long chatId, String text) {
        createNewCounterparty(chatId, text);
        showEnterAmountMenuNewMessage(chatId);
    }

    private void createNewCounterparty(long chatId, String text) {
        ExpenseDraft d = draft(chatId);
        Counterparty cp = counterpartyService.getOrCreate(text, chatId);
        d.counterpartyId = cp.getId();
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void selectOrSearchCounterparty(long chatId, String text) {
        ExpenseDraft d = draft(chatId);
        Optional<Counterparty> cp = counterpartyService.findExact(text);

        if (cp.isPresent()) {
            d.counterpartyId = cp.get().getId();
            draftService.save(chatId, DRAFT_TYPE, d);
            showEnterAmountMenuNewMessage(chatId);
            return;
        }

        showSimpleOrAskCreateCounterpartyMenuNewMessage(chatId, text);
    }

    private void showSimpleOrAskCreateCounterpartyMenuNewMessage(long chatId, String text) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_CP_SUGGEST);
        ExpenseDraft d = draft(chatId);

        d.pendingCounterpartyName = text;
        List<Counterparty> counterpartyList = counterpartyService.searchSimple(text);

        d.panelMessageId = ui.replacePanelKey(
                chatId,
                d.panelMessageId,
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

        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void showEnterAmountMenuNewMessage(long chatId) {
        ExpenseDraft d = draft(chatId);
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_AMOUNT);
        d.panelMessageId = ui.movePanelDownKey(
                chatId,
                d.panelMessageId,
                "advance.askAmount",
                keyboardService.backInline(chatId, ExpenseCb.amountBack())
        );
        draftService.save(chatId, DRAFT_TYPE, d);
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

    private ExpenseDraft draft(long chatId) {
        return draftService.get(chatId, DRAFT_TYPE, ExpenseDraft.class);
    }

    @Override
    public String getCommand() {
        return CommandName.EXPENSE.getName();
    }
}
