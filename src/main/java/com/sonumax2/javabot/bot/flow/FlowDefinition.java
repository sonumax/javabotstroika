package com.sonumax2.javabot.bot.flow;

import com.sonumax2.javabot.domain.draft.DraftType;
import com.sonumax2.javabot.domain.draft.OpDraftBase;

import java.util.*;

public class FlowDefinition<D extends OpDraftBase> {

    public final String ns;
    public final DraftType draftType;
    public final Class<D> draftClass;

    public final String startStepId;

    private final Map<String, FlowStep<D>> steps = new LinkedHashMap<>();
    private final Set<String> startCallbacks = new HashSet<>();
    private final Set<String> startCommands = new HashSet<>();

    public FlowDefinition(String ns, DraftType draftType, Class<D> draftClass, String startStepId) {
        this.ns = ns;
        this.draftType = draftType;
        this.draftClass = draftClass;
        this.startStepId = startStepId;
    }

    public FlowDefinition<D> addStartCommand(String cmd) {
        if (cmd == null) return this;
        String c = cmd.trim();
        if (!c.startsWith("/")) c = "/" + c;
        startCommands.add(c);
        return this;
    }

    public boolean isStartCommand(String text) {
        if (text == null) return false;
        String t = text.trim();
        // Telegram: "/advance@MyBot" -> берем часть до пробела
        int sp = t.indexOf(' ');
        if (sp > 0) t = t.substring(0, sp);

        // отрезаем "@botname"
        int at = t.indexOf('@');
        if (at > 0) t = t.substring(0, at);

        return startCommands.contains(t);
    }

    public Set<String> startCommands() {
        return startCommands;
    }

    public FlowDefinition<D> addStep(FlowStep<D> step) {
        steps.put(step.id(), step);
        return this;
    }

    public FlowDefinition<D> addStartCallback(String cb) {
        startCallbacks.add(cb);
        return this;
    }

    public boolean isStartCallback(String data) {
        return data != null && startCallbacks.contains(data);
    }

    public Set<String> startCallbacks() {
        return startCallbacks;
    }

    public FlowStep<D> step(String id) {
        return steps.get(id);
    }

    public Collection<FlowStep<D>> allSteps() {
        return steps.values();
    }
}
