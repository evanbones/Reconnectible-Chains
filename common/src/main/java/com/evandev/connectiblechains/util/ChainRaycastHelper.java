package com.evandev.connectiblechains.util;

import com.evandev.connectiblechains.entity.ChainCollisionEntity;
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.networking.packet.BannerSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.BuntingSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.HangingSyncS2CPacket;
import com.evandev.connectiblechains.platform.Services;
import com.evandev.connectiblechains.tag.ModTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

public class ChainRaycastHelper {

    private static long lastRemovalTick = -100L;

    public static boolean tryPlaceBunting(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModTagRegistry.BUNTING_ITEMS)) return false;

        double reach = player.isCreative() ? 5.0 : 4.5;
        Optional<ChainHitResult> hitOpt = raycastChains(player, reach);
        if (hitOpt.isEmpty()) return false;

        ChainHitResult hit = hitOpt.get();
        if (!new ItemStack(hit.chainData().sourceItem).is(ModTagRegistry.BUNTING_CHAIN_SOURCES)) return false;

        Entity chainedEntity = hit.chainedEntity();
        if (!(chainedEntity instanceof Chainable chainable)) return false;
        Entity holder = chainable.getChainHolder(hit.chainData());
        if (!(holder instanceof ChainKnotEntity holderKnot)) return false;

        Vec3 srcPos = chainable.getChainPos(1.0f);
        Vec3 dstPos = holderKnot.getChainPos(1.0f);
        double dx = dstPos.x() - srcPos.x();
        double dy = dstPos.y() - srcPos.y();
        double dz = dstPos.z() - srcPos.z();
        float distance3D = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float minTSpacing = distance3D > 0 ? 0.5f / distance3D : 1.0f;

        float minT = minTSpacing;
        float maxT = 1.0f - minTSpacing;
        if (minT > maxT) return false;

        float t = hit.t();
        t = Math.round(t / minTSpacing) * minTSpacing;
        t = Math.max(minT, Math.min(maxT, t));

        Chainable.ChainData link = hit.chainData();
        if (isSlotOccupied(link, t, minTSpacing * 0.5f)) return false;

        if (player.level().isClientSide) return true;

        DyeColor color = getBuntingColor(stack);
        if (color == null) return true;

        link.buntings.add(new Chainable.ChainData.BuntingEntry(t, color));
        link.buntings.sort(Comparator.comparingDouble(Chainable.ChainData.BuntingEntry::t));
        if (!player.isCreative()) stack.shrink(1);

        sendBuntingSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0f, 0.9f + player.level().random.nextFloat() * 0.2f);
        return true;
    }

    public static boolean tryPlaceBanner(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BannerItem bannerItem)) return false;

        double reach = player.isCreative() ? 5.0 : 4.5;
        Optional<ChainHitResult> hitOpt = raycastChains(player, reach);
        if (hitOpt.isEmpty()) return false;

        ChainHitResult hit = hitOpt.get();
        if (!new ItemStack(hit.chainData().sourceItem).is(ModTagRegistry.BANNER_CHAIN_SOURCES)) return false;

        Entity chainedEntity = hit.chainedEntity();
        if (!(chainedEntity instanceof Chainable chainable)) return false;
        Entity holder = chainable.getChainHolder(hit.chainData());
        if (!(holder instanceof ChainKnotEntity holderKnot)) return false;

        Vec3 srcPos = chainable.getChainPos(1.0f);
        Vec3 dstPos = holderKnot.getChainPos(1.0f);
        double bdx = dstPos.x() - srcPos.x();
        double bdy = dstPos.y() - srcPos.y();
        double bdz = dstPos.z() - srcPos.z();
        float distance3D = (float) Math.sqrt(bdx * bdx + bdy * bdy + bdz * bdz);
        float minTSpacing = distance3D > 0 ? 1.0f / distance3D : 1.0f;

        float minT = minTSpacing;
        float maxT = 1.0f - minTSpacing;
        if (minT > maxT) return false;

        float t = hit.t();
        t = Math.round(t / minTSpacing) * minTSpacing;
        t = Math.max(minT, Math.min(maxT, t));

        Chainable.ChainData link = hit.chainData();
        if (isSlotOccupied(link, t, minTSpacing * 0.5f)) return false;

        if (player.level().isClientSide) return true;

        DyeColor color = bannerItem.getColor();
        CompoundTag data = new CompoundTag();
        data.putString("BaseColor", color.getName());

        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag != null && blockEntityTag.contains("Patterns", Tag.TAG_LIST)) {
            data.put("Patterns", blockEntityTag.getList("Patterns", Tag.TAG_COMPOUND));
        }

        link.banners.add(new Chainable.ChainData.BannerEntry(t, color, data));
        link.banners.sort(Comparator.comparingDouble(Chainable.ChainData.BannerEntry::t));
        if (!player.isCreative()) stack.shrink(1);

        sendBannerSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 0.9f + player.level().random.nextFloat() * 0.2f);
        return true;
    }

    public static boolean tryPlaceHanging(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModTagRegistry.HANGABLE_ITEMS)) return false;

        double reach = player.isCreative() ? 5.0 : 4.5;
        Optional<ChainHitResult> hitOpt = raycastChains(player, reach);
        if (hitOpt.isEmpty()) return false;

        ChainHitResult hit = hitOpt.get();
        if (!new ItemStack(hit.chainData().sourceItem).is(ModTagRegistry.HANGING_CHAIN_SOURCES)) return false;

        Entity chainedEntity = hit.chainedEntity();
        if (!(chainedEntity instanceof Chainable chainable)) return false;
        Entity holder = chainable.getChainHolder(hit.chainData());
        if (!(holder instanceof ChainKnotEntity holderKnot)) return false;

        Vec3 srcPos = chainable.getChainPos(1.0f);
        Vec3 dstPos = holderKnot.getChainPos(1.0f);
        double hdx = dstPos.x() - srcPos.x();
        double hdy = dstPos.y() - srcPos.y();
        double hdz = dstPos.z() - srcPos.z();
        float distance3D = (float) Math.sqrt(hdx * hdx + hdy * hdy + hdz * hdz);
        float minTSpacing = distance3D > 0 ? 1.0f / distance3D : 1.0f;

        float minT = minTSpacing;
        float maxT = 1.0f - minTSpacing;
        if (minT > maxT) return false;

        float t = hit.t();
        t = Math.round(t / minTSpacing) * minTSpacing;
        t = Math.max(minT, Math.min(maxT, t));

        Chainable.ChainData link = hit.chainData();
        if (isSlotOccupied(link, t, minTSpacing * 0.5f)) return false;

        if (player.level().isClientSide) return true;

        ResourceLocation blockId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        link.hangings.add(new Chainable.ChainData.HangingEntry(t, blockId));
        link.hangings.sort(Comparator.comparingDouble(Chainable.ChainData.HangingEntry::t));
        if (!player.isCreative()) stack.shrink(1);

        Block hangBlock = BuiltInRegistries.BLOCK.get(blockId);
        if (hangBlock != Blocks.AIR) {
            BlockState hangState = hangBlock.defaultBlockState();
            if (hangState.hasProperty(BlockStateProperties.HANGING))
                hangState = hangState.setValue(BlockStateProperties.HANGING, true);
            BlockPos lightPos = HangingLightHelper.computeLightPos(chainedEntity, holderKnot, t, link.getSlack());
            if (lightPos != null)
                HangingLightHelper.place((ServerLevel) player.level(), lightPos, hangState.getLightEmission());
        }

        sendHangingSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 0.9f + player.level().random.nextFloat() * 0.2f);
        return true;
    }

    public static boolean tryRemoveDecoration(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) return false;
        if (hand == InteractionHand.OFF_HAND && !player.getMainHandItem().isEmpty()) return false;
        if (player.level().isClientSide && player.level().getGameTime() - lastRemovalTick < 4) return false;

        double reach = player.isCreative() ? 5.0 : 4.5;
        Vec3 rayOrigin = player.getEyePosition(1.0f);
        Vec3 rayDir = player.getViewVector(1.0f);

        double bestDist = Double.MAX_VALUE;
        Object bestEntry = null;
        Entity bestChainedEntity = null;
        ChainKnotEntity bestHolder = null;
        Chainable.ChainData bestLink = null;

        for (Chainable chainable : ChainTracker.getChains(player.level())) {
            Entity entity = (Entity) chainable;
            if (entity.distanceTo(player) > Chainable.getMaxChainLength() + reach) continue;

            for (Chainable.ChainData cd : chainable.getChainDataSet()) {
                if (cd.buntings.isEmpty() && cd.banners.isEmpty() && cd.hangings.isEmpty()) continue;
                Entity h = chainable.getChainHolder(cd);
                if (!(h instanceof ChainKnotEntity knot)) continue;

                Vec3 srcPos = chainable.getChainPos(1.0f);
                Vec3 dstPos = knot.getChainPos(1.0f);
                double dist3D = srcPos.distanceTo(dstPos);
                if (dist3D < 0.01) continue;
                float slack = cd.getSlack();

                for (Chainable.ChainData.BuntingEntry e : cd.buntings) {
                    double d = rayPointDist(rayOrigin, rayDir, chainPoint(e.t(), srcPos, dstPos, dist3D, slack), reach);
                    if (d < 0.5 && d < bestDist) { bestDist = d; bestEntry = e; bestChainedEntity = entity; bestHolder = knot; bestLink = cd; }
                }
                for (Chainable.ChainData.BannerEntry e : cd.banners) {
                    double d = rayPointDist(rayOrigin, rayDir, chainPoint(e.t(), srcPos, dstPos, dist3D, slack), reach);
                    if (d < 0.5 && d < bestDist) { bestDist = d; bestEntry = e; bestChainedEntity = entity; bestHolder = knot; bestLink = cd; }
                }
                for (Chainable.ChainData.HangingEntry e : cd.hangings) {
                    Vec3 p = chainPoint(e.t(), srcPos, dstPos, dist3D, slack);
                    double d = rayPointDist(rayOrigin, rayDir, new Vec3(p.x(), p.y() - 0.5, p.z()), reach);
                    if (d < 0.7 && d < bestDist) { bestDist = d; bestEntry = e; bestChainedEntity = entity; bestHolder = knot; bestLink = cd; }
                }
            }
        }

        if (bestEntry == null) return false;

        if (player.level().isClientSide) {
            lastRemovalTick = player.level().getGameTime();
            return true;
        }

        if (bestEntry instanceof Chainable.ChainData.BuntingEntry buntingEntry) {
            bestLink.buntings.remove(buntingEntry);
            if (!player.isCreative()) {
                ResourceLocation coloredId = new ResourceLocation("supplementaries", "bunting_" + buntingEntry.color().getName());
                Item buntingItem = BuiltInRegistries.ITEM.get(coloredId);

                if (buntingItem != Items.AIR) {
                    player.getInventory().add(new ItemStack(buntingItem));
                } else {
                    Item singleBuntingItem = BuiltInRegistries.ITEM.get(new ResourceLocation("supplementaries", "bunting"));
                    if (singleBuntingItem != Items.AIR) {
                        ItemStack buntingStack = new ItemStack(singleBuntingItem);
                        buntingStack.getOrCreateTag().putString("Color", buntingEntry.color().getName());
                        player.getInventory().add(buntingStack);
                    }
                }
            }
            sendBuntingSync(bestChainedEntity, bestHolder, bestLink, (ServerLevel) player.level());
        } else if (bestEntry instanceof Chainable.ChainData.BannerEntry bannerEntry) {
            bestLink.banners.remove(bannerEntry);
            if (!player.isCreative()) {
                ItemStack bannerStack = BannerBlock.byColor(bannerEntry.color()).asItem().getDefaultInstance();
                if (bannerEntry.data().contains("Patterns", Tag.TAG_LIST)) {
                    CompoundTag blockEntityTag = new CompoundTag();
                    blockEntityTag.put("Patterns", bannerEntry.data().getList("Patterns", Tag.TAG_COMPOUND));
                    bannerStack.addTagElement("BlockEntityTag", blockEntityTag);
                }
                player.getInventory().add(bannerStack);
            }
            sendBannerSync(bestChainedEntity, bestHolder, bestLink, (ServerLevel) player.level());
        } else if (bestEntry instanceof Chainable.ChainData.HangingEntry hangingEntry) {
            BlockPos lightPos = HangingLightHelper.computeLightPos(bestChainedEntity, bestHolder, hangingEntry.t(), bestLink.getSlack());
            if (lightPos != null) HangingLightHelper.remove((ServerLevel) player.level(), lightPos);
            bestLink.hangings.remove(hangingEntry);
            if (!player.isCreative()) {
                Item item = BuiltInRegistries.ITEM.get(hangingEntry.blockId());
                if (item != Items.AIR) player.getInventory().add(new ItemStack(item));
            }
            sendHangingSync(bestChainedEntity, bestHolder, bestLink, (ServerLevel) player.level());
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 0.9f + player.level().random.nextFloat() * 0.3f);
        return true;
    }

    private static Vec3 chainPoint(float t, Vec3 src, Vec3 dst, double dist3D, float slack) {
        return new Vec3(
                Mth.lerp(t, src.x(), dst.x()),
                src.y() + MathHelper.drip2(t * dist3D, dist3D, dst.y() - src.y(), slack) - 0.125,
                Mth.lerp(t, src.z(), dst.z())
        );
    }

    private static double rayPointDist(Vec3 origin, Vec3 dir, Vec3 point, double maxDist) {
        double sc = point.subtract(origin).dot(dir);
        if (sc < 0 || sc > maxDist) return Double.MAX_VALUE;
        return origin.add(dir.scale(sc)).distanceTo(point);
    }

    private static boolean isSlotOccupied(Chainable.ChainData link, float t, float halfSpacing) {
        for (Chainable.ChainData.BuntingEntry e : link.buntings) if (Math.abs(e.t() - t) < halfSpacing) return true;
        for (Chainable.ChainData.BannerEntry e : link.banners) if (Math.abs(e.t() - t) < halfSpacing) return true;
        for (Chainable.ChainData.HangingEntry e : link.hangings) if (Math.abs(e.t() - t) < halfSpacing) return true;
        return false;
    }

    private static void sendBuntingSync(Entity chainedEntity, ChainKnotEntity holder, Chainable.ChainData link, ServerLevel level) {
        Services.NETWORK.sendToAllClients(level.getServer(), new BuntingSyncS2CPacket(chainedEntity.getId(), holder.getId(), new ArrayList<>(link.buntings)));
    }

    private static void sendBannerSync(Entity chainedEntity, ChainKnotEntity holder, Chainable.ChainData link, ServerLevel level) {
        Services.NETWORK.sendToAllClients(level.getServer(), new BannerSyncS2CPacket(chainedEntity.getId(), holder.getId(), new ArrayList<>(link.banners)));
    }

    private static void sendHangingSync(Entity chainedEntity, ChainKnotEntity holder, Chainable.ChainData link, ServerLevel level) {
        Services.NETWORK.sendToAllClients(level.getServer(), new HangingSyncS2CPacket(chainedEntity.getId(), holder.getId(), new ArrayList<>(link.hangings)));
    }

    public static DyeColor getBuntingColor(ItemStack stack) {
        // Per-color items (e.g. supplementaries:bunting_white in 1.21.1)
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        if (path.startsWith("bunting_")) {
            DyeColor fromName = DyeColor.byName(path.substring("bunting_".length()), null);
            if (fromName != null) return fromName;
        }
        // Single item with color in NBT (supplementaries:bunting in 1.20.1 — "Color" string tag)
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            String colorName = tag.getString("Color");
            if (!colorName.isEmpty()) {
                DyeColor fromNbt = DyeColor.byName(colorName, null);
                if (fromNbt != null) return fromNbt;
            }
        }
        return DyeColor.WHITE;
    }

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

            ServerLevel serverWorld = (ServerLevel) player.level();
            Entity holder = chainable.getChainHolder(link);

            HangingLightHelper.removeAllForChain(serverWorld, chainedEntity, holder, link);
            link.customSlack = 1.0f / currentSag;
            HangingLightHelper.placeAllForChain(serverWorld, chainedEntity, holder, link);

            ChainCollisionEntity.destroyCollision(serverWorld, link);
            ChainCollisionEntity.createCollision((Entity & Chainable) chainedEntity, link);

            if (holder != null) {
                Services.NETWORK.sendToAllClients(serverWorld.getServer(), new ChainSlackSyncS2CPacket(chainedEntity.getId(), holder.getId(), link.customSlack));
            }

            if (!player.isCreative()) {
                EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(slot));
            }
        }
        return true;
    }

    /**
     * Casts a ray from the player's camera to find the closest chain curve.
     */
    public static Optional<ChainHitResult> raycastChains(Player player, double reach) {
        return raycastChains(player, reach, 0.25);
    }

    /**
     * Casts a ray from the player's camera to find the closest chain curve, with a configurable hit tolerance.
     */
    public static Optional<ChainHitResult> raycastChains(Player player, double reach, double tolerance) {
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

                    if (distToRay < tolerance) {
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
                stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
        }
        return true;
    }

    private static double distanceRaySegment(Vec3 rayOrigin, Vec3 rayDir, Vec3 p0, Vec3 p1, double maxReach) {
        return distanceRaySegmentEx(rayOrigin, rayDir, p0, p1, maxReach)[0];
    }

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
