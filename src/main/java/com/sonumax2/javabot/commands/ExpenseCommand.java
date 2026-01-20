package com.sonumax2.javabot.commands;

import com.sonumax2.javabot.commands.helper.Cb;
import com.sonumax2.javabot.commands.helper.CbParts;
import com.sonumax2.javabot.commands.helper.Command;
import com.sonumax2.javabot.commands.helper.ExpenseCb;
import com.sonumax2.javabot.model.ExpenseDraft;
import com.sonumax2.javabot.model.reference.BaseRefEntity;
import com.sonumax2.javabot.model.reference.Nomenclature;
import com.sonumax2.javabot.model.reference.WorkObject;
import com.sonumax2.javabot.model.repo.OperationRepository;
import com.sonumax2.javabot.model.UserState;
import com.sonumax2.javabot.service.*;
import com.sonumax2.javabot.service.bot.BotUi;
import com.sonumax2.javabot.service.bot.DraftService;
import com.sonumax2.javabot.service.bot.KeyboardService;
import com.sonumax2.javabot.service.bot.UserSessionService;
import com.sonumax2.javabot.util.TelegramCommandUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;

@Component
public class ExpenseCommand implements Command {

    private static final Logger log = LoggerFactory.getLogger(ExpenseCommand.class);

    private static final String PREVIEW_MENU = CbParts.ADD_OPR;

    private static final String DRAFT_TYPE = "EXPENSE";

    private static final EnumSet<UserState> TEXT_STATES = EnumSet.of(
        UserState.EXPENSE_WAIT_OBJECT_TEXT,
        UserState.EXPENSE_WAIT_NEW_NOMENCLATURE_TEXT,
        UserState.EXPENSE_WAIT_NOMENCLATURE_PICK,
        UserState.EXPENSE_WAIT_CP_TEXT,
        UserState.EXPENSE_WAIT_CP_PICK,
        UserState.EXPENSE_WAIT_AMOUNT,
        UserState.EXPENSE_WAIT_DATE_TEXT,
        UserState.EXPENSE_WAIT_NOTE
    );

