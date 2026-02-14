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
        for (FlowDefinition<?> flow : defs) {
            registerUnique(byNs, flow.ns, flow, "namespace");

            for (String cmd : flow.startCommands()) {
                registerUnique(byCommand, cmd, flow, "startCommand");
            }

            for (String cb : flow.startCallbacks()) {
                registerUnique(byStartCb, cb, flow, "startCallback");
            }

        }
    }

    private void registerUnique(
            Map<String, FlowDefinition<?>> map,
            String key,
            FlowDefinition<?> flow,
            String type
    ) {
        FlowDefinition<?> prev = map.putIfAbsent(key, flow);
        if (prev != null) {
            throw new IllegalStateException(
                    "FlowRegistry conflict for " + type + " '" + key + "': "
                            + prev.getClass().getSimpleName()
                            + " vs "
                            + flow.getClass().getSimpleName()
            );
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
