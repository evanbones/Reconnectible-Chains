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

        float t = hit.t();
        if (minTSpacing < 1.0f) {
            t = Math.round(t / minTSpacing) * minTSpacing;
            t = Math.max(0.0f, Math.min(1.0f, t));
        }

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

        float t = hit.t();
        if (minTSpacing < 1.0f) {
            t = Math.round(t / minTSpacing) * minTSpacing;
            t = Math.max(0.0f, Math.min(1.0f, t));
        }

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

        float t = hit.t();
        if (minTSpacing < 1.0f) {
            t = Math.round(t / minTSpacing) * minTSpacing;
            t = Math.max(0.0f, Math.min(1.0f, t));
        }

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

        Chainable.ChainData.HangingEntry nearestHanging = null;
        float nearestHangingDist = Float.MAX_VALUE;
        for (Chainable.ChainData.HangingEntry e : link.hangings) {
            float d = Math.abs(e.t() - t);
            if (d < nearestHangingDist) {
                nearestHangingDist = d;
                nearestHanging = e;
            }
        }

        float buntingWorldDist = nearestBunting != null ? nearestBuntingDist * distXZ : Float.MAX_VALUE;
        float bannerWorldDist = nearestBanner != null ? nearestBannerDist * distXZ : Float.MAX_VALUE;
        float hangingWorldDist = nearestHanging != null ? nearestHangingDist * distXZ : Float.MAX_VALUE;

        if (buntingWorldDist > 1.5f && bannerWorldDist > 1.5f && hangingWorldDist > 1.5f) return false;

        if (player.level().isClientSide) return true;

        float minDist = Math.min(buntingWorldDist, Math.min(bannerWorldDist, hangingWorldDist));
        if (minDist == buntingWorldDist) {
            link.buntings.remove(nearestBunting);
            if (!player.isCreative()) {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("supplementaries", "bunting_" + nearestBunting.color().getName()));
                if (item != Items.AIR) player.getInventory().add(new ItemStack(item));
            }
            sendBuntingSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        } else if (minDist == bannerWorldDist) {
            link.banners.remove(nearestBanner);
            if (!player.isCreative()) {
                ItemStack bannerStack = BannerBlock.byColor(nearestBanner.color()).asItem().getDefaultInstance();
                if (nearestBanner.data().contains("Patterns", Tag.TAG_LIST)) {
                    CompoundTag blockEntityTag = new CompoundTag();
                    blockEntityTag.put("Patterns", nearestBanner.data().getList("Patterns", Tag.TAG_COMPOUND));
                    bannerStack.addTagElement("BlockEntityTag", blockEntityTag);
                }
                player.getInventory().add(bannerStack);
            }
            sendBannerSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        } else {
            BlockPos lightPos = HangingLightHelper.computeLightPos(chainedEntity, holderKnot, nearestHanging.t(), link.getSlack());
            if (lightPos != null) HangingLightHelper.remove((ServerLevel) player.level(), lightPos);
            link.hangings.remove(nearestHanging);
            if (!player.isCreative()) {
                Item item = BuiltInRegistries.ITEM.get(nearestHanging.blockId());
                if (item != Items.AIR) player.getInventory().add(new ItemStack(item));
            }
            sendHangingSync(chainedEntity, holderKnot, link, (ServerLevel) player.level());
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8f, 0.9f + player.level().random.nextFloat() * 0.2f);
        return true;
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
