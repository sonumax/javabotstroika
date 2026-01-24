package com.sonumax2.javabot.bot.flow;

import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.domain.draft.DraftType;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.domain.draft.service.DraftService;
import com.sonumax2.javabot.domain.session.service.UserSessionService;

public class FlowContext<D extends OpDraftBase> {

    public final long chatId;

    public final BotUi ui;
    public final KeyboardService keyboard;
    public final UserSessionService session;
    public final DraftService drafts;

    public final DraftType draftType;
    public final Class<D> draftClass;

    public final FlowDefinition<D> def;

    public final D d;

    public FlowContext(
            long chatId,
            BotUi ui,
            KeyboardService keyboard,
            UserSessionService session,
            DraftService drafts,
            DraftType draftType,
            Class<D> draftClass,
            FlowDefinition<D> def,
            D d
    ) {
        this.chatId = chatId;
        this.ui = ui;
        this.keyboard = keyboard;
        this.session = session;
        this.drafts = drafts;
        this.draftType = draftType;
        this.draftClass = draftClass;
        this.def = def;
        this.d = d;
    }
}
