package com.evandev.connectiblechains.entity;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.platform.Services;
import com.evandev.connectiblechains.tag.ModTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

public class ChainKnotEntity extends HangingEntity implements Chainable, ChainLinkEntity {

    private HashSet<ChainData> chainDataSet = new HashSet<>();
    @NotNull
    private Item sourceItem;

    public ChainKnotEntity(EntityType<ChainKnotEntity> entityType, Level level) {
        super(entityType, level);
        sourceItem = Items.CHAIN;
    }

    public ChainKnotEntity(Level level, BlockPos pos, @NotNull Item sourceItem) {
        super(ModEntityTypes.CHAIN_KNOT.get(), level, pos);
        this.sourceItem = sourceItem;
        setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    @Nullable
    public static ChainKnotEntity getOrNull(Level level, BlockPos pos) {
        List<ChainKnotEntity> chainKnotEntities = level.getEntitiesOfClass(ChainKnotEntity.class, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()).inflate(1));
        for (ChainKnotEntity chainKnotEntity : chainKnotEntities) {
            if (chainKnotEntity.blockPosition().equals(pos)) {
                return chainKnotEntity;
            }
        }
        return null;
    }

    public static ChainKnotEntity getOrCreate(Level level, BlockPos pos, @NotNull Item newSourceItem) {
        ChainKnotEntity chainKnotEntity = getOrNull(level, pos);
        if (chainKnotEntity == null) {
            chainKnotEntity = new ChainKnotEntity(level, pos, newSourceItem);
            level.addFreshEntity(chainKnotEntity);
        }
        return chainKnotEntity;
    }

    @Override
    public HashSet<ChainData> getChainDataSet() {
        return chainDataSet;
    }

    @Override
    public void replaceChainData(@Nullable ChainData oldChainData, @Nullable ChainData newChainData) {
        if (oldChainData != null) {
            if (!chainDataSet.removeIf(chainData -> chainData.equals(oldChainData) || chainData.equals(newChainData))) {
                CommonClass.LOGGER.warn("Attempted to remove {}, from {}. But it was not able to find it?", oldChainData, chainDataSet);
            }
        }
        if (newChainData != null) chainDataSet.add(newChainData);
    }

    @Override
    public void setChainData(HashSet<ChainData> chainDataSet) {
        this.chainDataSet = chainDataSet;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && !this.survives()) {
            this.discard();
            this.dropItem(null);
        }
        super.tick();
        if (this.level() instanceof ServerLevel serverWorld) {
            Chainable.tickChain(serverWorld, this);
        }
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack handStack = player.getItemInHand(hand);
        if (level().isClientSide()) {
            ChainData chainDataForPlayer = getChainData(player);
            if (chainDataForPlayer != null) {
                if (!player.isCreative()) {
                    player.getInventory().add(new ItemStack(chainDataForPlayer.sourceItem));
                }
                return InteractionResult.SUCCESS;
            }
            if (handStack.is(ModTagRegistry.CATENARY_ITEMS)) {
                if (handStack.getItem() != this.sourceItem) return InteractionResult.PASS;
                if (!player.isCreative()) handStack.shrink(1);
                return InteractionResult.SUCCESS;
            }
            if (handStack.is(Items.SHEARS)) return InteractionResult.CONSUME;
            return InteractionResult.PASS;
        }

