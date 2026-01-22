//package com.sonumax2.javabot.commands;
//
//import com.sonumax2.javabot.bot.commands.Command;
//import com.sonumax2.javabot.model.OpsEditDraft;
//import com.sonumax2.javabot.model.users.UserState;
//import com.sonumax2.javabot.service.*;
//import com.sonumax2.javabot.util.InputParseUtils;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.objects.Update;
//
//import java.util.List;
//
//@Component
//public class OpsEditCommand implements Command {
//
//    private static final String EDIT_AMOUNT = "ops:edit:amount:";
//    private static final String EDIT_NOTE   = "ops:edit:note:";
//
//    private final BotMessageService botMessageService;
//    private final LocalizationService localizationService;
//    private final KeyboardService keyboardService;
//    private final UserSessionService userSessionService;
//    private final DraftService draftService;
//    private final OperationService operationService;
//
//    public OpsEditCommand(BotMessageService botMessageService,
//                          LocalizationService localizationService,
//                          KeyboardService keyboardService,
//                          UserSessionService userSessionService,
//                          DraftService draftService,
//                          OperationService operationService) {
//        this.botMessageService = botMessageService;
//        this.localizationService = localizationService;
//        this.keyboardService = keyboardService;
//        this.userSessionService = userSessionService;
//        this.draftService = draftService;
//        this.operationService = operationService;
//    }
//
//    @Override
//    public boolean canHandle(Update update) {
//        if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
//            String d = update.getCallbackQuery().getData();
//            return d.startsWith(EDIT_AMOUNT) || d.startsWith(EDIT_NOTE);
//        }
//
//        if (update.hasMessage() && update.getMessage().hasText()) {
//            long chatId = update.getMessage().getChatId();
//            var st = userSessionService.getUserState(chatId);
//            return st == UserState.OPS_EDIT_AMOUNT || st == UserState.OPS_EDIT_NOTE;
//        }
//
//        return false;
//    }
//
//    @Override
//    public void handle(Update update) {
//        if (update.hasCallbackQuery()) handleCb(update);
//        else handleText(update);
//    }
//
//    private void handleCb(Update update) {
//        var cq = update.getCallbackQuery();
//        String d = cq.getData();
//        long chatId = cq.getMessage().getChatId();
//        int msgId = cq.getMessage().getMessageId();
//
//        botMessageService.answerCallback(cq.getId());
//
//        if (d.startsWith(EDIT_AMOUNT)) {
//            Long id = parseId(d, EDIT_AMOUNT);
//            if (id == null) return;
//
//            var dr = draftService.get(chatId, OpsEditDraft.class);
//            dr.opId = id;
//            draftService.save(chatId, dr);
//
//            userSessionService.setUserState(chatId, UserState.OPS_EDIT_AMOUNT);
//
//            botMessageService.editText(
//                    chatId, msgId,
//                    msg(chatId, "opsedit.askAmount"),
//                    keyboardService.listInline(chatId, List.<KeyboardService.KbItem>of(), "menu.back", "ops:open:" + id)
//            );
//            return;
//        }
//
//        if (d.startsWith(EDIT_NOTE)) {
//            Long id = parseId(d, EDIT_NOTE);
//            if (id == null) return;
//
//            var dr = draftService.get(chatId, OpsEditDraft.class);
//            dr.opId = id;
//            draftService.save(chatId, dr);
//
//            userSessionService.setUserState(chatId, UserState.OPS_EDIT_NOTE);
//
//            botMessageService.editText(
//                    chatId, msgId,
//                    msg(chatId, "opsedit.askNote"),
//                    keyboardService.listInline(chatId, List.<KeyboardService.KbItem>of(), "menu.back", "ops:open:" + id)
//            );
//        }
//    }
//
//    private void handleText(Update update) {
//        long chatId = update.getMessage().getChatId();
//        String text = update.getMessage().getText();
//
//        var st = userSessionService.getUserState(chatId);
//        var dr = draftService.get(chatId, OpsEditDraft.class);
//        Long opId = dr.opId;
//
//        if (opId == null) {
//            userSessionService.setUserState(chatId, UserState.IDLE);
//            send(chatId, msg(chatId, "opsedit.lost"), keyboardService.mainMenuInline(chatId));
//            return;
//        }
//
//        if (st == UserState.OPS_EDIT_AMOUNT) {
//            var amount = InputParseUtils.parseAmount(text);
//            if (amount == null || amount.signum() <= 0) {
//                send(chatId, msg(chatId, "opsedit.amountInvalid"),
//                        keyboardService.listInline(chatId, List.<KeyboardService.KbItem>of(), "menu.back", "ops:open:" + opId));
//                return;
//            }
//
//            boolean ok = operationService.editAmountMy24h(chatId, opId, amount);
//            finish(chatId, opId, ok);
//            return;
//        }
//
//        if (st == UserState.OPS_EDIT_NOTE) {
//            String note = text.trim();
//            if ("-".equals(note)) note = null;
//            if (note != null && note.isBlank()) note = null;
//
//            boolean ok = operationService.editNoteMy24h(chatId, opId, note);
//            finish(chatId, opId, ok);
//        }
//    }
//
//    private void finish(long chatId, long opId, boolean ok) {
//        userSessionService.setUserState(chatId, UserState.IDLE);
//        draftService.clear(chatId);
//
//        var items = List.of(new KeyboardService.KbItem(msg(chatId, "opsedit.toList"), "ops:back:list"));
//        send(chatId,
//                ok ? msg(chatId, "opsedit.saved") : msg(chatId, "opsedit.denied"),
//                keyboardService.listInline(chatId, items, "menu.back", "ops:open:" + opId));
//    }
//
//    private void send(long chatId, String text, Object kb) {
//        botMessageService.send(SendMessage.builder()
//                .chatId(String.valueOf(chatId))
//                .text(text)
//                .replyMarkup((org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard) kb)
//                .build());
//    }
//
//    private Long parseId(String data, String prefix) {
//        try { return Long.parseLong(data.substring(prefix.length())); }
//        catch (Exception e) { return null; }
//    }
//
//    private String msg(long chatId, String key, Object... args) {
//        return localizationService.getLocaleMessage(chatId, key, args);
//    }
//
//    @Override
//    public String getCommand() { return "OPS_EDIT"; }
//}
