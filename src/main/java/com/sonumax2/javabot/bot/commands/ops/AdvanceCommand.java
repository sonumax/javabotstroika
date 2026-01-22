package com.sonumax2.javabot.bot.commands.ops;

import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.commands.cb.AdvanceCb;
import com.sonumax2.javabot.bot.commands.cb.CbParts;
import com.sonumax2.javabot.bot.commands.Command;
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

    private static final DraftType DRAFT_TYPE = DraftType.ADVANCE;

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
        int clickedMessageId = cq.getMessage().getMessageId();

        userSessionService.setPanelMessageId(chatId, clickedMessageId);

        ui.ack(cq.getId());

        if (AdvanceCb.isStartPick(data)) {
            draftService.clear(chatId, DRAFT_TYPE);
            showChooseDatePanel(chatId);
            return;
        }

        if (AdvanceCb.isDateErrorBackPick(data)
                || AdvanceCb.isDateManualBackPick(data)
                || AdvanceCb.isAmountBackPick(data)
                || AdvanceCb.isAmountErrorBackPick(data)) {
            showChooseDatePanel(chatId);
            return;
        }

        if (AdvanceCb.isDatePick(data)) {
            AdvanceDraft d = draft(chatId);
            if (AdvanceCb.isDateManualPick(data)) {
                showEnterDatePanel(chatId);
                return;
            }

            LocalDate date = LocalDate.now();
            if (AdvanceCb.isDateYesterdayPick(data)) date = date.minusDays(1);

            d.date = date;
            draftService.save(chatId, DRAFT_TYPE, d);

            if (d.returnToConfirm) {
                setConfirm(chatId, false);
                showConfirmPanel(chatId);
                return;
            }

            showEnterAmountPanel(chatId);
            return;
        }

        if (AdvanceCb.isNoteBackPick(data)) {
            showEnterAmountPanel(chatId);
            return;
        }

        if (AdvanceCb.isNoteSkipPick(data)) {
            AdvanceDraft d = draft(chatId);
            d.note = null;
            draftService.save(chatId, DRAFT_TYPE, d);

            if (d.returnToConfirm) {
                setConfirm(chatId, false);
            }

            showConfirmPanel(chatId);
            return;
        }

        if (AdvanceCb.isConfirmPick(data)) {
            if (AdvanceCb.isConfirmSavePick(data)) {
                saveAdvance(chatId);
                return;
            }
            if (AdvanceCb.isConfirmEditDatePick(data)) {
                setConfirm(chatId, true);
                showChooseDatePanel(chatId);
                return;
            }
            if (AdvanceCb.isConfirmEditAmountPick(data)) {
                setConfirm(chatId, true);
                showEnterAmountPanel(chatId);
                return;
            }
            if (AdvanceCb.isConfirmEditNotePick(data)) {
                setConfirm(chatId, true);
                showEnterNotePanel(chatId);
                return;
            }
            if (AdvanceCb.isConfirmCancelPick(data)) {
                goToMainMenu(chatId, "cancelled");
                return;
            }
            if (AdvanceCb.isConfirmBackPick(data)) {
                showConfirmPanel(chatId);
            }
        }
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

    private void showChooseDatePanel(long chatId) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_DATE);

        AdvanceDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? AdvanceCb.confirmBack() : PREVIEW_MENU;

        var kb = keyboardService.datePickerInline(chatId, AdvanceCb.NS, backCallback);
        String key = "advance.askDate";
        Integer pid = panelId(chatId);
        if (pid == null) {
            ui.movePanelDownKey(chatId, null, key, kb);
            return;
        }

        ui.editKey(chatId, pid, key, kb);
    }

    private void showEnterDatePanel(long chatId) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_DATE_TEXT);

        var kb = keyboardService.backInline(chatId, AdvanceCb.manualDateBack());
        String key = "advance.enterDate";

        Integer pid = panelId(chatId);
        if (pid == null) {
            ui.movePanelDownKey(chatId, null, key, kb);
            return;
        }

        ui.editKey(chatId, pid, key, kb);
    }

    private void checkAndSaveDate(long chatId, String raw) {
        DateParseResult res = InputParseUtils.parseSmartDate(raw, LocalDate.now());
        AdvanceDraft d = draft(chatId);

        if (res.error != DateParseResult.Error.NONE || res.date.isAfter(LocalDate.now())) {
            String keyMessage = res.error == DateParseResult.Error.NONE ? "dateInFuture" : "dateInvalid";
            showErrorDatePanel(chatId, keyMessage);
            return;
        }

        d.date = res.date;
        draftService.save(chatId, DRAFT_TYPE, d);

        if (d.returnToConfirm) {
            d.returnToConfirm = false;
            draftService.save(chatId, DRAFT_TYPE, d);
            showConfirmPanel(chatId);
            return;
        }

        showEnterAmountPanel(chatId);
    }

    private void showErrorDatePanel(long chatId, String keyMessage) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_DATE_TEXT);

        var kb = keyboardService.backInline(chatId, AdvanceCb.errorDateBack());

        Integer pid = panelId(chatId);
        if (pid == null) {
            ui.movePanelDownKey(chatId, null, keyMessage, kb);
            return;
        }

        ui.editKey(chatId, pid, keyMessage, kb);
    }

    private void showEnterAmountPanel(long chatId) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_AMOUNT);

        AdvanceDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.amountBack();

        var kb = keyboardService.backInline(chatId, backCallback);
        String key = "advance.askAmount";

        Integer pid = panelId(chatId);
        if (pid == null) {
            ui.movePanelDownKey(chatId, null, key, kb);
            return;
        }

        ui.editKey(chatId, pid, key, kb);
    }

    private void checkAndSaveAmount(long chatId, String raw) {
        BigDecimal amount = InputParseUtils.parseAmount(raw);

        if (amount == null || amount.signum() <= 0) {
            showErrorAmountPanel(chatId);
            return;
        }

        AdvanceDraft d = draft(chatId);
        d.amount = amount;
        draftService.save(chatId, DRAFT_TYPE, d);

        if (d.returnToConfirm) {
            d.returnToConfirm = false;
            draftService.save(chatId, DRAFT_TYPE, d);
            showConfirmPanel(chatId);
            return;
        }

        showEnterNotePanel(chatId);
    }

    private void showErrorAmountPanel(long chatId) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_AMOUNT);

        AdvanceDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.amountBack();

        var kb = keyboardService.backInline(chatId, backCallback);
        String key = "amountInvalid";

        Integer pid = panelId(chatId);
        if (pid == null) {
            ui.movePanelDownKey(chatId, null, key, kb);
            return;
        }

        ui.editKey(chatId, pid, key, kb);
    }

    private void showEnterNotePanel(long chatId) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_WAIT_NOTE);

        AdvanceDraft d = draft(chatId);
        String backCallback = d.returnToConfirm ? AdvanceCb.confirmBack() : AdvanceCb.noteBack();

        var kb = keyboardService.skipInline(chatId, AdvanceCb.noteSkip(), backCallback);
        String key = "advance.askNote";

        Integer pid = panelId(chatId);
        if (pid == null) {
            ui.movePanelDownKey(chatId, null, key, kb);
            return;
        }

        ui.editKey(chatId, pid, key, kb);
    }

    private void checkAndSaveNote(long chatId, String raw) {
        AdvanceDraft d = draft(chatId);
        d.note = normalizeNote(raw);
        draftService.save(chatId, DRAFT_TYPE, d);

        if (d.returnToConfirm) {
            d.returnToConfirm = false;
            draftService.save(chatId, DRAFT_TYPE, d);
        }

        showConfirmPanel(chatId);
    }

    private void showConfirmPanel(long chatId) {
        userSessionService.setUserState(chatId, UserState.ADVANCE_CONFIRM);

        AdvanceDraft d = draft(chatId);

        var kb = keyboardService.confirmInline(chatId, AdvanceCb.NS);
        String key = "adv.confirm";

        Integer pid = panelId(chatId);
        if (pid == null) {
            ui.movePanelDownKey(chatId, null, key, kb, d.amount, d.date, (d.note == null ? "—" : d.note));
            return;
        }

        ui.editKey(chatId, pid, key, kb, d.amount, d.date, (d.note == null ? "—" : d.note));
    }

    private void setConfirm(long chatId, boolean confirm) {
        AdvanceDraft d = draft(chatId);
        d.returnToConfirm = confirm;
        draftService.save(chatId, DRAFT_TYPE, d);
    }

    private void saveAdvance(long chatId) {
        AdvanceDraft d = draft(chatId);
        String note = d.note;

        log.info("ADV save: chatId={}, date={}, amount={}, note={}", chatId, d.date, d.amount, note);

        if (d.date == null) {
            showChooseDatePanel(chatId);
            return;
        }

        if (d.amount == null) {
            showEnterAmountPanel(chatId);
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

        goToMainMenu(chatId, "advance.saved", name, op.getAmount(), op.getOpDate());
    }

    private void goToMainMenu(long chatId, String keyMessage, Object... args) {
        draftService.clear(chatId, DRAFT_TYPE);
        userSessionService.setUserState(chatId, UserState.IDLE);

        var kb = keyboardService.mainMenuInline(chatId);

        Integer pid = panelId(chatId);
        if (pid == null) {
            ui.movePanelDownKey(chatId, null, keyMessage, kb, args);
        } else {
            ui.editKey(chatId, pid, keyMessage, kb, args);
        }
    }

    private String normalizeNote(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        return s.isBlank() ? null : s;
    }

    private Integer panelId(long chatId) {
        Long id = userSessionService.getPanelMessageId(chatId);
        return id == null ? null : id.intValue();
    }

    private AdvanceDraft draft(long chatId) {
        return draftService.get(chatId, DRAFT_TYPE, AdvanceDraft.class);
    }

    @Override
    public String getCommand() {
        return CommandName.ADVANCE.getName();
    }
}
