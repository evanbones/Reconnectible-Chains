package com.evandev.connectiblechains.util;

import com.evandev.connectiblechains.entity.ChainCollisionEntity;
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.networking.packet.BuntingSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.platform.Services;
import com.evandev.connectiblechains.tag.ModTagRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

        float t = hit.t();
        Vec3 srcPos = chainable.getChainPos(1.0f);
        Vec3 dstPos = holderKnot.getChainPos(1.0f);
        float chainLength = (float) srcPos.distanceTo(dstPos);
        float minTSpacing = chainLength > 0 ? 0.5f / chainLength : 1.0f;

        Chainable.ChainData link = hit.chainData();
        for (Chainable.ChainData.BuntingEntry entry : link.buntings) {
            if (Math.abs(entry.t() - t) < minTSpacing) return false;
        }

        if (player.level().isClientSide) return true;

        DyeColor color = getBuntingColor(stack.getItem());
        if (color == null) return true;

        link.buntings.add(new Chainable.ChainData.BuntingEntry(t, color));
        link.buntings.sort(Comparator.comparingDouble(Chainable.ChainData.BuntingEntry::t));
        if (!player.isCreative()) stack.shrink(1);

        sendBuntingSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        return true;
    }

    /**
     * Attempts to remove a bunting from a looked-at rope chain using shears.
     * Returns false if there is no bunting near the look point, allowing slack adjustment to proceed.
     *
     * @return true if a bunting was found and removed.
     */
    public static boolean tryRemoveBunting(Player player, InteractionHand hand) {
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
        if (link.buntings.isEmpty()) return false;

        // Find the nearest bunting to the click point
        Vec3 srcPos = chainable.getChainPos(1.0f);
        Vec3 dstPos = holderKnot.getChainPos(1.0f);
        float chainLength = (float) srcPos.distanceTo(dstPos);

        Chainable.ChainData.BuntingEntry nearest = null;
        float nearestDist = Float.MAX_VALUE;
        for (Chainable.ChainData.BuntingEntry entry : link.buntings) {
            float dist = Math.abs(entry.t() - t);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entry;
            }
        }

        if (nearest == null || nearestDist * chainLength > 1.5f) return false;

        if (player.level().isClientSide) return true;

        link.buntings.remove(nearest);
        if (!player.isCreative()) {
            Item buntingItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("supplementaries", "bunting_" + nearest.color().getName()));
            if (buntingItem != Items.AIR) {
                player.getInventory().add(new ItemStack(buntingItem));
            }
        }

        sendBuntingSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        return true;
    }

    private static void sendBuntingSync(Entity chainedEntity, ChainKnotEntity holder, Chainable.ChainData link, ServerLevel level) {
        Services.NETWORK.sendToAllClients(level.getServer(), new BuntingSyncS2CPacket(chainedEntity.getId(), holder.getId(), new ArrayList<>(link.buntings)));
    }

    public static DyeColor getBuntingColor(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return null;
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
        Vec3 u = rayDir;
        Vec3 v = p1.subtract(p0);
        Vec3 w = rayOrigin.subtract(p0);

        double a = u.dot(u);
        double b = u.dot(v);
        double c = v.dot(v);
        double d = u.dot(w);
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
        Vec3 dP = w.add(u.scale(sc)).subtract(v.scale(tc));
        return new double[]{dP.length(), tc};
    }

    public record ChainHitResult(Entity chainedEntity, Chainable.ChainData chainData, double hitDistance, float t) {
    }
}
