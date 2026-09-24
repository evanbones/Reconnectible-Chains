package com.evandev.connectiblechains.entity;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.compat.sable.SableHelper;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.*;
import com.evandev.connectiblechains.platform.Services;
import com.evandev.connectiblechains.tag.ModTagRegistry;
import com.evandev.connectiblechains.util.ChainCollisionIndex;
import com.evandev.connectiblechains.util.ChainTracker;
import com.evandev.connectiblechains.util.MathHelper;
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
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

public class ChainKnotEntity extends HangingEntity implements Chainable, ChainLinkEntity {
    private static final ChainData[] EMPTY_CHAIN_DATA = new ChainData[0];
    public Direction attachedFace = Direction.UP;
    private HashSet<ChainData> chainDataSet = new HashSet<>();
    private volatile ChainData[] chainDataArray = EMPTY_CHAIN_DATA;
    @NotNull
    private Item sourceItem;
    private float knotScale = Float.NaN;
    private BlockState knotScaleState;

    public ChainKnotEntity(EntityType<ChainKnotEntity> entityType, Level level) {
        super(entityType, level);
        sourceItem = Items.CHAIN;
    }

    public ChainKnotEntity(Level level, BlockPos pos, @NotNull Item sourceItem, Direction face) {
        super(ModEntityTypes.CHAIN_KNOT.get(), level, pos);
        this.sourceItem = sourceItem;
        this.attachedFace = face != null ? face : Direction.UP;
        this.recalculateBoundingBox();
    }

