package com.sonumax2.javabot.bot.flow;

import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.domain.draft.service.DraftService;
import com.sonumax2.javabot.domain.session.service.UserSessionService;

public record FlowServices(
        BotUi ui,
        KeyboardService keyboards,
        UserSessionService session,
        DraftService drafts
) {}
