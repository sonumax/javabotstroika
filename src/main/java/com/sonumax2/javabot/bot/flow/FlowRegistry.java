package com.sonumax2.javabot.bot.flow;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FlowRegistry {

    private final Map<String, FlowDefinition<?>> byNs = new HashMap<>();
    private final Map<String, FlowDefinition<?>> byCommand = new HashMap<>();
    private final Map<String, FlowDefinition<?>> byStartCb = new HashMap<>();

    public FlowRegistry(List<FlowDefinition<?>> defs) {
        for (FlowDefinition<?> d : defs) {
            byNs.put(d.ns, d);

            for (String cb : d.startCallbacks()) {
                byStartCb.put(cb, d);
            }

            for (String cmd : d.startCommands()) {
                byCommand.put(cmd, d);
            }
        }
    }

    public FlowDefinition<?> getByStartCallback(String cb) {
        return byStartCb.get(cb);
    }


    public FlowDefinition<?> getByCommand(String cmdText) {
        if (cmdText == null) return null;

        String t = cmdText.trim();
        int sp = t.indexOf(' ');
        if (sp > 0) t = t.substring(0, sp);

        int at = t.indexOf('@');
        if (at > 0) t = t.substring(0, at);

        return byCommand.get(t);
    }


    public FlowDefinition<?> get(String ns) {
        return byNs.get(ns);
    }
}
