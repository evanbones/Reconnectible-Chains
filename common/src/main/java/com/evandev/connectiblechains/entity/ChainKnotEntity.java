package com.evandev.connectiblechains.entity;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.platform.Services;
import com.evandev.connectiblechains.tag.ModTagRegistry;
import com.evandev.connectiblechains.util.ChainTracker;
import com.evandev.connectiblechains.util.MathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
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
    public Direction attachedFace = Direction.UP;
    private HashSet<ChainData> chainDataSet = new HashSet<>();
    @NotNull
    private Item sourceItem;

    public ChainKnotEntity(EntityType<ChainKnotEntity> entityType, Level level) {
        super(entityType, level);
        sourceItem = Items.CHAIN;
    }

    public ChainKnotEntity(Level level, BlockPos pos, @NotNull Item sourceItem, Direction face) {
        super(ModEntityTypes.CHAIN_KNOT.get(), level, pos);
        this.sourceItem = sourceItem;
        this.attachedFace = face != null ? face : Direction.UP;
        setPos(pos.getX(), pos.getY(), pos.getZ());
    }

    @Nullable
    public static ChainKnotEntity getOrNull(Level level, BlockPos pos) {
        List<ChainKnotEntity> chainKnotEntities = level.getEntitiesOfClass(ChainKnotEntity.class, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()).inflate(1));
        for (ChainKnotEntity chainKnotEntity : chainKnotEntities) {
            if (chainKnotEntity.getPos().equals(pos) && !chainKnotEntity.isRemoved()) {
                return chainKnotEntity;
            }
        }
        return null;
    }

    public static ChainKnotEntity getOrCreate(Level level, BlockPos pos, @NotNull Item newSourceItem, Direction face) {
        ChainKnotEntity chainKnotEntity = getOrNull(level, pos);
        if (chainKnotEntity == null) {
            chainKnotEntity = new ChainKnotEntity(level, pos, newSourceItem, face);
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
    public void tick() {
        if (!this.level().isClientSide && !this.survives()) {
            this.discard();
            this.dropItem(null);
        }
        super.tick();

        ChainTracker.register(this.level(), this);

        if (this.level() instanceof ServerLevel serverWorld) {
            Chainable.tickChain(serverWorld, this);
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!this.level().isClientSide) {
            this.detachAllChains();
        }

        ChainTracker.unregister(this.level(), this);

        super.remove(reason);
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
            if (handStack.is(Items.SHEARS)) return InteractionResult.SUCCESS;
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
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean skipAttackInteraction(@NotNull Entity attacker) {
        if (!super.skipAttackInteraction(attacker)) playSound(getSourceBlockSoundGroup().getHitSound(), 0.5F, 1.0F);
        return true;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        this.writeChainDataSetToNbt(nbt, this.chainDataSet);
        nbt.putInt("AttachedFace", this.attachedFace.get3DDataValue());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.readChainDataFromNbt(nbt);
        if (nbt.contains("AttachedFace")) {
            this.attachedFace = Direction.from3DDataValue(nbt.getInt("AttachedFace"));
        }
    }

    @Override
    protected void recalculateBoundingBox() {
        BlockPos attachedBlockPos = this.pos;
        this.setPosRaw(attachedBlockPos.getX() + 0.5D, attachedBlockPos.getY() + 0.5D, attachedBlockPos.getZ() + 0.5D);

        double width = getType().getWidth() / 2.0;
        double height = getType().getHeight();

        setBoundingBox(new AABB(getX() - width, getY(), getZ() - width, getX() + width, getY() + height, getZ() + width));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double maxRange = Chainable.getMaxChainLength();
        double effectiveRange = maxRange + 64.0;

        double d = this.getBoundingBoxForCulling().getSize();
        if (Double.isNaN(d)) {
            d = 1.0D;
        }
        d *= 64.0D * getViewScale();

        if (!this.getChainDataSet().isEmpty()) {
            return distance < Math.max(d * d, effectiveRange * effectiveRange);
        }

        return distance < d * d || super.shouldRenderAtSqrDistance(distance);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        AABB result = super.getBoundingBoxForCulling();
        for (ChainData chainData : new HashSet<>(this.getChainDataSet())) {
            Entity entity = this.getChainHolder(chainData);
            if (entity == null) continue;

            result = result.minmax(entity.getBoundingBox());

            double distance = this.position().distanceTo(entity.position());
            double dy = entity.getY() - this.getY();
            double sag = Math.abs(MathHelper.drip2(distance / 2.0, distance, dy, chainData.getSlack()));

            double minY = Math.min(this.getY(), entity.getY()) - sag - 1.0;
            result = result.minmax(new AABB(this.getX(), minY, this.getZ(), this.getX(), minY, this.getZ()));
        }
        return result.inflate(1.0);
    }

    @Override
    public boolean survives() {
        return this.level().getBlockState(this.getPos()).is(ModTagRegistry.CHAIN_CONNECTIBLE);
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
            Entity holder = getChainHolder(chainData);
            Services.NETWORK.sendToClient(player, new ChainAttachS2CPacket(this, null, holder, chainData.sourceItem));
            if (holder != null && chainData.customSlack >= 0) {
                Services.NETWORK.sendToClient(player, new ChainSlackSyncS2CPacket(this.getId(), holder.getId(), chainData.customSlack));
            }
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        int id = BuiltInRegistries.ITEM.getId(getSourceItem());
        int data = id | (this.attachedFace.get3DDataValue() << 24);
        return new ClientboundAddEntityPacket(this, data, this.getPos());
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        int data = packet.getData();
        this.sourceItem = BuiltInRegistries.ITEM.byId(data & 0xFFFFFF);
        this.attachedFace = Direction.from3DDataValue((data >> 24) & 0xFF);
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

    @Override
    public Vec3 getChainPos(float delta) {
        double offset = 0.3;
        return this.getPosition(delta).add(
                attachedFace.getStepX() * offset,
                attachedFace.getStepY() * offset,
                attachedFace.getStepZ() * offset
        );
    }

    @Override
    public int getWidth() {
        return 9;
    }

    @Override
    public int getHeight() {
        return 9;
    }
}