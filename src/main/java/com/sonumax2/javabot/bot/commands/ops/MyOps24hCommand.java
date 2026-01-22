//package com.sonumax2.javabot.commands;
//
//import com.sonumax2.javabot.bot.commands.Command;
//import com.sonumax2.javabot.domain.operation.Operation;
//import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
//import com.sonumax2.javabot.bot.ui.BotMessageService;
//import com.sonumax2.javabot.bot.ui.KeyboardService;
//import com.sonumax2.javabot.bot.ui.LocalizationService;
//import com.sonumax2.javabot.domain.operation.service.OperationService;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.objects.Update;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Component
//public class MyOps24hCommand implements Command {
//
//    private static final String CMD_LIST = "cmd:ops:24h";
//    private static final String OPEN = "ops:open:";
//    private static final String BACK_LIST = "ops:back:list";
//    private static final String CANCEL = "ops:cancel:";
//    private static final String CANCEL_YES = "ops:cancel:yes:";
//
//    private final BotMessageService botMessageService;
//    private final LocalizationService localizationService;
//    private final KeyboardService keyboardService;
//    private final OperationService operationService;
//    private final OperationRepository operationRepository;
//
//    public MyOps24hCommand(
//            BotMessageService botMessageService,
//            LocalizationService localizationService,
//            KeyboardService keyboardService,
//            OperationService operationService,
//            OperationRepository operationRepository
//    ) {
//        this.botMessageService = botMessageService;
//        this.localizationService = localizationService;
//        this.keyboardService = keyboardService;
//        this.operationService = operationService;
//        this.operationRepository = operationRepository;
//    }
//
//    @Override
//    public boolean canHandle(Update update) {
//        if (!update.hasCallbackQuery() || update.getCallbackQuery().getData() == null) return false;
//        String d = update.getCallbackQuery().getData();
//        return CMD_LIST.equals(d)
//                || BACK_LIST.equals(d)
//                || d.startsWith(OPEN)
//                || d.startsWith(CANCEL); // включает CANCEL_YES
//    }
//
//    @Override
//    public void handle(Update update) {
//        var cq = update.getCallbackQuery();
//        String d = cq.getData();
//        long chatId = cq.getMessage().getChatId();
//        int msgId = cq.getMessage().getMessageId();
//
//        botMessageService.answerCallback(cq.getId());
//
//        if (CMD_LIST.equals(d) || BACK_LIST.equals(d)) {
//            showList(chatId, msgId);
//            return;
//        }
//
//        if (d.startsWith(OPEN)) {
//            Long id = parseId(d, OPEN);
//            if (id == null) {
//                botMessageService.editText(chatId, msgId,
//                        msg(chatId, "ops24h.badId"),
//                        keyboardService.listInline(chatId, List.<KeyboardService.KbItem>of(), "menu.back", BACK_LIST));
//                return;
//            }
//            showDetails(chatId, msgId, id);
//            return;
//        }
//
//        if (d.startsWith(CANCEL_YES)) {
//            Long id = parseId(d, CANCEL_YES);
//            if (id == null) {
//                botMessageService.editText(chatId, msgId,
//                        msg(chatId, "ops24h.badId"),
//                        keyboardService.listInline(chatId, List.<KeyboardService.KbItem>of(), "menu.back", BACK_LIST));
//                return;
//            }
//            doCancel(chatId, msgId, id);
//            return;
//        }
//
//        if (d.startsWith(CANCEL)) {
//            Long id = parseId(d, CANCEL);
//            if (id == null) {
//                botMessageService.editText(chatId, msgId,
//                        msg(chatId, "ops24h.badId"),
//                        keyboardService.listInline(chatId, List.<KeyboardService.KbItem>of(), "menu.back", BACK_LIST));
//                return;
//            }
//            showCancelConfirm(chatId, msgId, id);
//        }
//    }
//
//    private void showList(long chatId, int msgId) {
//        var ops = operationService.myRecent24h(chatId);
//
//        String text = ops.isEmpty()
//                ? msg(chatId, "ops24h.empty")
//                : msg(chatId, "ops24h.title");
//
//        var items = ops.stream()
//                .map(o -> new KeyboardService.KbItem(
//                        "#" + o.getId() + "  " + o.getOpDate() + "  " + o.getAmount(),
//                        OPEN + o.getId()
//                ))
//                .toList();
//
//        botMessageService.editText(
//                chatId,
//                msgId,
//                text,
//                keyboardService.listInline(chatId, items, "menu.back", "cmd.menu")
//        );
//    }
//
//    private void showDetails(long chatId, int msgId, long id) {
//        var opt = operationRepository.findById(id);
//        if (opt.isEmpty() || !isMine(opt.get(), chatId)) {
//            botMessageService.editText(
//                    chatId, msgId,
//                    msg(chatId, "ops24h.notYours"),
//                    keyboardService.listInline(chatId, List.<KeyboardService.KbItem>of(), "menu.back", BACK_LIST)
//            );
//            return;
//        }
//
//        Operation o = opt.get();
//        String note = (o.getNote() == null || o.getNote().isBlank()) ? "—" : o.getNote();
//
//        String text = msg(chatId, "ops24h.details",
//                o.getId(), o.getOpDate(), o.getAmount(), note);
//
//        List<KeyboardService.KbItem> actions = new ArrayList<>();
//        if (canChange(o)) {
//            actions.add(new KeyboardService.KbItem(msg(chatId, "ops24h.btnCancel"), CANCEL + id));
//        } else {
//            text = text + "\n\n" + msg(chatId, "ops24h.cancelOnly24h");
//        }
//
//        actions.add(new KeyboardService.KbItem(msg(chatId, "ops24h.btnEditAmount"), "ops:edit:amount:" + id));
//        actions.add(new KeyboardService.KbItem(msg(chatId, "ops24h.btnEditNote"),   "ops:edit:note:" + id));
//
//
//        botMessageService.editText(
//                chatId, msgId,
//                text,
//                keyboardService.listInline(chatId, actions, "menu.back", BACK_LIST)
//        );
//    }
//
//    private void showCancelConfirm(long chatId, int msgId, long id) {
//        // confirm экран тоже защищаем: не показываем, если уже не доступно
//        var opt = operationRepository.findById(id);
//        if (opt.isEmpty() || !isMine(opt.get(), chatId) || !canChange(opt.get())) {
//            botMessageService.editText(
//                    chatId, msgId,
//                    msg(chatId, "ops24h.cancelDenied"),
//                    keyboardService.listInline(chatId, List.<KeyboardService.KbItem>of(), "menu.back", BACK_LIST)
//            );
//            return;
//        }
//
//        List<KeyboardService.KbItem> items = List.of(
//                new KeyboardService.KbItem(msg(chatId, "ops24h.btnYesCancel"), CANCEL_YES + id)
//        );
//
//        botMessageService.editText(
//                chatId, msgId,
//                msg(chatId, "ops24h.cancelConfirm", id),
//                keyboardService.listInline(chatId, items, "menu.back", OPEN + id)
//        );
//    }
//
//    private void doCancel(long chatId, int msgId, long id) {
//        boolean ok = operationService.cancelMy24h(chatId, id, null);
//
//        botMessageService.editText(
//                chatId, msgId,
//                ok ? msg(chatId, "ops24h.cancelOk", id)
//                        : msg(chatId, "ops24h.cancelDenied"),
//                keyboardService.listInline(chatId, List.<KeyboardService.KbItem>of(), "menu.back", BACK_LIST)
//        );
//    }
//
//    private boolean isMine(Operation o, long chatId) {
//        return o.getChatId() != null && o.getChatId().equals(chatId);
//    }
//
//    private boolean canChange(Operation o) {
//        if (o.isCancelled()) return false;
//        if (o.getCreatedAt() == null) return false;
//        return !o.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24));
//    }
//
//    private Long parseId(String data, String prefix) {
//        try {
//            return Long.parseLong(data.substring(prefix.length()));
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    private String msg(long chatId, String key, Object... args) {
//        return localizationService.getLocaleMessage(chatId, key, args);
//    }
//
//    @Override
//    public String getCommand() {
//        return "OPS_24H";
//    }
//}
