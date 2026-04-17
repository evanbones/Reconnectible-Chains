package com.evandev.connectiblechains.entity;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.platform.Services;
import com.evandev.connectiblechains.tag.ModTagRegistry;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface Chainable {
    String CHAINS_NBT_KEY = "Chains";
    String SOURCE_ITEM_KEY = "SourceItem";

    static double getMaxChainLength() {
        return CommonClass.runtimeConfig.getMaxChainRange();
    }

    private static <E extends HangingEntity & Chainable> boolean canAttachTo(E entity, Entity potentialHolder) {
        if (entity.getChainData(potentialHolder) != null) {
            return false;
        } else if (potentialHolder instanceof Chainable chainable) {
            return !entity.equals(potentialHolder) && chainable.getChainData(entity) == null;
        }
        return false;
    }

    private static <E extends HangingEntity & Chainable> HashSet<ChainData> readChainDataSet(E entity, ValueInput input) {
        HashSet<ChainData> result = new HashSet<>();

        input.childrenList(CHAINS_NBT_KEY).ifPresent(list -> {
            for (ValueInput element : list) {
                ChainData newChainData = null;
                String sourceItemStr = element.getStringOr(SOURCE_ITEM_KEY, "minecraft:chain");
                Identifier itemId = Identifier.tryParse(sourceItemStr);
                Item source = itemId != null ? BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.IRON_CHAIN) : Items.IRON_CHAIN;

                Optional<UUID> uuidOpt = element.read("UUID", UUIDUtil.CODEC);
                Optional<Integer> destX = element.getInt("DestX");
                Optional<Integer> relX = element.getInt("RelX");

                if (uuidOpt.isPresent()) {
                    newChainData = new ChainData(Either.left(uuidOpt.get()), source);
                } else if (destX.isPresent()) {
                    BlockPos desPos = new BlockPos(destX.get(), element.getIntOr("DestY", 0), element.getIntOr("DestZ", 0));
                    BlockPos relPos = desPos.subtract(entity.getPos());
                    newChainData = new ChainData(Either.right(relPos), source);
                } else if (relX.isPresent()) {
                    BlockPos relPos = new BlockPos(relX.get(), element.getIntOr("RelY", 0), element.getIntOr("RelZ", 0));
                    newChainData = new ChainData(Either.right(relPos), source);
                }

                if (newChainData != null) {
                    float slack = element.getFloatOr("Slack", -1f);
                    if (slack >= 0) {
                        newChainData.customSlack = slack;
                    }
                    result.add(newChainData);
                }
            }
        });
        return result;
    }

    private static <E extends HangingEntity & Chainable> void resolveChainDataSet(E entity, HashSet<ChainData> chainDataSet) {
        if (!(entity.level() instanceof ServerLevel serverWorld)) return;

        for (ChainData chainData : new HashSet<>(chainDataSet)) {
            if (chainData.unresolvedChainData != null) {
                Optional<UUID> optionalUUID = chainData.unresolvedChainData.left();
                Optional<BlockPos> optionalBlockPos = chainData.unresolvedChainData.right();

                if (optionalUUID.isPresent()) {
                    Entity chainHolder = serverWorld.getEntity(optionalUUID.get());
                    if (chainHolder != null) {
                        ChainData newChainData = new ChainData(chainHolder, chainData.sourceItem);
                        newChainData.customSlack = chainData.customSlack;
                        entity.replaceChainData(chainData, null);
                        attachChain(entity, newChainData, null, true);
                    }
                } else if (optionalBlockPos.isPresent()) {
                    BlockPos targetPos = entity.getPos().offset(optionalBlockPos.get());

                    if (!serverWorld.isLoaded(targetPos)) {
                        continue;
                    }

                    ChainKnotEntity chainHolder = ChainKnotEntity.getOrNull(serverWorld, targetPos);
                    if (chainHolder != null) {
                        ChainData newChainData = new ChainData(chainHolder, chainData.sourceItem);
                        newChainData.customSlack = chainData.customSlack;
                        entity.replaceChainData(chainData, null);
                        attachChain(entity, newChainData, null, true);
                    }
                }
            }
        }
    }

    private static <E extends HangingEntity & Chainable> void detachChain(E entity, ChainData chainData, boolean sendPacket, boolean dropItem) {
        if (chainData.chainHolder != null && chainData.isAlive()) {
            Entity holder = chainData.chainHolder;

            chainData.kill();
            entity.replaceChainData(chainData, null);
            entity.onChainDetached(chainData);
            if (entity.level() instanceof ServerLevel serverWorld) {
                if (dropItem) {
                    entity.spawnAtLocation(serverWorld, new ItemStack(chainData.sourceItem), 0.0f);
                }

                if (sendPacket) {
                    Services.NETWORK.sendToAllClients(serverWorld.getServer(), new ChainAttachS2CPacket(entity, holder, null, chainData.sourceItem));
                }
                ChainCollisionEntity.destroyCollision(serverWorld, chainData);

                if (holder instanceof ChainKnotEntity knot) {
                    checkAndDiscardKnot(serverWorld, knot);
                }

                if (entity instanceof ChainKnotEntity knot) {
                    checkAndDiscardKnot(serverWorld, knot);
                }
            }
        }
    }

    private static void checkAndDiscardKnot(ServerLevel level, ChainKnotEntity knot) {
        if (!knot.getChainDataSet().isEmpty()) return;

        List<Chainable> incoming = ChainItemCallbacks.collectChainablesAround(level, knot.getPos(), c -> c.getChainData(knot) != null);

        if (incoming.isEmpty()) {
            knot.discard();
            knot.dropItem(level, null);
        }
    }

    private static <E extends HangingEntity & Chainable> void attachChain(E entity, ChainData chainData, @Nullable Entity previousHolder, boolean sendPacket) {
        if (chainData.chainHolder == null) {
            throw new IllegalArgumentException("Given chainData has empty holder");
        }

        entity.replaceChainData(entity.getChainData(previousHolder), chainData);
        entity.onChainAttached(chainData);

        if (sendPacket && entity.level() instanceof ServerLevel serverLevel) {
            Services.NETWORK.sendToAllClients(serverLevel.getServer(), new ChainAttachS2CPacket(entity, previousHolder, chainData.chainHolder, chainData.sourceItem));
            if (chainData.customSlack >= 0) {
                Services.NETWORK.sendToAllClients(serverLevel.getServer(), new ChainSlackSyncS2CPacket(entity.getId(), chainData.chainHolder.getId(), chainData.customSlack));
            }
            if (chainData.chainHolder instanceof Chainable) {
                ChainCollisionEntity.createCollision(entity, chainData);
            }
        }
    }

    static <E extends HangingEntity & Chainable> void tickChain(ServerLevel level, E entity) {
        HashSet<ChainData> chainDataSet = entity.getChainDataSet();
        resolveChainDataSet(entity, chainDataSet);

        for (ChainData chainData : new HashSet<>(chainDataSet)) {
            Entity chainHolder = entity.getChainHolder(chainData);
            if (chainHolder != null) {
                if (entity.isRemoved() || chainHolder.isRemoved()) {
                    Entity.RemovalReason reason = entity.isRemoved() ? entity.getRemovalReason() : chainHolder.getRemovalReason();
                    if (reason != null && reason.shouldDestroy()) {
                        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
                            entity.detachChain(chainData);
                        } else {
                            entity.detachChainWithoutDrop(chainData);
                        }
                    }
                }

                chainHolder = entity.getChainHolder(chainData);
                if (chainHolder != null && chainHolder.level().equals(entity.level())) {
                    float distanceTo = entity.distanceTo(chainHolder);
                    if (!entity.beforeChainTick(chainHolder, distanceTo)) {
                        continue;
                    }

                    if (chainHolder instanceof Chainable) {
                        ChainCollisionEntity.createCollision(entity, chainData);
                    }

                    if (distanceTo > getMaxChainLength()) {
                        entity.breakLongChain(chainData);
                    }
                }
            }
        }
    }

    @Nullable
    private static <E extends HangingEntity & Chainable> Entity getChainHolder(E entity, ChainData chainData) {
        if (!entity.getChainDataSet().contains(chainData)) {
            return null;
        }

        if (chainData.unresolvedChainHolderId != 0 && entity.level().isClientSide()) {
            Entity chainHolder = entity.level().getEntity(chainData.unresolvedChainHolderId);
            if (chainHolder != null) {
                ChainData newData = new ChainData(chainHolder, chainData.sourceItem);
                newData.customSlack = chainData.customSlack;
                entity.replaceChainData(chainData, newData);
            }
        }

        return chainData.chainHolder;
    }

    static SoundType getSourceBlockSoundGroup(Item sourceItem) {
        if (sourceItem instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState().getSoundType();
        } else if (new ItemStack(sourceItem).is(ModTagRegistry.ROPES)) {
            return new SoundType(1.0f, 1.0f, SoundEvents.LEAD_BREAK, SoundType.WOOL.getStepSound(), SoundEvents.LEAD_TIED, SoundType.WOOL.getHitSound(), SoundType.WOOL.getFallSound());
        }
        return SoundType.CHAIN;
    }

    default boolean canAttachTo(Entity entity) {
        return canAttachTo((HangingEntity & Chainable) this, entity);
    }

    HashSet<ChainData> getChainDataSet();

    void replaceChainData(@Nullable ChainData oldChainData, @Nullable ChainData newChainData);

    void setChainData(HashSet<ChainData> chainData);

    default void addUnresolvedChainHolderId(int unresolvedOldChainHolderId, int unresolvedNewChainHolderId, Item sourceItem) {
        ChainData oldChainData = null, newChainData = null;
        if (unresolvedOldChainHolderId != 0) {
            oldChainData = new ChainData(unresolvedOldChainHolderId, sourceItem);
        }
        if (unresolvedNewChainHolderId != 0) {
            newChainData = new ChainData(unresolvedNewChainHolderId, sourceItem);
        }
        this.replaceChainData(oldChainData, newChainData);
    }

    default void readChainDataFromNbt(ValueInput input) {
        input.getString(SOURCE_ITEM_KEY).ifPresent(s -> {
            Identifier id = Identifier.tryParse(s);
            if (id != null) BuiltInRegistries.ITEM.getOptional(id).ifPresent(this::setSourceItem);
        });
        HashSet<ChainData> chainData = readChainDataSet((HangingEntity & Chainable) this, input);
        if (!this.getChainDataSet().isEmpty() && chainData.isEmpty()) {
            this.detachAllChainsWithoutDrop();
        }
        this.setChainData(chainData);
    }

    default void writeChainDataSetToNbt(ValueOutput output, HashSet<ChainData> chainDataSet) {
        output.putString(SOURCE_ITEM_KEY, BuiltInRegistries.ITEM.getKey(getSourceItem()).toString());
        BlockPos relativeTo = ((HangingEntity) this).getPos();

        List<ChainData> validChains = new ArrayList<>();
        for (ChainData chainData : chainDataSet) {
            if (chainData.chainHolder != null || chainData.unresolvedChainData != null) {
                validChains.add(chainData);
            }
        }

        if (!validChains.isEmpty()) {
            ValueOutput.ValueOutputList listOutput = output.childrenList(CHAINS_NBT_KEY);

            for (ChainData chainData : validChains) {
                Either<UUID, BlockPos> either = chainData.unresolvedChainData;
                if (chainData.chainHolder instanceof ChainKnotEntity chainKnotEntity) {
                    either = Either.right(chainKnotEntity.getPos().subtract(relativeTo));
                } else if (chainData.chainHolder != null) {
                    either = Either.left(chainData.chainHolder.getUUID());
                }

                if (either != null) {
                    ValueOutput elementOutput = listOutput.addChild();
                    String sourceItem = BuiltInRegistries.ITEM.getKey(chainData.sourceItem).toString();
                    elementOutput.putString(SOURCE_ITEM_KEY, sourceItem);
                    elementOutput.putFloat("Slack", chainData.customSlack);

                    either.ifLeft(uuid -> {
                        elementOutput.store("UUID", UUIDUtil.CODEC, uuid);
                    }).ifRight(blockPos -> {
                        elementOutput.putInt("RelX", blockPos.getX());
                        elementOutput.putInt("RelY", blockPos.getY());
                        elementOutput.putInt("RelZ", blockPos.getZ());
                    });
                }
            }
        }
    }

    default void detachChain(ChainData chainData) {
        detachChain((HangingEntity & Chainable) this, chainData, true, true);
    }

    default void detachChainWithoutDrop(ChainData chainData) {
        detachChain((HangingEntity & Chainable) this, chainData, true, false);
    }

    default void detachAllChains() {
        for (ChainData chainData : new HashSet<>(this.getChainDataSet())) {
            detachChain(chainData);
        }
    }

    default void detachAllChainsWithoutDrop() {
        for (ChainData chainData : new HashSet<>(this.getChainDataSet())) {
            detachChainWithoutDrop(chainData);
        }
    }

    default void onChainDetached(ChainData removedChainData) {
    }

    default boolean beforeChainTick(Entity chainHolder, float distance) {
        return true;
    }

    default void breakLongChain(ChainData chainData) {
        CommonClass.LOGGER.debug("Breaking chain as it is too long! {}", chainData);
        this.detachChain(chainData);
    }

    default void attachChain(ChainData chainData, @Nullable Entity previousHolder, boolean sendPacket) {
        attachChain((HangingEntity & Chainable) this, chainData, previousHolder, sendPacket);
    }

    default void onChainAttached(ChainData newChainData) {
    }

    @Nullable
    default Entity getChainHolder(ChainData chainData) {
        return getChainHolder((HangingEntity & Chainable) this, chainData);
    }

    @Nullable
    default ChainData getChainData(@Nullable Entity holder) {
        if (holder != null) {
            for (ChainData chainData : new HashSet<>(getChainDataSet())) {
                if (getChainHolder(chainData) == holder) {
                    return chainData;
                }
            }
        }
        return null;
    }

    Item getSourceItem();

    void setSourceItem(Item item);

    default SoundType getSourceBlockSoundGroup() {
        return getSourceBlockSoundGroup(getSourceItem());
    }

    Vec3 getChainPos(float delta);

    final class ChainData {
        public final ArrayList<Integer> collisionStorage = new ArrayList<>(16);
        @NotNull
        public final Item sourceItem;
        public final int unresolvedChainHolderId;
        @Nullable
        private final Entity chainHolder;
        @Nullable
        public Either<UUID, BlockPos> unresolvedChainData;
        public float customSlack = -1f;
        private boolean isDead = false;

        public ChainData(@Nullable Either<UUID, BlockPos> unresolvedChainData, @NotNull Item sourceItem) {
            this.unresolvedChainData = unresolvedChainData;
            this.sourceItem = sourceItem;
            this.chainHolder = null;
            this.unresolvedChainHolderId = 0;
        }

        public ChainData(@Nullable Entity chainHolder, @NotNull Item sourceItem) {
            this.chainHolder = chainHolder;
            this.sourceItem = sourceItem;
            this.unresolvedChainData = null;
            this.unresolvedChainHolderId = 0;
        }

        public ChainData(int unresolvedChainHolderId, @NotNull Item sourceItem) {
            this.unresolvedChainHolderId = unresolvedChainHolderId;
            this.sourceItem = sourceItem;
            this.chainHolder = null;
            this.unresolvedChainData = null;
        }

        public float getSlack() {
            return customSlack < 0 ? CommonClass.runtimeConfig.getChainHangAmount() : customSlack;
        }

        public SoundType getSourceBlockSoundGroup() {
            return Chainable.getSourceBlockSoundGroup(sourceItem);
        }

        private int getHolderId() {
            return chainHolder != null ? chainHolder.getId() : unresolvedChainHolderId;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ChainData chainData)) return false;
            if (this.sourceItem != chainData.sourceItem) return false;

            int thisId = getHolderId();
            int thatId = chainData.getHolderId();
            if (thisId != 0 && thisId == thatId) return true;
            return unresolvedChainData != null && unresolvedChainData.equals(chainData.unresolvedChainData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getHolderId(), unresolvedChainData, sourceItem);
        }

        public void kill() {
            if (isDead) CommonClass.LOGGER.warn("Stop! Stop! {} is already dead!", this);
            isDead = true;
        }

        public boolean isAlive() {
            return !isDead;
        }

        public void applyRotation(Rotation rotation) {
            if (unresolvedChainData != null) {
                unresolvedChainData = unresolvedChainData.mapRight(blockPos -> blockPos.rotate(rotation));
            }
        }

        @Override
        public String toString() {
            return "ChainData{" + "collisionStorage=" + collisionStorage + ", unresolvedChainData=" + unresolvedChainData + ", sourceItem=" + sourceItem + ", unresolvedChainHolderId=" + unresolvedChainHolderId + ", chainHolder=" + chainHolder + ", customSlack=" + customSlack + '}';
        }
    }
}