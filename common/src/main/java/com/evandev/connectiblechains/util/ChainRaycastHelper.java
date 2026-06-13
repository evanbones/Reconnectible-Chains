package com.evandev.connectiblechains.util;

import com.evandev.connectiblechains.entity.ChainCollisionEntity;
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.networking.packet.BannerSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.BuntingSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.platform.Services;
import com.evandev.connectiblechains.tag.ModTagRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

public class ChainRaycastHelper {

    /**
     * Attempts to place a bunting on a looked-at rope chain.
     *
     * @return true if a rope chain was found and interacted with.
     */
    public static boolean tryPlaceBunting(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModTagRegistry.BUNTING_ITEMS)) return false;

        double reach = player.isCreative() ? 5.0 : 4.5;
        Optional<ChainHitResult> hitOpt = raycastChains(player, reach);
        if (hitOpt.isEmpty()) return false;

        ChainHitResult hit = hitOpt.get();
        if (!new ItemStack(hit.chainData().sourceItem).is(ModTagRegistry.ROPES)) return false;

        Entity chainedEntity = hit.chainedEntity();
        if (!(chainedEntity instanceof Chainable chainable)) return false;
        Entity holder = chainable.getChainHolder(hit.chainData());
        if (!(holder instanceof ChainKnotEntity holderKnot)) return false;

        Vec3 srcPos = chainable.getChainPos(1.0f);
        Vec3 dstPos = holderKnot.getChainPos(1.0f);
        double dx = dstPos.x() - srcPos.x();
        double dz = dstPos.z() - srcPos.z();
        float distanceXZ = (float) Math.sqrt(dx * dx + dz * dz);
        float minTSpacing = distanceXZ > 0 ? 0.5f / distanceXZ : 1.0f;

        float t = hit.t();
        if (minTSpacing < 1.0f) {
            t = Math.round(t / minTSpacing) * minTSpacing;
            t = Math.max(0.0f, Math.min(1.0f, t));
        }

        Chainable.ChainData link = hit.chainData();
        for (Chainable.ChainData.BuntingEntry entry : link.buntings) {
            if (Math.abs(entry.t() - t) < minTSpacing * 0.5f) return false;
        }
        for (Chainable.ChainData.BannerEntry entry : link.banners) {
            if (Math.abs(entry.t() - t) < minTSpacing * 0.5f) return false;
        }

        if (player.level().isClientSide) return true;

        DyeColor color = getBuntingColor(stack.getItem());
        if (color == null) return true;

        link.buntings.add(new Chainable.ChainData.BuntingEntry(t, color));
        link.buntings.sort(Comparator.comparingDouble(Chainable.ChainData.BuntingEntry::t));
        if (!player.isCreative()) stack.shrink(1);

        sendBuntingSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0f, 0.9f + player.level().random.nextFloat() * 0.2f);
        return true;
    }

    /**
     * Attempts to remove the nearest decoration (bunting or banner) from a looked-at rope chain.
     * Whichever is closest to the look point wins. Falls through if nothing is within 1.5 blocks.
     */
    public static boolean tryRemoveDecoration(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.SHEARS)) return false;

        double reach = player.isCreative() ? 5.0 : 4.5;
        Optional<ChainHitResult> hitOpt = raycastChains(player, reach);
        if (hitOpt.isEmpty()) return false;

        ChainHitResult hit = hitOpt.get();
        if (!new ItemStack(hit.chainData().sourceItem).is(ModTagRegistry.ROPES)) return false;

        Entity chainedEntity = hit.chainedEntity();
        if (!(chainedEntity instanceof Chainable chainable)) return false;
        Entity holder = chainable.getChainHolder(hit.chainData());
        if (!(holder instanceof ChainKnotEntity holderKnot)) return false;

        float t = hit.t();
        Chainable.ChainData link = hit.chainData();

        Vec3 srcPos = chainable.getChainPos(1.0f);
        Vec3 dstPos = holderKnot.getChainPos(1.0f);
        double rdx = dstPos.x() - srcPos.x();
        double rdz = dstPos.z() - srcPos.z();
        float distXZ = (float) Math.sqrt(rdx * rdx + rdz * rdz);

        Chainable.ChainData.BuntingEntry nearestBunting = null;
        float nearestBuntingDist = Float.MAX_VALUE;
        for (Chainable.ChainData.BuntingEntry e : link.buntings) {
            float d = Math.abs(e.t() - t);
            if (d < nearestBuntingDist) {
                nearestBuntingDist = d;
                nearestBunting = e;
            }
        }

        Chainable.ChainData.BannerEntry nearestBanner = null;
        float nearestBannerDist = Float.MAX_VALUE;
        for (Chainable.ChainData.BannerEntry e : link.banners) {
            float d = Math.abs(e.t() - t);
            if (d < nearestBannerDist) {
                nearestBannerDist = d;
                nearestBanner = e;
            }
        }

        float buntingWorldDist = nearestBunting != null ? nearestBuntingDist * distXZ : Float.MAX_VALUE;
        float bannerWorldDist = nearestBanner != null ? nearestBannerDist * distXZ : Float.MAX_VALUE;

        if (buntingWorldDist > 1.5f && bannerWorldDist > 1.5f) return false;

        if (player.level().isClientSide) return true;

        if (buntingWorldDist <= bannerWorldDist) {
            link.buntings.remove(nearestBunting);
            if (!player.isCreative()) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("supplementaries", "bunting_" + nearestBunting.color().getName()));
                if (item != Items.AIR) player.getInventory().add(new ItemStack(item));
            }
            sendBuntingSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        } else {
            link.banners.remove(nearestBanner);
            if (!player.isCreative()) {
                ItemStack bannerStack = BannerBlock.byColor(nearestBanner.color()).asItem().getDefaultInstance();
                if (nearestBanner.data().contains("Pattern")) {
                    var ctx = player.level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
                    BannerPatternLayers.CODEC.parse(ctx, nearestBanner.data().get("Pattern"))
                            .result().ifPresent(p -> bannerStack.set(DataComponents.BANNER_PATTERNS, p));
                }
                player.getInventory().add(bannerStack);
            }
            sendBannerSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8f, 0.9f + player.level().random.nextFloat() * 0.2f);
        return true;
    }

    private static void sendBuntingSync(Entity chainedEntity, ChainKnotEntity holder, Chainable.ChainData link, ServerLevel level) {
        Services.NETWORK.sendToAllClients(level.getServer(), new BuntingSyncS2CPacket(chainedEntity.getId(), holder.getId(), new ArrayList<>(link.buntings)));
    }

    public static boolean tryPlaceBanner(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BannerItem bannerItem)) return false;

        double reach = player.isCreative() ? 5.0 : 4.5;
        Optional<ChainHitResult> hitOpt = raycastChains(player, reach);
        if (hitOpt.isEmpty()) return false;

        ChainHitResult hit = hitOpt.get();
        if (!new ItemStack(hit.chainData().sourceItem).is(ModTagRegistry.ROPES)) return false;

        Entity chainedEntity = hit.chainedEntity();
        if (!(chainedEntity instanceof Chainable chainable)) return false;
        Entity holder = chainable.getChainHolder(hit.chainData());
        if (!(holder instanceof ChainKnotEntity holderKnot)) return false;

        Vec3 srcPos = chainable.getChainPos(1.0f);
        Vec3 dstPos = holderKnot.getChainPos(1.0f);
        double bdx = dstPos.x() - srcPos.x();
        double bdz = dstPos.z() - srcPos.z();
        float distanceXZ = (float) Math.sqrt(bdx * bdx + bdz * bdz);
        float minTSpacing = distanceXZ > 0 ? 1.0f / distanceXZ : 1.0f;

        float t = hit.t();
        if (minTSpacing < 1.0f) {
            t = Math.round(t / minTSpacing) * minTSpacing;
            t = Math.max(0.0f, Math.min(1.0f, t));
        }

        Chainable.ChainData link = hit.chainData();
        for (Chainable.ChainData.BannerEntry entry : link.banners) {
            if (Math.abs(entry.t() - t) < minTSpacing * 0.5f) return false;
        }
        for (Chainable.ChainData.BuntingEntry entry : link.buntings) {
            if (Math.abs(entry.t() - t) < minTSpacing * 0.5f) return false;
        }

        if (player.level().isClientSide) return true;

        DyeColor color = bannerItem.getColor();
        CompoundTag data = new CompoundTag();
        data.putString("BaseColor", color.getName());

        BannerPatternLayers patterns = stack.get(DataComponents.BANNER_PATTERNS);
        if (patterns != null && !patterns.equals(BannerPatternLayers.EMPTY)) {
            var ctx = player.level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            BannerPatternLayers.CODEC.encodeStart(ctx, patterns)
                    .result().ifPresent(tag -> data.put("Pattern", tag));
        }

        link.banners.add(new Chainable.ChainData.BannerEntry(t, color, data));
        link.banners.sort(Comparator.comparingDouble(Chainable.ChainData.BannerEntry::t));
        if (!player.isCreative()) stack.shrink(1);

        sendBannerSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 0.9f + player.level().random.nextFloat() * 0.2f);
        return true;
    }

    private static void sendBannerSync(Entity chainedEntity, ChainKnotEntity holder, Chainable.ChainData link, ServerLevel level) {
        Services.NETWORK.sendToAllClients(level.getServer(), new BannerSyncS2CPacket(chainedEntity.getId(), holder.getId(), new ArrayList<>(link.banners)));
    }

    public static DyeColor getBuntingColor(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        if (!path.startsWith("bunting_")) return null;
        return DyeColor.byName(path.substring("bunting_".length()), null);
    }

    /**
     * Attempts to adjust the slack of a looked-at chain.
     *
     * @return true if a chain was clicked and interacted with, false otherwise.
     */
    public static boolean tryAdjustSlack(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.SHEARS)) return false;

        double reachDistance = player.isCreative() ? 5.0 : 4.5;

        Optional<ChainHitResult> hitOpt = raycastChains(player, reachDistance);
        if (hitOpt.isEmpty()) return false;

        ChainHitResult hit = hitOpt.get();

        if (player.level().isClientSide) return true;

        Chainable.ChainData link = hit.chainData();
        Entity chainedEntity = hit.chainedEntity();

        if (chainedEntity instanceof Chainable chainable) {
            float currentSlack = link.getSlack();
            float currentSag = 1.0f / currentSlack;
            float oldSag = currentSag;

            float sagStep = 0.05f;
            float minSag = 0.05f;
            float maxSag = 1.40f;

            if (player.isShiftKeyDown()) {
                currentSag = Math.max(minSag, currentSag - sagStep);
            } else {
                currentSag = Math.min(maxSag, currentSag + sagStep);
            }

            if (Math.abs(currentSag - oldSag) < 0.001f) {
                return true;
            }

            link.customSlack = 1.0f / currentSag;
            ServerLevel serverWorld = (ServerLevel) player.level();

            ChainCollisionEntity.destroyCollision(serverWorld, link);
            ChainCollisionEntity.createCollision((Entity & Chainable) chainedEntity, link);

            Entity holder = chainable.getChainHolder(link);
            if (holder != null) {
                Services.NETWORK.sendToAllClients(serverWorld.getServer(), new ChainSlackSyncS2CPacket(chainedEntity.getId(), holder.getId(), link.customSlack));
            }

            if (!player.isCreative()) {
                EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.hurtAndBreak(1, player, slot);
            }
        }
        return true;
    }

    /**
     * Casts a ray from the player's camera to find the closest chain curve.
     */
    public static Optional<ChainHitResult> raycastChains(Player player, double reach) {
        Vec3 rayOrigin = player.getEyePosition(1.0f);
        Vec3 rayDir = player.getViewVector(1.0f);

        double closestDistance = reach;
        ChainHitResult bestHit = null;

        for (Chainable chainable : ChainTracker.getChains(player.level())) {
            Entity entity = (Entity) chainable;

            if (entity.distanceTo(player) > Chainable.getMaxChainLength() + reach) {
                continue;
            }

            for (Chainable.ChainData chainData : chainable.getChainDataSet()) {
                Entity chainHolder = chainable.getChainHolder(chainData);
                if (chainHolder == null) continue;

                Vec3 srcPos = chainable.getChainPos(1.0f);
                Vec3 dstPos;
                if (chainHolder instanceof ChainKnotEntity knot) {
                    dstPos = knot.getChainPos(1.0f);
                } else {
                    dstPos = chainHolder.getLeashOffset(1.0f).add(chainHolder.position());
                }

                double distance = srcPos.distanceTo(dstPos);
                double maxDroop = Math.abs(MathHelper.drip2(distance / 2.0, distance, dstPos.y() - srcPos.y(), chainData.getSlack()) - (dstPos.y() - srcPos.y()) / 2.0);

                double distToStraightLine = distanceRaySegment(rayOrigin, rayDir, srcPos, dstPos, reach);
                if (distToStraightLine > maxDroop + 0.5) {
                    continue;
                }

                int segments = Math.max(8, Math.min(64, (int) (distance * 1.5)));
                Vec3 lastPoint = srcPos.add(0, -0.125, 0);

                for (int i = 1; i <= segments; i++) {
                    double tSeg = (double) i / segments;
                    double x = Mth.lerp(tSeg, srcPos.x(), dstPos.x());
                    double y = srcPos.y() + MathHelper.drip2((tSeg * distance), distance, dstPos.y() - srcPos.y(), chainData.getSlack()) - 0.125;
                    double z = Mth.lerp(tSeg, srcPos.z(), dstPos.z());
                    Vec3 currentPoint = new Vec3(x, y, z);

                    double[] rayResult = distanceRaySegmentEx(rayOrigin, rayDir, lastPoint, currentPoint, reach);
                    double distToRay = rayResult[0];
                    double tc = rayResult[1];

                    if (distToRay < 0.25) {
                        double distanceToPlayer = rayOrigin.distanceTo(currentPoint);
                        if (distanceToPlayer < closestDistance) {
                            closestDistance = distanceToPlayer;
                            float hitT = (float) ((i - 1 + tc) / segments);
                            bestHit = new ChainHitResult(entity, chainData, distanceToPlayer, hitT);
                        }
                    }
                    lastPoint = currentPoint;
                }
            }
        }
        return Optional.ofNullable(bestHit);
    }

    /**
     * Attempts to break a looked-at chain.
     */
    public static boolean tryBreakChain(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(Items.SHEARS)) return false;

        double reachDistance = player.isCreative() ? 5.0 : 4.5;

        Optional<ChainHitResult> hitOpt = raycastChains(player, reachDistance);
        if (hitOpt.isEmpty()) return false;

        if (player.level().isClientSide) return true;

        ChainHitResult hit = hitOpt.get();
        Chainable.ChainData link = hit.chainData();
        Entity chainedEntity = hit.chainedEntity();

        if (chainedEntity instanceof Chainable chainable) {
            chainable.detachChain(link);

            if (!player.isCreative()) {
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
        }
        return true;
    }

    /**
     * Calculates the shortest distance between a Ray and a 3D Line Segment.
     */
    private static double distanceRaySegment(Vec3 rayOrigin, Vec3 rayDir, Vec3 p0, Vec3 p1, double maxReach) {
        return distanceRaySegmentEx(rayOrigin, rayDir, p0, p1, maxReach)[0];
    }

    /**
     * Returns [distance, tc] where tc is the segment parameter [0, 1] at the closest point.
     */
    private static double[] distanceRaySegmentEx(Vec3 rayOrigin, Vec3 rayDir, Vec3 p0, Vec3 p1, double maxReach) {
        Vec3 v = p1.subtract(p0);
        Vec3 w = rayOrigin.subtract(p0);

        double a = rayDir.dot(rayDir);
        double b = rayDir.dot(v);
        double c = v.dot(v);
        double d = rayDir.dot(w);
        double e = v.dot(w);

        double D = a * c - b * b;
        double sc, tc;

        if (D < 1e-8) {
            sc = 0.0;
            tc = (b > c ? d / b : e / c);
        } else {
            sc = (b * e - c * d) / D;
            tc = (a * e - b * d) / D;
        }

        if (tc < 0.0) {
            tc = 0.0;
            sc = -d / a;
        } else if (tc > 1.0) {
            tc = 1.0;
            sc = (b - d) / a;
        }

        if (sc < 0.0 || sc > maxReach) return new double[]{Double.MAX_VALUE, tc};
        Vec3 dP = w.add(rayDir.scale(sc)).subtract(v.scale(tc));
        return new double[]{dP.length(), tc};
    }

    public record ChainHitResult(Entity chainedEntity, Chainable.ChainData chainData, double hitDistance, float t) {
    }
}