        if (this.isAlive() && player.level() instanceof ServerLevel) {
            boolean hasConnectedFromPlayer = false;
            List<Chainable> list = ChainItemCallbacks.collectChainablesAround(this.level(), this.getPos(), entity -> entity.getChainData(player) != null);

            for (Chainable chainable : list) {
                ChainData chainData = chainable.getChainData(player);
                if (chainData == null || !chainable.canAttachTo(this)) continue;
                if (chainData.sourceItem != this.sourceItem) continue;

                chainable.attachChain(new ChainData(this, chainData.sourceItem), player, true);
                hasConnectedFromPlayer = true;
            }

            if (hasConnectedFromPlayer) {
                playPlacementSound();
                return InteractionResult.SUCCESS;
            }

            ChainData matchingData = null;
            for (ChainData chainData : new HashSet<>(getChainDataSet())) {
                if (player == getChainHolder(chainData)) {
                    matchingData = chainData;
                    break;
                }
            }
            if (matchingData != null) {
                detachChainWithoutDrop(matchingData);
                if (!player.isCreative()) {
                    player.getInventory().add(new ItemStack(matchingData.sourceItem));
                }
                this.gameEvent(GameEvent.ENTITY_INTERACT, player);
                return InteractionResult.SUCCESS;
            }

            if (handStack.is(ModTagRegistry.CATENARY_ITEMS)) {
                if (handStack.getItem() != this.sourceItem) return InteractionResult.PASS;
                playPlacementSound();
                attachChain(new ChainData(player, handStack.getItem()), null, true);
                if (!player.isCreative()) handStack.shrink(1);
                return InteractionResult.SUCCESS;
            }

            if (handStack.is(Items.SHEARS)) {
                if (player.isCreative()) detachAllChainsWithoutDrop();
                else detachAllChains();
                this.remove(RemovalReason.DISCARDED);
                this.dropItem(player);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean skipAttackInteraction(@NotNull Entity attacker) {
        if (attacker instanceof Player player) {
            if (level().isClientSide) return false;
            if (player.isCreative()) {
                detachAllChainsWithoutDrop();
            } else {
                detachAllChains();
                this.dropItem(player);
            }
            this.remove(RemovalReason.KILLED);
            return true;
        }
        return super.skipAttackInteraction(attacker);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        this.writeChainDataSetToNbt(nbt, this.chainDataSet);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.readChainDataFromNbt(nbt);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        AABB result = super.getBoundingBoxForCulling();
        for (ChainData chainData : new HashSet<>(this.getChainDataSet())) {
            Entity entity = this.getChainHolder(chainData);
            if (entity == null) continue;
            result = result.minmax(entity.getBoundingBox());
        }
        return result;
    }

    @Override
    protected @NotNull AABB calculateBoundingBox(@NotNull BlockPos pos, @NotNull Direction direction) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        double width = this.getType().getWidth();
        double height = this.getType().getHeight();
        double radius = width / 2.0;

        return new AABB(
                x - radius, y, z - radius,
                x + radius, y + height, z + radius
        );
    }

    @Override
    public boolean survives() {
        return this.level().getBlockState(this.blockPosition()).is(ModTagRegistry.CHAIN_CONNECTIBLE);
    }

    @Override
    public void onChainAttached(ChainData newChainData) {
        this.playSound(newChainData.getSourceBlockSoundGroup().getBreakSound(), 1.0F, 1.0F);
    }

    @Override
    public @NotNull Item getSourceItem() {
        return sourceItem;
    }

    @Override
    public void setSourceItem(@NotNull Item sourceItem) {
        this.sourceItem = sourceItem;
    }

    @Override
    public void onChainDetached(ChainData removedChainData) {
        this.playSound(removedChainData.getSourceBlockSoundGroup().getBreakSound(), 1.0F, 1.0F);
    }

    public void playPlacementSound() {
        this.playSound(getSourceBlockSoundGroup().getPlaceSound(), 1.0F, 1.0F);
    }

    @Override
    public void dropItem(@Nullable Entity breaker) {
        this.playSound(getSourceBlockSoundGroup().getBreakSound(), 1.0F, 1.0F);
    }

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        for (ChainData chainData : getChainDataSet()) {
            Services.NETWORK.sendToClient(player, new ChainAttachS2CPacket(this, null, getChainHolder(chainData), chainData.sourceItem));
        }
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        int rawChainItemSourceId = packet.getData();
        this.sourceItem = BuiltInRegistries.ITEM.byId(rawChainItemSourceId);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        int id = BuiltInRegistries.ITEM.getId(this.getSourceItem());
        return new ClientboundAddEntityPacket(this, entity, id);
    }

    @Override
    public float rotate(@NotNull Rotation rotation) {
        for (ChainData chainData : chainDataSet) {
            chainData.applyRotation(rotation);
        }
        return super.rotate(rotation);
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return new ItemStack(getSourceItem());
    }

    public Vec3 getChainPos(float delta) {
        return this.getPosition(delta).add(0.0, 0.2, 0.0);
    }
}