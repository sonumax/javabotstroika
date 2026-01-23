package com.sonumax2.javabot.bot.commands.ops;

import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.commands.cb.AdvanceCb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.ui.PanelMode;
import com.sonumax2.javabot.domain.draft.AdvanceDraft;
import com.sonumax2.javabot.domain.draft.DraftType;
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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

@Order(50)
@Component
public class AdvanceCommand implements Command {

    private static final Logger log = LoggerFactory.getLogger(AdvanceCommand.class);

    private static final String PREVIEW_MENU = CbParts.ADD_OPR;
    private static final DraftType DRAFT_TYPE = DraftType.ADVANCE;

    private static final EnumSet<UserState> FLOW_STATES = EnumSet.of(
            UserState.ADVANCE_WAIT_DATE,
            UserState.ADVANCE_WAIT_DATE_TEXT,
            UserState.ADVANCE_WAIT_AMOUNT,
            UserState.ADVANCE_WAIT_NOTE,
            UserState.ADVANCE_CONFIRM
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
            return data.startsWith(AdvanceCb.NS) && FLOW_STATES.contains(st);
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            if (text != null && text.startsWith("/")) return false;

            UserState st = userSessionService.getUserState(chatId);

            return FLOW_STATES.contains(st);
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
        PanelMode mode = PanelMode.EDIT;

        ui.setPanelId(chatId, messageId);
        ui.ack(cq.getId());

        if (AdvanceCb.isStartPick(data)) {
            draftService.clear(chatId, DRAFT_TYPE);
            showChooseDatePanel(chatId, mode);
            return;
        }

        if (AdvanceCb.isDateErrorBackPick(data)
                || AdvanceCb.isDateManualBackPick(data)
                || AdvanceCb.isAmountBackPick(data)
                || AdvanceCb.isAmountErrorBackPick(data)) {
            showChooseDatePanel(chatId, mode);
            return;
        }

        if (AdvanceCb.isDate(data)) {
            AdvanceDraft d = draft(chatId);
            if (AdvanceCb.isDateManualPick(data)) {
                showEnterDatePanel(chatId, mode);
                return;
            }

            LocalDate date = LocalDate.now();
            if (AdvanceCb.isDateYesterdayPick(data)) date = date.minusDays(1);

            d.date = date;
            draftService.save(chatId, DRAFT_TYPE, d);

            if (d.returnToConfirm) {
                setConfirm(chatId, false);
                showConfirmPanel(chatId, mode);
                return;
            }

            showEnterAmountPanel(chatId, mode);
            return;
        }

        if (AdvanceCb.isNoteBackPick(data)) {
            showEnterAmountPanel(chatId, mode);
            return;
        }

        if (AdvanceCb.isNoteSkipPick(data)) {
            AdvanceDraft d = draft(chatId);
            d.note = null;
            draftService.save(chatId, DRAFT_TYPE, d);

            if (d.returnToConfirm) {
                setConfirm(chatId, false);
            }

            showConfirmPanel(chatId, mode);
            return;
        }

        if (AdvanceCb.isConfirmPick(data)) {
            if (AdvanceCb.isConfirmSavePick(data)) {
                saveAdvance(chatId, mode);
                return;
            }
            if (AdvanceCb.isConfirmEditDatePick(data)) {
                setConfirm(chatId, true);
                showChooseDatePanel(chatId, mode);
                return;
            }
            if (AdvanceCb.isConfirmEditAmountPick(data)) {
                setConfirm(chatId, true);
                showEnterAmountPanel(chatId, mode);
                return;
            }
            if (AdvanceCb.isConfirmEditNotePick(data)) {
                setConfirm(chatId, true);
                showEnterNotePanel(chatId, mode);
                return;
            }
            if (AdvanceCb.isConfirmCancelPick(data)) {
                goToMainMenu(chatId, "cancelled", mode);
                return;
            }
            if (AdvanceCb.isConfirmBackPick(data)) {
                showConfirmPanel(chatId, mode);
            }
        }
    }

    private void handleText(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        UserState st = userSessionService.getUserState(chatId);
        PanelMode mode = PanelMode.MOVE_DOWN;

        switch (st) {
            case ADVANCE_WAIT_DATE_TEXT -> checkAndSaveDate(chatId, text, mode);
            case ADVANCE_WAIT_AMOUNT -> checkAndSaveAmount(chatId, text, mode);
            case ADVANCE_WAIT_NOTE -> checkAndSaveNote(chatId, text, mode);
            default -> {
                refreshCurrentPanel(chatId, mode);
            }
        }
    }

