package com.evandev.connectiblechains.util;

import com.evandev.connectiblechains.entity.Chainable;
import net.minecraft.world.level.Level;

import java.util.*;

public class ChainTracker {
    private static final Map<Level, Set<Chainable>> TRACKED_CHAINS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void register(Level level, Chainable chainable) {
        if (level == null || chainable == null) return;

        TRACKED_CHAINS.computeIfAbsent(level, _ -> Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>())))
                .add(chainable);
    }

    public static void unregister(Level level, Chainable chainable) {
        if (level == null || chainable == null) return;
        Set<Chainable> chains = TRACKED_CHAINS.get(level);
        if (chains != null) {
            chains.remove(chainable);
        }
    }

    public static List<Chainable> getChains(Level level) {
        Set<Chainable> chains = TRACKED_CHAINS.get(level);
        if (chains == null) return List.of();
        synchronized (chains) {
            return List.copyOf(chains);
        }
    }
}
