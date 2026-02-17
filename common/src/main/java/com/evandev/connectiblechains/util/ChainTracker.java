package com.evandev.connectiblechains.util;

import com.evandev.connectiblechains.entity.Chainable;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class ChainTracker {
    private static final Map<Level, Set<Chainable>> TRACKED_CHAINS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void register(Level level, Chainable chainable) {
        if (level == null || chainable == null) return;

        TRACKED_CHAINS.computeIfAbsent(level, k -> Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>())))
                .add(chainable);
    }

    public static void unregister(Level level, Chainable chainable) {
        if (level == null || chainable == null) return;
        Set<Chainable> chains = TRACKED_CHAINS.get(level);
        if (chains != null) {
            chains.remove(chainable);
        }
    }

    public static Set<Chainable> getChains(Level level) {
        return TRACKED_CHAINS.getOrDefault(level, Collections.emptySet());
    }
}