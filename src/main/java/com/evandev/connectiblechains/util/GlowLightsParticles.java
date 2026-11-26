package com.evandev.connectiblechains.util;

import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GlowLightsParticles {
    private static final String ITEM_PREFIX = "glow_lights_";
    private static final float CHANCE_PER_BLOCK = 0.03f;
    private static final double SPREAD = 2.1 / 16.0;

    private static final Map<Identifier, Optional<ParticleOptions>> PARTICLES = new HashMap<>();
    private static MethodHandle colorLookup;
    private static boolean colorLookupResolved;

    public static void tick(Level level, ChainKnotEntity knot, Chainable.ChainData chainData, Entity holder) {
        if (chainData.glowLights == null || !(holder instanceof ChainKnotEntity holderKnot)) return;
        Optional<ParticleOptions> particle = PARTICLES.computeIfAbsent(chainData.glowLights, GlowLightsParticles::particleFor);
        if (particle.isEmpty()) return;

        Vec3 src = knot.getChainPos(1.0f);
        Vec3 dst = holderKnot.getChainPos(1.0f);
        RandomSource random = level.getRandom();
        float expected = (float) src.distanceTo(dst) * CHANCE_PER_BLOCK;
        int count = (int) expected + (random.nextFloat() < expected % 1f ? 1 : 0);
        if (count == 0) return;

        DyeColor color = dyeColor(chainData.glowLights);
        float slack = chainData.getSlack();
        for (int i = 0; i < count; i++) {
            Vec3 pos = HangingBlockPlacement.anchor(src, dst, random.nextFloat(), slack);
            float[] rgb = color(color, random);
            level.addParticle(particle.get(),
                    pos.x() + Mth.nextDouble(random, -SPREAD, SPREAD),
                    pos.y() + Mth.nextDouble(random, -SPREAD, SPREAD),
                    pos.z() + Mth.nextDouble(random, -SPREAD, SPREAD),
                    rgb[0], rgb[1], rgb[2]);
        }
    }

    private static Optional<ParticleOptions> particleFor(Identifier glowLights) {
        Identifier id = Identifier.fromNamespaceAndPath(glowLights.getNamespace(), "glow_light");
        return BuiltInRegistries.PARTICLE_TYPE.getOptional(id)
                .filter(ParticleOptions.class::isInstance)
                .map(ParticleOptions.class::cast);
    }

    private static DyeColor dyeColor(Identifier glowLights) {
        String path = glowLights.getPath();
        return path.startsWith(ITEM_PREFIX) ? DyeColor.byName(path.substring(ITEM_PREFIX.length()), null) : null;
    }

    private static float[] color(DyeColor color, RandomSource random) {
        MethodHandle lookup = colorLookup();
        if (lookup != null) {
            try {
                return (float[]) lookup.invokeExact(color, random);
            } catch (Throwable ignored) {
            }
        }
        int rgb = color != null ? color.getTextureDiffuseColor() : Mth.hsvToRgb(random.nextFloat(), 1f, 1f);
        return new float[]{(rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f};
    }

    private static MethodHandle colorLookup() {
        if (!colorLookupResolved) {
            colorLookupResolved = true;
            try {
                Class<?> handler = Class.forName("net.mehvahdjukaar.snowyspirit.dynamicpack.ClientDynamicResourcesHandler");
                colorLookup = MethodHandles.publicLookup().findStatic(handler, "getGlowLightColor",
                        MethodType.methodType(float[].class, DyeColor.class, RandomSource.class));
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        return colorLookup;
    }
}
