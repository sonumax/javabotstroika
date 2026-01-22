package com.sonumax2.javabot.bot.commands.ops;

import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.commands.cb.AdvanceCb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.domain.draft.AdvanceDraft;
import com.sonumax2.javabot.domain.operation.Operation;
import com.sonumax2.javabot.domain.operation.OperationType;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import com.sonumax2.javabot.domain.session.UserState;
import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.domain.draft.service.DraftService;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.domain.session.service.UserSessionService;
import com.sonumax2.javabot.util.DateParseResult;
import com.sonumax2.javabot.util.InputParseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

@Component
public class AdvanceCommand implements Command {

    private static final Logger log = LoggerFactory.getLogger(AdvanceCommand.class);

    private static final String PREVIEW_MENU = CbParts.ADD_OPR;

    private static final String DRAFT_TYPE = "ADVANCE";

    private static final EnumSet<UserState> ADV_TEXT_STATES = EnumSet.of(
            UserState.ADVANCE_WAIT_DATE_TEXT,
            UserState.ADVANCE_WAIT_AMOUNT,
            UserState.ADVANCE_WAIT_NOTE
    );

    private final KeyboardService keyboardService;
    private final UserSessionService userSessionService;
    private final DraftService draftService;
    private final OperationRepository operationRepository;
    private final BotUi ui;

    public AdvanceCommand(
            KeyboardService keyboardService,
            UserSessionService userSessionService,
            DraftService draftService,
            OperationRepository operationRepository, BotUi ui
    ) {
        this.keyboardService = keyboardService;
        this.userSessionService = userSessionService;
        this.draftService = draftService;
        this.operationRepository = operationRepository;
        this.ui = ui;
    }

    @Override
    public boolean canHandle(Update update) {
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            String data = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            UserState st = userSessionService.getUserState(chatId);

            if (AdvanceCb.isStartPick(data)) return true;

            if (AdvanceCb.isDatePick(data) || AdvanceCb.isDateErrorBackPick(data)) {
                return st == UserState.ADVANCE_WAIT_DATE || st == UserState.ADVANCE_WAIT_DATE_TEXT;
            }

            if (AdvanceCb.isAmountBackPick(data) || AdvanceCb.isAmountErrorBackPick(data)) {
                return st == UserState.ADVANCE_WAIT_AMOUNT;
            }

            if (AdvanceCb.isNoteSkipPick(data) || AdvanceCb.isNoteBackPick(data)) {
                return st == UserState.ADVANCE_WAIT_NOTE;
            }

            if (AdvanceCb.isConfirmPick(data)) {
                if (AdvanceCb.isConfirmBackPick(data)) {
                    if (st == UserState.ADVANCE_CONFIRM) return true;
                    if (st == UserState.ADVANCE_WAIT_DATE
                            || st == UserState.ADVANCE_WAIT_DATE_TEXT
                            || st == UserState.ADVANCE_WAIT_AMOUNT
                            || st == UserState.ADVANCE_WAIT_NOTE) {
                        return draft(chatId).returnToConfirm;
                    }
                    return false;
                }
                return st == UserState.ADVANCE_CONFIRM;
            }

            return false;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            UserState st = userSessionService.getUserState(chatId);
            return ADV_TEXT_STATES.contains(st);
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

        if (AdvanceCb.isStartPick(data)) {
            draftService.clear(chatId, DRAFT_TYPE);

            AdvanceDraft d = draft(chatId);
            d.panelMessageId = messageId;
            draftService.save(chatId, DRAFT_TYPE, d);

            showChooseDateMenuEditMessage(chatId, messageId);
            return;
        }

        if (AdvanceCb.isDateErrorBackPick(data)
                || AdvanceCb.isDateManualBackPick(data)
                || AdvanceCb.isAmountBackPick(data)
                || AdvanceCb.isAmountErrorBackPick(data)) {
            showChooseDateMenuEditMessage(chatId, messageId);
            return;
        }

        if (AdvanceCb.isDatePick(data)) {
            AdvanceDraft d = draft(chatId);
            if (AdvanceCb.isDateManualPick(data)) {
                showEnterDateEditMessage(chatId, messageId);
                return;
            }

            LocalDate date = LocalDate.now();
            if (AdvanceCb.isDateYesterdayPick(data)) date = date.minusDays(1);

            d.date = date;
            draftService.save(chatId, DRAFT_TYPE,d);

            if (d.returnToConfirm) {
                d.returnToConfirm = false;
                draftService.save(chatId, DRAFT_TYPE,d);
                showConfirmMenuNewMessage(chatId);
                return;
            }

            showEnterAmountMenuEditKey(chatId, d.panelMessageId);
            return;
        }

        if (AdvanceCb.isNoteBackPick(data)) {
            showEnterAmountMenuEditKey(chatId, messageId);
            return;
        }

        if (AdvanceCb.isNoteSkipPick(data)) {
            AdvanceDraft d = draft(chatId);
            d.note = null;
            draftService.save(chatId, DRAFT_TYPE,d);

            if (d.returnToConfirm) {
                d.returnToConfirm = false;
                draftService.save(chatId, DRAFT_TYPE,d);
            }

            showConfirmMenuEditKey(chatId, messageId, d.amount, d.date, d.note);
            return;
        }

        if (AdvanceCb.isConfirmPick(data)) {
            if (AdvanceCb.isConfirmSavePick(data)) {
                saveAdvance(chatId, messageId);
                return;
            }
            if (AdvanceCb.isConfirmEditDatePick(data)) {
                AdvanceDraft d = draft(chatId);
                d.returnToConfirm = true;
                draftService.save(chatId, DRAFT_TYPE, d);

                showChooseDateMenuEditMessage(chatId, messageId);
                return;
            }
            if (AdvanceCb.isConfirmEditAmountPick(data)) {
                AdvanceDraft d = draft(chatId);
                d.returnToConfirm = true;
                draftService.save(chatId, DRAFT_TYPE, d);

                showEnterAmountMenuEditKey(chatId, messageId);
                return;
            }
            if (AdvanceCb.isConfirmEditNotePick(data)) {
                AdvanceDraft d = draft(chatId);
                d.returnToConfirm = true;
                draftService.save(chatId, DRAFT_TYPE, d);

                showEnterNoteMenuEditMessage(chatId, messageId);
                return;
            }
            if (AdvanceCb.isConfirmCancelPick(data)) {
                goToMainMenu(chatId, messageId, "cancelled");
                return;
            }
            if (AdvanceCb.isConfirmBackPick(data)) {
                AdvanceDraft d = draft(chatId);
                showConfirmMenuEditKey(chatId, messageId, d.amount, d.date, d.note);
            }
        }
    }