    private static final EnumSet<UserState> EXP_FLOW_STATES = EnumSet.of(
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

    public ExpenseCommand(
            KeyboardService keyboardService,
            UserSessionService userSessionService,
            DraftService draftService,
            OperationRepository operationRepository,
            BotUi ui,
            WorkObjectService workObjectService,
            NomenclatureService nomenclatureService, CounterpartyService counterpartyService
    ) {
        this.keyboardService = keyboardService;
        this.userSessionService = userSessionService;
        this.draftService = draftService;
        this.operationRepository = operationRepository;
        this.ui = ui;
        this.workObjectService = workObjectService;
        this.nomenclatureService = nomenclatureService;
        this.counterpartyService = counterpartyService;
    }

    @Override
    public boolean canHandle(Update update) {

        // callbacks
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            String data = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            UserState st = userSessionService.getUserState(chatId);

            if (ExpenseCb.isStartPick(data)) return true;

            return data.startsWith(ExpenseCb.NS) && EXP_FLOW_STATES.contains(st);
        }


        // text
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
        }
    }

    private void handleCallback(Update update) {
        var cq = update.getCallbackQuery();
        String data = cq.getData();
        long chatId = cq.getMessage().getChatId();
        int messageId = cq.getMessage().getMessageId();

        ui.ack(cq.getId());

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
                showCreateNewObjectMenuEditMessage(chatId, messageId);
                return;
            }

            if(ExpenseCb.isNewObjectBackPick(data)){
                showChoseObjectMenuEditMessage(chatId, messageId);
                return;
            }

            if(ExpenseCb.isObjectPick(data)) {
                long objectId = Cb.tailLong(data, ExpenseCb.pickObject());
                ExpenseDraft d = draft(chatId);
                d.objectId = objectId;
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

            if(ExpenseCb.isNewNomenclatureBackPick(data)) {
                showChoseNomenclatureMenuEditMessage(chatId, messageId);
                return;
            }

            if(ExpenseCb.isCreateNomenclaturePick(data)) {
                ExpenseDraft d = draft(chatId);
                createNewNomenclature(chatId, d.pendingNomenclatureName);
                showChoseCounterpartyMenuEditMessage(chatId, d.panelMessageId);
                return;
            }

            if(ExpenseCb.isSearchBackNomenclaturePick(data)) {
                showChoseNomenclatureMenuEditMessage(chatId, messageId);
                return;
            }

            if(ExpenseCb.isNomenclaturePick(data)) {
                String idNomenclature = ExpenseCb.getCreateNomenclatureId(data);
                ExpenseDraft d = draft(chatId);
                d.nomenclatureId = Long.parseLong(idNomenclature);
                draftService.save(chatId, DRAFT_TYPE, d);

                showChoseCounterpartyMenuEditMessage(chatId, d.panelMessageId);
                return;
            }
        }

        if (ExpenseCb.isCounterparty(data)) {
            if (ExpenseCb.isCounterpartyCreatePick(data)) {
                showCreateNewCounterpartyMenuEditMessage(chatId, messageId);
                return;
            }

            if (ExpenseCb.isCounterpartyBackPick(data) ) {
                showChoseNomenclatureMenuEditMessage(chatId, messageId);
                return;
            }

            if("exp:cnp:create".equals(data)){
                ExpenseDraft d = draft(chatId);
                createNewNomenclature(chatId, d.pendingNomenclatureName);
                showEnterAmountMenuEditKey(chatId, d.panelMessageId);
                return;
            }

            if("exp:cnp:new:back".equals(data)) {
                showChoseCounterpartyMenuEditMessage(chatId, messageId);
                return;
            }

            if("exp:cnp:search:back".equals(data)) {
                showChoseNomenclatureMenuEditMessage(chatId, messageId);
                return;
            }

            if(data.startsWith("exp:cnp:pick:")) {
                String id = data.substring("exp:cnp:pick:".length());
                ExpenseDraft d = draft(chatId);
                d.nomenclatureId = Long.parseLong(id);
                draftService.save(chatId, DRAFT_TYPE, d);

                showChoseCounterpartyMenuEditMessage(chatId, d.panelMessageId);
                return;
            }
        }

        if(ExpenseCb.isAmountBackPick(data)) {
            //TODO добавить обработку возврата из суммы
            return;
        }


    }

    private void showChoseObjectMenuEditMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_OBJECT);
        List<WorkObject> workObjects = workObjectService.listActive();

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : PREVIEW_MENU;

        ui.editKey(
                chatId,
                messageId,
                "askObject",
                keyboardService.listTwoInOneInline(
                        chatId,
                        workObjects,
                        ExpenseCb.pickObject(),
                        ExpenseCb.newObject(),
                        backCallback
                )
        );
    }

    private void showCreateNewObjectMenuEditMessage(long chatId, int messageId) {
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



        List<? extends BaseRefEntity> nomenclatureList = recentRefService.recentEntities(chatId, RecentRefType.NOMENCLATURE);
        List<? extends BaseRefEntity> nomenclatureActiveList = nomenclatureService.listActiveTop50();

        List<BaseRefEntity> menu = TelegramCommandUtils.mergeRecentWithActive(nomenclatureList, nomenclatureActiveList, 8);

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

        List<? extends BaseRefEntity> counterpartyList = recentRefService.recentEntities(chatId, RecentRefType.COUNTERPARTY);
        List<? extends BaseRefEntity> counterpartyActiveList = counterpartyService.listActiveTop50();

        List<BaseRefEntity> menu = TelegramCommandUtils.mergeRecentWithActive(counterpartyList, counterpartyActiveList, 8);

        ExpenseDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? ExpenseCb.confirmBack() : ExpenseCb.counterpartyBack();

        ui.editKey(
                chatId,
                messageId,
                "expense.counterparty.choseOrCreate",
                keyboardService.listTwoInOneInline(
                        chatId,
                        menu,
                        ExpenseCb.pickCounterparty(),
                        ExpenseCb.createCounterparty(),
                        backCallback
                )
        );
    }

    private void showCreateNewCounterpartyMenuEditMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_CP_TEXT);
        ui.editKey(
                chatId,
                messageId,
                "counterparty.askNew",
                keyboardService.backInline(chatId, ExpenseCb.createCounterpartyBack())
        );
    }

    private void showEnterAmountMenuEditKey(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_AMOUNT);
        ui.editKey(
                chatId,
                messageId,
                "askAmount",
                keyboardService.backInline(chatId, "exp:amount:back")
        );
    }




    private void handleText(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        UserState st = userSessionService.getUserState(chatId);

        switch (st) {
            case EXPENSE_WAIT_OBJECT_TEXT -> createNewObjectAndShowNextMenu(chatId, text);
            case EXPENSE_WAIT_NOMENCLATURE_PICK -> selectOrSearchNomenclature(chatId, text);
            case EXPENSE_WAIT_NEW_NOMENCLATURE_TEXT -> createNewNomenclatureAndShowNextMenu(chatId, text);

//            case ADVANCE_WAIT_DATE_TEXT -> onDateFromText(chatId, text);
//            case ADVANCE_WAIT_AMOUNT -> onAmountFromText(chatId, text);
//            case ADVANCE_WAIT_NOTE -> onNoteText(chatId, text);
            default -> {
                // ignore
            }
        }
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

        List<? extends BaseRefEntity> nomenclatureList = recentRefService.recentEntities(chatId, RecentRefType.NOMENCLATURE);
        List<? extends BaseRefEntity> nomenclatureActiveList = nomenclatureService.listActiveTop50();

        List<BaseRefEntity> menu = TelegramCommandUtils.mergeRecentWithActive(nomenclatureList, nomenclatureActiveList, 8);

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

    private void createNewNomenclatureAndShowNextMenu(long chatId, String text) {
        createNewNomenclature(chatId, text);
        showChoseCounterpartyMenuNewMessage(chatId);
    }

    private void showSimpleOrAskCreateNomenclatureMenuNewMessage(long chatId, String text) {
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_NOMENCLATURE_SUGGEST);
        ExpenseDraft d = draft(chatId);

        d.pendingNomenclatureName = text;
        List<? extends BaseRefEntity> nomenclatureList = nomenclatureService.searchSimple(text);

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

    private void showChoseCounterpartyMenuNewMessage(long chatId) {
        ExpenseDraft d = draft(chatId);
        userSessionService.setUserState(chatId, UserState.EXPENSE_WAIT_CP_PICK);
        List<? extends BaseRefEntity> counterpartyList = recentRefService.recentEntities(chatId, RecentRefType.COUNTERPARTY);
        d.panelMessageId = ui.movePanelDownKey(
                chatId,
                d.panelMessageId,
                "expense.counterparty.choseOrCreate",
                keyboardService.listTwoInOneInline(
                        chatId,
                        counterpartyList,
                        "exp:cnp:pick:",
                        "exp:cnp:new",
                        "exp:cnp:back"
                )
        );
    }

    private ExpenseDraft draft(long chatId) {
        return draftService.get(chatId, DRAFT_TYPE, ExpenseDraft.class);
    }

    @Override
    public String getCommand() {
        return "EXPENSE";
    }
}
