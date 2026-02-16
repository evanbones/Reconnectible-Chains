package com.evandev.connectiblechains.util;

import com.evandev.connectiblechains.entity.ChainCollisionEntity;
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.platform.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class ChainRaycastHelper {

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
            float sagStep = 0.1f;

            if (player.isShiftKeyDown()) {
                currentSag = Math.max(0.02f, currentSag - sagStep);
            } else {
                currentSag = Math.min(2.0f, currentSag + sagStep);
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

        AABB searchBox = new AABB(rayOrigin, rayOrigin).inflate(reach);
        double closestDistance = reach;
        ChainHitResult bestHit = null;

        for (Entity entity : player.level().getEntitiesOfClass(Entity.class, searchBox)) {
            if (!(entity instanceof Chainable chainable)) continue;

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
                int segments = Math.max(8, (int) (distance * 4));

                Vec3 lastPoint = srcPos.add(0, -0.125, 0);

                for (int i = 1; i <= segments; i++) {
                    double t = (double) i / segments;
                    double x = Mth.lerp(t, srcPos.x(), dstPos.x());
                    double y = srcPos.y() + MathHelper.drip2((t * distance), distance, dstPos.y() - srcPos.y(), chainData.getSlack()) - 0.125;
                    double z = Mth.lerp(t, srcPos.z(), dstPos.z());
                    Vec3 currentPoint = new Vec3(x, y, z);

                    double distToRay = distanceRaySegment(rayOrigin, rayDir, lastPoint, currentPoint, reach);

                    if (distToRay < 0.25) {
                        double distanceToPlayer = rayOrigin.distanceTo(currentPoint);
                        if (distanceToPlayer < closestDistance) {
                            closestDistance = distanceToPlayer;
                            bestHit = new ChainHitResult(entity, chainData, distanceToPlayer);
                        }
                    }
                    lastPoint = currentPoint;
                }
            }
        }
        return Optional.ofNullable(bestHit);
    }

    /**
     * Calculates the shortest distance between a Ray and a 3D Line Segment.
     */
    private static double distanceRaySegment(Vec3 rayOrigin, Vec3 rayDir, Vec3 p0, Vec3 p1, double maxReach) {
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

        if (sc < 0.0 || sc > maxReach) return Double.MAX_VALUE;
        Vec3 dP = w.add(u.scale(sc)).subtract(v.scale(tc));
        return dP.length();
    }

    public record ChainHitResult(Entity chainedEntity, Chainable.ChainData chainData, double hitDistance) {
    }
}