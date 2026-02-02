package com.sonumax2.javabot.bot.flow;

import com.sonumax2.javabot.bot.ui.BotUi;
import com.sonumax2.javabot.bot.ui.KeyboardService;
import com.sonumax2.javabot.domain.draft.DraftType;
import com.sonumax2.javabot.domain.draft.OpDraftBase;
import com.sonumax2.javabot.domain.draft.service.DraftService;
import com.sonumax2.javabot.domain.session.service.UserSessionService;

public class FlowContext<D extends OpDraftBase> {

    public final long chatId;

    public final FlowServices services;

    public final DraftType draftType;
    public final Class<D> draftClass;

    public final FlowDefinition<D> def;

    public final D d;

    public FlowContext(
            long chatId,
            FlowServices services,
            DraftType draftType,
            Class<D> draftClass,
            FlowDefinition<D> def,
            D d
    ) {
        this.chatId = chatId;
        this.services = services;
        this.draftType = draftType;
        this.draftClass = draftClass;
        this.def = def;
        this.d = d;
    }

    public BotUi ui() { return services.ui(); }
    public KeyboardService keyboards() { return services.keyboards(); }
    public UserSessionService session() { return services.session(); }
    public DraftService drafts() { return services.drafts(); }
}
