package com.evandev.connectiblechains.item;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.evandev.connectiblechains.tag.ModTagRegistry;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.function.Predicate;

public class ChainItemCallbacks {

    public static InteractionResult chainUseEvent(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (player == null) return InteractionResult.PASS;

        if (ChainRaycastHelper.tryAdjustSlack(player, hand)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState blockState = level.getBlockState(blockPos);

        if (blockState.is(ModTagRegistry.CHAIN_CONNECTIBLE)) {
            ChainKnotEntity existingKnot = ChainKnotEntity.getOrNull(level, blockPos);

            if (existingKnot == null && hasAnyLeadsToConnect(level, blockPos, player)) {
                return InteractionResult.PASS;
            }

            Direction knotDir = determineKnotDirection(blockState, hitResult.getDirection());

            if (stack.is(ModTagRegistry.CATENARY_ITEMS)) {
                if (player.isShiftKeyDown() && existingKnot == null) {
                    return InteractionResult.PASS;
                }

                if (existingKnot != null && existingKnot.getSourceItem() != stack.getItem()) {
                    return InteractionResult.FAIL;
                }

                if (level instanceof ServerLevel serverWorld) {
                    ChainKnotEntity knot = existingKnot != null ? existingKnot : ChainKnotEntity.getOrCreate(serverWorld, blockPos, stack.getItem(), knotDir);
                    return knot.interact(player, hand);
                }
                return InteractionResult.SUCCESS;
            }

            List<Chainable> draggedChains = collectChainablesAround(level, blockPos, entity -> entity.getChainData(player) != null);

            if (!draggedChains.isEmpty()) {
                if (existingKnot != null) {
                    boolean hasMatch = draggedChains.stream().anyMatch(c -> c.getSourceItem() == existingKnot.getSourceItem());
                    if (!hasMatch) {
                        return InteractionResult.FAIL;
                    }
                }

                if (level instanceof ServerLevel serverWorld) {
                    return attachHeldChainsToBlock(player, serverWorld, blockPos, knotDir);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private static Direction determineKnotDirection(BlockState state, Direction hitFace) {
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
            if (axis == Direction.Axis.Y) {
                return Direction.UP;
            }
            if (hitFace.getAxis() == axis) {
                return hitFace;
            }
            return Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        }

        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }

        return Direction.UP;
    }

    public static InteractionResult attachHeldChainsToBlock(Player player, ServerLevel level, BlockPos pos, Direction face) {
        List<Chainable> list = collectChainablesAround(level, pos, entity -> entity.getChainData(player) != null);

        ChainKnotEntity existingKnot = ChainKnotEntity.getOrNull(level, pos);
        boolean attachedAny = false;

        Item targetItem = existingKnot != null ? existingKnot.getSourceItem() : (list.isEmpty() ? null : list.getFirst().getSourceItem());

        if (targetItem == null) return InteractionResult.PASS;

        ChainKnotEntity chainKnotEntity = existingKnot;

        for (Chainable chainable : list) {
            if (chainable.getSourceItem() != targetItem) continue;

            if (chainKnotEntity == null) {
                chainKnotEntity = ChainKnotEntity.getOrCreate(level, pos, targetItem, face);
                chainKnotEntity.playPlacementSound();
            }

            if (chainable.canAttachTo(chainKnotEntity)) {
                Chainable.ChainData chainData = chainable.getChainData(player);
                if (chainData != null) {
                    chainable.attachChain(new Chainable.ChainData(chainKnotEntity, chainData.sourceItem), player, true);
                    attachedAny = true;
                }
            }
        }

        if (attachedAny) {
            level.gameEvent(GameEvent.BLOCK_ATTACH, pos, GameEvent.Context.of(player));
            return InteractionResult.SUCCESS;
        } else {
            if (existingKnot == null && chainKnotEntity != null) {
                chainKnotEntity.discard();
            }
            return InteractionResult.PASS;
        }
    }

    public static List<Chainable> collectChainablesAround(Level level, BlockPos pos, Predicate<Chainable> predicate) {
        double distance = CommonClass.runtimeConfig.getMaxChainRange();
        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()).inflate(distance);
        return level.getEntitiesOfClass(Entity.class, box, entity -> entity instanceof Chainable chainable && predicate.test(chainable)).stream().map(Chainable.class::cast).toList();
    }

    public static boolean hasAnyLeadsToConnect(Level level, BlockPos pos, Player player) {
        LeashFenceKnotEntity leashKnotEntity = null;
        boolean found = false;

        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        for (Mob mobEntity : level.getEntitiesOfClass(Mob.class, new AABB(i - 7.0, j - 7.0, k - 7.0, i + 7.0, j + 7.0, k + 7.0))) {
            if (mobEntity.getLeashHolder() == player) {
                if (leashKnotEntity == null) {
                    leashKnotEntity = LeashFenceKnotEntity.getOrCreateKnot(level, pos);
                    leashKnotEntity.playPlacementSound();
                }
                mobEntity.setLeashedTo(leashKnotEntity, true);
                found = true;
            }
        }
        return found;
    }

    public static void infoToolTip(ItemStack itemStack, Item.TooltipContext context, TooltipFlag tooltipFlag, List<Component> texts) {
        if (CommonClass.runtimeConfig.doShowToolTip()) {
            if (itemStack.is(ModTagRegistry.CATENARY_ITEMS)) {
                if (Screen.hasShiftDown()) {
                    texts.add(1, Component.translatable("message.connectiblechains.connectible_chain_detailed").withStyle(ChatFormatting.AQUA));
                } else {
                    texts.add(1, Component.translatable("message.connectiblechains.connectible_chain").withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }
}