    private void showChooseDateMenuEditMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_DATE);

        AdvanceDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? AdvanceCb.confirmBack() : PREVIEW_MENU;

        ui.editKey(
                chatId,
                messageId,
                "advance.askDate",
                keyboardService.datePickerInline(chatId, AdvanceCb.NS, backCallback)
        );
    }

    private void showEnterDateEditMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_DATE_TEXT);
        ui.editKey(
                chatId,
                messageId,
                "advance.enterDate",
                keyboardService.backInline(chatId, AdvanceCb.manualDateBack())
        );
    }

    private void showEnterAmountMenuEditKey(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_AMOUNT);

        AdvanceDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.amountBack();

        ui.editKey(
                chatId,
                messageId,
                "advance.askAmount",
                keyboardService.backInline(chatId, backCallback)
        );
    }

    private void showEnterNoteMenuEditMessage(long chatId, int messageId) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_NOTE);

        AdvanceDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.noteBack();

        ui.editKey(
                chatId,
                messageId,
                "advance.askNote",
                keyboardService.skipInline(chatId, AdvanceCb.noteSkip(), backCallback)
        );
    }

    private void showConfirmMenuEditKey(long chatId, int messageId, BigDecimal amount, LocalDate date, String note) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_CONFIRM);
        ui.editKey(
                chatId,
                messageId,
                "adv.confirm",
                keyboardService.confirmInline(chatId, AdvanceCb.NS),
                amount,
                date,
                (note == null ? "—" : note)
        );
    }



    private void handleText(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        UserState st = userSessionService.getUserState(chatId);

        switch (st) {
            case ADVANCE_WAIT_DATE_TEXT -> checkAndSaveDate(chatId, text);
            case ADVANCE_WAIT_AMOUNT -> checkAndSaveAmount(chatId, text);
            case ADVANCE_WAIT_NOTE -> checkAndSaveNote(chatId, text);
            default -> {
                // ignore
            }
        }
    }

    private void checkAndSaveDate(long chatId, String raw) {
        DateParseResult res = InputParseUtils.parseSmartDate(raw, LocalDate.now());
        AdvanceDraft d = draft(chatId);

        if (res.error != DateParseResult.Error.NONE || res.date.isAfter(LocalDate.now())) {
            String keyMessage = res.error == DateParseResult.Error.NONE ? "dateInFuture" : "dateInvalid";
            showErrorDateNewMessage(chatId, keyMessage);
            return;
        }

        d.date = res.date;
        draftService.save(chatId, DRAFT_TYPE,d);

        if (d.returnToConfirm) {
            d.returnToConfirm = false;
            draftService.save(chatId, DRAFT_TYPE,d);
            showConfirmMenuNewMessage(chatId);
            return;
        }

        showEnterAmountMenuNewMessage(chatId);
    }

    private void showErrorDateNewMessage(long chatId, String keyMessage) {
        AdvanceDraft d = draft(chatId);
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_DATE_TEXT);
        d.panelMessageId = ui.replacePanelKey(
                chatId,
                d.panelMessageId,
                keyMessage,
                keyboardService.backInline(chatId, AdvanceCb.errorDateBack())
        );
        draftService.save(chatId, DRAFT_TYPE,d);
    }

    private void showEnterAmountMenuNewMessage(long chatId) {
        AdvanceDraft d = draft(chatId);
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_AMOUNT);
        d.panelMessageId = ui.movePanelDownKey(
                chatId,
                d.panelMessageId,
                "advance.askAmount",
                keyboardService.backInline(chatId, AdvanceCb.amountBack())
        );
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void checkAndSaveAmount(long chatId, String raw) {
        BigDecimal amount = InputParseUtils.parseAmount(raw);

        if (amount == null || amount.signum() <= 0) {
            showErrorAmountMenu(chatId);
            return;
        }

        AdvanceDraft d = draft(chatId);
        d.amount = amount;
        draftService.save(chatId, DRAFT_TYPE,d);

        if (d.returnToConfirm) {
            d.returnToConfirm = false;
            draftService.save(chatId, DRAFT_TYPE,d);
            showConfirmMenuNewMessage(chatId);
            return;
        }

        showEnterNoteMenuNewMessage(chatId);
    }

    private void showErrorAmountMenu(long chatId) {
        AdvanceDraft d = draft(chatId);
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_AMOUNT);
        d.panelMessageId = ui.replacePanelKey(
                chatId,
                d.panelMessageId,
                "amountInvalid",
                keyboardService.backInline(chatId, d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.amountBack())
        );
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void showEnterNoteMenuNewMessage(long chatId) {
        AdvanceDraft d = draft(chatId);
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_NOTE);
        d.panelMessageId = ui.replacePanelKey(
                chatId,
                d.panelMessageId,
                "advance.askNote",
                keyboardService.skipInline(chatId, AdvanceCb.noteSkip(), AdvanceCb.noteBack())
        );
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void checkAndSaveNote(long chatId, String raw) {
        AdvanceDraft d = draft(chatId);
        d.note = normalizeNote(raw);
        draftService.save(chatId, DRAFT_TYPE,d);

        if (d.returnToConfirm) {
            d.returnToConfirm = false;
            draftService.save(chatId, DRAFT_TYPE,d);
        }

        showConfirmMenuNewMessage(chatId);
    }

    private void showConfirmMenuNewMessage(long chatId) {
        AdvanceDraft d = draft(chatId);
        userSessionService.setUserState(chatId, UserState.ADVANCE_CONFIRM);

        d.panelMessageId = ui.replacePanelKey(
                chatId,
                d.panelMessageId,
                "adv.confirm",
                keyboardService.confirmInline(chatId, AdvanceCb.NS),
                d.amount,
                d.date,
                (d.note == null ? "—" : d.note)
        );
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void saveAdvance(long chatId, int messageId) {
        AdvanceDraft d = draft(chatId);
        String note = d.note;

        log.info("ADV save: chatId={}, date={}, amount={}, note={}", chatId, d.date, d.amount, note);

        if (d.date == null) {
            showDateChoseDateMenuNewMessage(chatId);
            return;
        }

        if (d.amount == null) {
            showEnterAmountMenuNewMessage(chatId);
            return;
        }

        Operation op = new Operation();
        op.setChatId(chatId);
        op.setOpType(OperationType.ADVANCE);
        op.setOpDate(d.date);
        op.setAmount(d.amount);
        op.setNote(note);
        op.setCreatedAt(LocalDateTime.now());

        operationRepository.save(op);

        String name = userSessionService.displayName(chatId);

        goToMainMenu(chatId, messageId, "advance.saved", name,  op.getAmount(), op.getOpDate());
    }

    private void showDateChoseDateMenuNewMessage(long chatId) {
        AdvanceDraft d = draft(chatId);
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_DATE);
        d.panelMessageId = ui.replacePanelKey(
                chatId,
                d.panelMessageId,
                "advance.askDate",
                keyboardService.datePickerInline(chatId, AdvanceCb.NS, AdvanceCb.confirmBack())
        );
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void goToMainMenu(long chatId, int messageId, String keyMessage, Object... args) {
        draftService.clear(chatId, DRAFT_TYPE);
        userSessionService.setUserState(chatId, UserState.IDLE);
        ui.editKey(
                chatId,
                messageId,
                keyMessage,
                keyboardService.mainMenuInline(chatId),
                args
        );
    }

    private String normalizeNote(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        return s.isBlank() ? null : s;
    }

    private AdvanceDraft draft (long chatId) {
        return draftService.get(chatId, DRAFT_TYPE, AdvanceDraft.class);
    }

    @Override
    public String getCommand() {
        return CommandName.ADVANCE.getName();
    }
}