    @Nullable
    public static ChainKnotEntity getOrNull(Level level, BlockPos pos) {
        List<ChainKnotEntity> chainKnotEntities = level.getEntitiesOfClass(ChainKnotEntity.class, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()).inflate(1));
        for (ChainKnotEntity chainKnotEntity : chainKnotEntities) {
            if (chainKnotEntity.blockPosition().equals(pos) && !chainKnotEntity.isRemoved()) {
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

    public ChainData[] getChainDataArray() {
        return chainDataArray;
    }

    private void updateChainDataArray() {
        this.chainDataArray = this.chainDataSet.isEmpty() ? EMPTY_CHAIN_DATA : this.chainDataSet.toArray(new ChainData[0]);
    }

    @Override
    public @Nullable ChainData getChainData(@Nullable Entity holder) {
        if (holder == null) return null;
        ChainData[] chains = this.chainDataArray;
        for (ChainData chain : chains) {
            if (getChainHolder(chain) == holder) {
                return chain;
            }
        }
        return null;
    }

    @Override
    public @Nullable ChainData getChainData(int holderId) {
        if (holderId == 0) return null;
        ChainData[] chains = this.chainDataArray;
        for (ChainData chain : chains) {
            if (chain.getHolderId() == holderId) {
                return chain;
            }
        }
        return null;
    }

    @Override
    public void replaceChainData(@Nullable ChainData oldChainData, @Nullable ChainData newChainData) {
        if (oldChainData != null) {
            if (!chainDataSet.removeIf(chainData -> chainData.equals(oldChainData) || chainData.equals(newChainData))) {
                CommonClass.LOGGER.warn("Attempted to remove {}, from {}. But it was not able to find it?", oldChainData, chainDataSet);
            }
        }
        if (newChainData != null) chainDataSet.add(newChainData);
        updateChainDataArray();
    }

    @Override
    public void setChainData(HashSet<ChainData> chainDataSet) {
        this.chainDataSet = chainDataSet;
        updateChainDataArray();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        ChainTracker.register(this.level(), this);

        if (this.level() instanceof ServerLevel serverWorld) {
            if (!this.isRemoved() && !this.survives()) {
                this.dropItem(null);
                this.discard();
                return;
            }

            Chainable.tickChain(serverWorld, this);
        } else {
            resolveClientHolders();
        }

        if (!this.isRemoved()) {
            syncCollision();
        }
    }

    private void resolveClientHolders() {
        ChainData[] chains = this.chainDataArray;
        boolean anyUnresolved = false;
        for (ChainData chain : chains) {
            if (chain.needsResolution()) {
                anyUnresolved = true;
                break;
            }
        }
        if (!anyUnresolved) return;

        for (ChainData chain : chains) {
            getChainHolder(chain);
        }
    }

    private void syncCollision() {
        ChainData[] chains = this.chainDataArray;
        for (ChainData chainData : chains) {
            Entity chainHolder = chainData.getResolvedHolder();
            if (chainHolder instanceof Chainable && !chainHolder.isRemoved()) {
                ChainCollisionIndex.ensure(this.level(), this, chainHolder, chainData);
            }
        }
    }

    public float getKnotScale() {
        BlockState state = this.level().getBlockState(this.blockPosition());
        if (Float.isNaN(knotScale) || state != knotScaleState) {
            knotScaleState = state;
            knotScale = computeKnotScale();
        }
        return knotScale;
    }

    private float computeKnotScale() {
        Direction face = this.attachedFace;
        BlockState blockState = this.level().getBlockState(this.blockPosition());
        VoxelShape shape = blockState.getShape(this.level(), this.blockPosition());
        if (shape.isEmpty()) return 5 / 6f;

        double lx = this.getX() - Math.floor(this.getX());
        double ly = this.getY() - Math.floor(this.getY());
        double lz = this.getZ() - Math.floor(this.getZ());

        double push = 0.05;
        lx -= face.getStepX() * push;
        ly -= face.getStepY() * push;
        lz -= face.getStepZ() * push;

        AABB attachmentPoint = new AABB(lx - 0.05, ly - 0.05, lz - 0.05, lx + 0.05, ly + 0.05, lz + 0.05);
        AABB bestBox = null;

        for (AABB box : shape.toAabbs()) {
            if (box.intersects(attachmentPoint)) {
                bestBox = box;
                break;
            }
        }

        if (bestBox == null) {
            bestBox = shape.bounds();
        }

        double dim1 = 0, dim2 = 0;
        switch (face.getAxis()) {
            case Y -> {
                dim1 = bestBox.getXsize();
                dim2 = bestBox.getZsize();
            }
            case Z -> {
                dim1 = bestBox.getXsize();
                dim2 = bestBox.getYsize();
            }
            case X -> {
                dim1 = bestBox.getYsize();
                dim2 = bestBox.getZsize();
            }
        }

        double minDim = Math.min(dim1, dim2);
        return Math.max(0.5f, Math.min(1.5f, (float) (minDim + 0.0625) / 0.375f));
    }


    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!this.level().isClientSide) {
            this.detachAllChains();
        }

        ChainTracker.unregister(this.level(), this);
        ChainCollisionIndex.removeAllOwnedBy(this.level(), this);

        super.remove(reason);
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        ChainTracker.unregister(this.level(), this);
        ChainCollisionIndex.removeAllOwnedBy(this.level(), this);
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
            if (handStack.is(ModTagRegistry.SHEAR_TOOLS)) return InteractionResult.SUCCESS;
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

            if (handStack.is(ModTagRegistry.SHEAR_TOOLS)) {
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
        this.recalculateBoundingBox();
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

        if (this.chainDataArray.length > 0) {
            return distance < Math.max(d * d, effectiveRange * effectiveRange);
        }

        return distance < d * d || super.shouldRenderAtSqrDistance(distance);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        AABB result = super.getBoundingBoxForCulling();
        ChainData[] chains = this.chainDataArray;
        if (chains.length == 0) {
            return result;
        }

        for (ChainData chainData : chains) {
            Entity entity = chainData.getResolvedHolder();
            if (entity == null) {
                entity = this.getChainHolder(chainData);
            }
            if (entity == null) continue;

            Vec3 holderPos = SableHelper.getHolderPosInEntitySpace(this.level(), this, entity);
            if (!Chainable.isValidChainDistance(this.position(), holderPos)) continue;

            AABB holderBox = entity.getBoundingBox().move(holderPos.subtract(entity.position()));
            result = result.minmax(holderBox);

            double distance = this.position().distanceTo(holderPos);
            double dy = holderPos.y() - this.getY();
            double sag = Math.abs(MathHelper.drip2(distance / 2.0, distance, dy, chainData.getSlack()));
            double minY = Math.min(this.getY(), holderPos.y()) - sag - 1.0;
            result = result.minmax(new AABB(this.getX(), minY, this.getZ(), this.getX(), minY, this.getZ()));
        }
        return result.inflate(1.0);
    }

    @Override
    protected @NotNull AABB calculateBoundingBox(@NotNull BlockPos pos, @NotNull Direction direction) {
        Direction face = this.attachedFace != null ? this.attachedFace : Direction.UP;

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        double width = this.getType().getWidth() / 2.0;
        double height = this.getType().getHeight();

        double tipX = x + face.getStepX() * height;
        double tipY = y + face.getStepY() * height;
        double tipZ = z + face.getStepZ() * height;

        double spreadX = face.getAxis() == Direction.Axis.X ? 0.0 : width;
        double spreadY = face.getAxis() == Direction.Axis.Y ? 0.0 : width;
        double spreadZ = face.getAxis() == Direction.Axis.Z ? 0.0 : width;

        return new AABB(
                Math.min(x, tipX) - spreadX, Math.min(y, tipY) - spreadY, Math.min(z, tipZ) - spreadZ,
                Math.max(x, tipX) + spreadX, Math.max(y, tipY) + spreadY, Math.max(z, tipZ) + spreadZ
        );
    }

    @Override
    protected AABB calculateSupportBox() {
        return new AABB(this.blockPosition());
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
        ChainData[] chains = this.chainDataArray;
        for (ChainData chainData : chains) {
            Entity holder = getChainHolder(chainData);
            Services.NETWORK.sendToClient(player, new ChainAttachS2CPacket(this, null, holder, chainData.sourceItem));
            if (holder != null && chainData.customSlack >= 0) {
                Services.NETWORK.sendToClient(player, new ChainSlackSyncS2CPacket(this.getId(), holder.getId(), chainData.customSlack));
            }
            if (holder != null && !chainData.buntings.isEmpty()) {
                Services.NETWORK.sendToClient(player, new BuntingSyncS2CPacket(this.getId(), holder.getId(), chainData.buntings));
            }
            if (holder != null && !chainData.banners.isEmpty()) {
                Services.NETWORK.sendToClient(player, new BannerSyncS2CPacket(this.getId(), holder.getId(), chainData.banners));
            }
            if (holder != null && !chainData.hangings.isEmpty()) {
                Services.NETWORK.sendToClient(player, new HangingSyncS2CPacket(this.getId(), holder.getId(), chainData.hangings));
            }
        }
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        int data = packet.getData();
        this.sourceItem = BuiltInRegistries.ITEM.byId(data & 0xFFFFFF);
        this.attachedFace = Direction.from3DDataValue((data >> 24) & 0xFF);
        this.recalculateBoundingBox();
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        int id = BuiltInRegistries.ITEM.getId(this.getSourceItem());
        int data = id | (this.attachedFace.get3DDataValue() << 24);
        return new ClientboundAddEntityPacket(this, entity, data);
    }

    @Override
    public float rotate(@NotNull Rotation rotation) {
        ChainData[] chains = this.chainDataArray;
        for (ChainData chain : chains) {
            chain.applyRotation(rotation);
        }
        return super.rotate(rotation);
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return new ItemStack(getSourceItem());
    }

    public Vec3 getChainPos(float delta) {
        double offset = 0.1;
        return this.getPosition(delta).add(
                attachedFace.getStepX() * offset,
                attachedFace.getStepY() * offset,
                attachedFace.getStepZ() * offset
        );
    }

    @Override
    public Vec3 getLightProbePosition(float partialTicks) {
        AABB aabb = this.getBoundingBox();
        BlockPos bestPos = this.blockPosition();
        int best = Integer.MIN_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ),
                BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ))) {
            int brightness = Math.max(
                    this.level().getBrightness(LightLayer.BLOCK, pos),
                    this.level().getBrightness(LightLayer.SKY, pos));
            if (brightness == 15) return Vec3.atCenterOf(pos);
            if (brightness > best) {
                best = brightness;
                bestPos = pos.immutable();
            }
        }

        return Vec3.atCenterOf(bestPos);
    }
}