    private void showChooseDatePanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_DATE);

        AdvanceDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? AdvanceCb.confirmBack() : PREVIEW_MENU;

        ui.panelKey(
                chatId,
                mode,
                "advance.askDate",
                keyboardService.datePickerInline(chatId, AdvanceCb.NS, backCallback)
        );
    }

    private void showEnterDatePanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_DATE_TEXT);
        ui.panelKey(
                chatId,
                mode,
                "advance.enterDate",
                keyboardService.backInline(chatId, AdvanceCb.manualDateBack())
        );
    }

    private void checkAndSaveDate(long chatId, String raw, PanelMode mode) {
        DateParseResult res = InputParseUtils.parseSmartDate(raw, LocalDate.now());
        AdvanceDraft d = draft(chatId);

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
            showConfirmPanel(chatId, mode);
        } else {
            showEnterAmountPanel(chatId, mode);
        }
    }

    private void showErrorDatePanel(long chatId, String keyMessage, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_DATE_TEXT);
        ui.panelKey(
                chatId,
                mode,
                keyMessage,
                keyboardService.backInline(chatId, AdvanceCb.errorDateBack())
        );
    }

    private void showEnterAmountPanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_AMOUNT);

        AdvanceDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.amountBack();

        ui.panelKey(
                chatId,
                mode,
                "advance.askAmount",
                keyboardService.backInline(chatId, backCallback)
        );
    }

    private void checkAndSaveAmount(long chatId, String raw, PanelMode mode) {
        BigDecimal amount = InputParseUtils.parseAmount(raw);

        if (amount == null || amount.signum() <= 0) {
            showErrorAmountPanel(chatId, mode);
            return;
        }

        AdvanceDraft d = draft(chatId);
        d.amount = amount;

        boolean toConfirm = d.returnToConfirm;
        d.returnToConfirm = false;
        draftService.save(chatId, DRAFT_TYPE, d);

        if (toConfirm) {
            showConfirmPanel(chatId, mode);
        } else {
            showEnterNotePanel(chatId, mode);
        }
    }

    private void showErrorAmountPanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_AMOUNT);

        AdvanceDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.amountBack();

        ui.panelKey(
                chatId,
                mode,
                "amountInvalid",
                keyboardService.backInline(chatId, backCallback)
        );
    }

    private void showEnterNotePanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_NOTE);

        AdvanceDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.noteBack();

        ui.panelKey(
                chatId,
                mode,
                "advance.askNote",
                keyboardService.skipInline(chatId, AdvanceCb.noteSkip(), backCallback)
        );
    }

    private void checkAndSaveNote(long chatId, String raw, PanelMode mode) {
        AdvanceDraft d = draft(chatId);
        d.note = normalizeNote(raw);

        if (d.returnToConfirm) {
            d.returnToConfirm = false;
        }

        draftService.save(chatId, DRAFT_TYPE, d);
        showConfirmPanel(chatId, mode);
    }

    private void showConfirmPanel(long chatId, PanelMode mode) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_CONFIRM);

        AdvanceDraft d = draft(chatId);
        ui.panelKey(
                chatId,
                mode,
                "adv.confirm",
                keyboardService.confirmInline(chatId, AdvanceCb.NS),
                d.amount,
                d.date,
                (d.note == null ? "—" : d.note)
        );
    }

    private void setConfirm(long chatId, boolean confirm) {
        AdvanceDraft d = draft(chatId);
        d.returnToConfirm = confirm;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void saveAdvance(long chatId, PanelMode mode) {
        AdvanceDraft d = draft(chatId);
        String note = d.note;

        log.info("ADV save: chatId={}, date={}, amount={}, note={}", chatId, d.date, d.amount, note);

        if (d.date == null) {
            showChooseDatePanel(chatId, mode);
            return;
        }

        if (d.amount == null) {
            showEnterAmountPanel(chatId, mode);
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

        goToMainMenu(chatId, "advance.saved", mode, name, op.getAmount(), op.getOpDate());
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

    private void refreshCurrentPanel(long chatId, PanelMode mode) {
        UserState st = userSessionService.getUserState(chatId);

        switch (st) {
            case ADVANCE_WAIT_DATE -> showChooseDatePanel(chatId, mode);
            case ADVANCE_CONFIRM -> showConfirmPanel(chatId, mode);

            default -> { /* ничего */ }
        }
    }

    private String normalizeNote(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        return s.isBlank() ? null : s;
    }

    private AdvanceDraft draft(long chatId) {
        return draftService.get(chatId, DRAFT_TYPE, AdvanceDraft.class);
    }

    @Override
    public String getCommand() {
        return CommandName.ADVANCE.getName();
    }
}
