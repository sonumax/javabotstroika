package com.sonumax2.javabot.bot.commands.ops;

import com.sonumax2.javabot.bot.commands.Command;
import com.sonumax2.javabot.bot.commands.CommandName;
import com.sonumax2.javabot.bot.commands.cb.AdvanceCb;
import com.sonumax2.javabot.bot.flow.FlowDefinition;
import com.sonumax2.javabot.bot.flow.FlowEngine;
import com.sonumax2.javabot.domain.draft.AdvanceDraft;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Order(50)
@Component
public class AdvanceCommand implements Command {

    private final FlowEngine flowEngine;
    private final FlowDefinition<AdvanceDraft> advanceFlow;

    public AdvanceCommand(FlowEngine flowEngine, FlowDefinition<AdvanceDraft> advanceFlow) {
        this.flowEngine = flowEngine;
        this.advanceFlow = advanceFlow;
    }

    @Override
    public boolean canHandle(Update update) {
        if (!update.hasCallbackQuery()) return false;
        String data = update.getCallbackQuery().getData();
        return AdvanceCb.isStartPick(data);
    }

    @Override
    public void handle(Update update) {
        flowEngine.handle(update, advanceFlow);
    }

    @Override
    public String getCommand() {
        return CommandName.ADVANCE.getName();
    }
}
