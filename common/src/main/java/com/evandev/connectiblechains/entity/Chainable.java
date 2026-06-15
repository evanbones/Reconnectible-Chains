package com.evandev.connectiblechains.entity;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.BannerSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.BuntingSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.HangingSyncS2CPacket;
import com.evandev.connectiblechains.platform.Services;
import com.evandev.connectiblechains.tag.ModTagRegistry;
import com.evandev.connectiblechains.util.HangingLightHelper;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
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

    private static <E extends HangingEntity & Chainable> HashSet<ChainData> readChainDataSet(E entity, CompoundTag nbt) {
        HashSet<ChainData> result = new HashSet<>();
        if (nbt.contains(CHAINS_NBT_KEY, Tag.TAG_LIST)) {
            ListTag list = nbt.getList(CHAINS_NBT_KEY, Tag.TAG_COMPOUND);
            for (Tag element : list) {
                if (!(element instanceof CompoundTag compound)) continue;

                ChainData newChainData = null;
                Item source = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(compound.getString(SOURCE_ITEM_KEY)));

                if (compound.hasUUID("UUID")) {
                    newChainData = new ChainData(Either.left(compound.getUUID("UUID")), source);
                } else if (compound.contains("DestX")) {
                    BlockPos desPos = new BlockPos(compound.getInt("DestX"), compound.getInt("DestY"), compound.getInt("DestZ"));
                    BlockPos relPos = desPos.subtract(entity.getPos());
                    Either<UUID, BlockPos> either = Either.right(relPos);
                    newChainData = new ChainData(either, source);
                } else if (compound.contains("RelX")) {
                    var relPos = new BlockPos(compound.getInt("RelX"), compound.getInt("RelY"), compound.getInt("RelZ"));
                    newChainData = new ChainData(Either.right(relPos), source);
                }

                if (newChainData != null) {
                    if (compound.contains("Slack")) newChainData.customSlack = compound.getFloat("Slack");
                    if (compound.contains("Buntings", Tag.TAG_LIST)) {
                        ListTag buntingList = compound.getList("Buntings", Tag.TAG_COMPOUND);
                        for (Tag bTag : buntingList) {
                            if (!(bTag instanceof CompoundTag buntingTag)) continue;
                            DyeColor color = DyeColor.byName(buntingTag.getString("Color"), null);
                            if (color != null)
                                newChainData.buntings.add(new ChainData.BuntingEntry(buntingTag.getFloat("T"), color));
                        }
                    }
                    if (compound.contains("Banners", Tag.TAG_LIST)) {
                        ListTag bannerList = compound.getList("Banners", Tag.TAG_COMPOUND);
                        for (Tag bTag : bannerList) {
                            if (!(bTag instanceof CompoundTag bannerTag)) continue;
                            CompoundTag data = bannerTag.getCompound("Data");
                            DyeColor color = DyeColor.byName(data.getString("BaseColor"), DyeColor.WHITE);
                            newChainData.banners.add(new ChainData.BannerEntry(bannerTag.getFloat("T"), color, data));
                        }
                    }
                    if (compound.contains("Hangings", Tag.TAG_LIST)) {
                        ListTag hangingList = compound.getList("Hangings", Tag.TAG_COMPOUND);
                        for (Tag hTag : hangingList) {
                            if (!(hTag instanceof CompoundTag hangingTag)) continue;
                            ResourceLocation blockId = ResourceLocation.tryParse(hangingTag.getString("Block"));
                            if (blockId != null)
                                newChainData.hangings.add(new ChainData.HangingEntry(hangingTag.getFloat("T"), blockId));
                        }
                    }
                    result.add(newChainData);
                }
            }
        }
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
                        newChainData.buntings.addAll(chainData.buntings);
                        newChainData.banners.addAll(chainData.banners);
                        newChainData.hangings.addAll(chainData.hangings);
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
                        newChainData.buntings.addAll(chainData.buntings);
                        newChainData.banners.addAll(chainData.banners);
                        newChainData.hangings.addAll(chainData.hangings);
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
                    entity.spawnAtLocation(chainData.sourceItem);
                    for (ChainData.BuntingEntry entry : chainData.buntings) {
                        Item buntingItem = BuiltInRegistries.ITEM.get(new ResourceLocation("supplementaries", "bunting_" + entry.color().getName()));
                        if (buntingItem != Items.AIR) {
                            entity.spawnAtLocation(buntingItem);
                        }
                    }
                    for (ChainData.BannerEntry entry : chainData.banners) {
                        ItemStack bannerStack = BannerBlock.byColor(entry.color()).asItem().getDefaultInstance();
                        if (entry.data().contains("Patterns", Tag.TAG_LIST)) {
                            CompoundTag blockEntityTag = new CompoundTag();
                            blockEntityTag.put("Patterns", entry.data().getList("Patterns", Tag.TAG_COMPOUND));
                            bannerStack.addTagElement("BlockEntityTag", blockEntityTag);
                        }
                        entity.spawnAtLocation(bannerStack);
                    }
                    for (ChainData.HangingEntry entry : chainData.hangings) {
                        Item hangingItem = BuiltInRegistries.ITEM.get(entry.blockId());
                        if (hangingItem != Items.AIR) entity.spawnAtLocation(hangingItem);
                    }
                }

                if (sendPacket) {
                    Services.NETWORK.sendToAllClients(serverWorld.getServer(), new ChainAttachS2CPacket(entity, holder, null, chainData.sourceItem));
                }
                ChainCollisionEntity.destroyCollision(serverWorld, chainData);
                HangingLightHelper.removeAllForChain(serverWorld, entity, holder, chainData);

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
            knot.dropItem(null);
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
            if (!chainData.buntings.isEmpty()) {
                Services.NETWORK.sendToAllClients(serverLevel.getServer(), new BuntingSyncS2CPacket(entity.getId(), chainData.chainHolder.getId(), new ArrayList<>(chainData.buntings)));
            }
            if (!chainData.banners.isEmpty()) {
                Services.NETWORK.sendToAllClients(serverLevel.getServer(), new BannerSyncS2CPacket(entity.getId(), chainData.chainHolder.getId(), new ArrayList<>(chainData.banners)));
            }
            if (!chainData.hangings.isEmpty()) {
                Services.NETWORK.sendToAllClients(serverLevel.getServer(), new HangingSyncS2CPacket(entity.getId(), chainData.chainHolder.getId(), new ArrayList<>(chainData.hangings)));
            }
            if (chainData.chainHolder instanceof Chainable) {
                ChainCollisionEntity.createCollision(entity, chainData);
            }
            HangingLightHelper.placeAllForChain(serverLevel, entity, chainData.chainHolder, chainData);
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
                        if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
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
                        if (!chainData.hangings.isEmpty() && level.getGameTime() % 40 == 0) {
                            HangingLightHelper.placeAllForChain(level, entity, chainHolder, chainData);
                        }
                    }

                    double maxRange = getMaxChainLength();

                    if (chainHolder instanceof Player player) {
                        double warningStart = maxRange * 0.75;
                        if (distanceTo > warningStart) {
                            float ratio = (float) ((distanceTo - warningStart) / (maxRange - warningStart));
                            int interval = Math.max(2, (int) (20.0f * (1.0f - ratio)));
                            if (++chainData.warningTickCounter >= interval) {
                                chainData.warningTickCounter = 0;
                                SoundType soundType = chainData.getSourceBlockSoundGroup();
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        soundType.getHitSound(), SoundSource.PLAYERS, 0.4f,
                                        0.6f + player.level().getRandom().nextFloat() * 0.4f);
                                if (CommonClass.runtimeConfig.doShowRangeWarningHud() && player instanceof ServerPlayer serverPlayer) {
                                    serverPlayer.displayClientMessage(
                                            Component.translatable("message.connectiblechains.chain_range_warning").withStyle(ChatFormatting.YELLOW),
                                            true);
                                }
                            }
                        } else {
                            chainData.warningTickCounter = 0;
                        }
                    }

                    if (distanceTo > maxRange) {
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

        if (chainData.unresolvedChainHolderId != 0 && entity.level().isClientSide) {
            Entity chainHolder = entity.level().getEntity(chainData.unresolvedChainHolderId);
            if (chainHolder != null) {
                ChainData newData = new ChainData(chainHolder, chainData.sourceItem);
                newData.customSlack = chainData.customSlack;
                newData.buntings.addAll(chainData.buntings);
                newData.banners.addAll(chainData.banners);
                newData.hangings.addAll(chainData.hangings);
                entity.replaceChainData(chainData, newData);
            }
        }

        return chainData.chainHolder;
    }

    static SoundType getSourceBlockSoundGroup(Item sourceItem) {
        if (sourceItem instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState().getSoundType();
        } else if (new ItemStack(sourceItem).is(ModTagRegistry.ROPES)) {
            return new SoundType(1.0f, 1.0f, SoundEvents.LEASH_KNOT_BREAK, SoundType.WOOL.getStepSound(), SoundEvents.LEASH_KNOT_PLACE, SoundType.WOOL.getHitSound(), SoundType.WOOL.getFallSound());
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

    default void readChainDataFromNbt(CompoundTag nbt) {
        setSourceItem(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(nbt.getString(SOURCE_ITEM_KEY))));
        HashSet<ChainData> chainData = readChainDataSet((HangingEntity & Chainable) this, nbt);
        if (!this.getChainDataSet().isEmpty() && chainData.isEmpty()) {
            this.detachAllChainsWithoutDrop();
        }
        this.setChainData(chainData);
    }

    default void writeChainDataSetToNbt(CompoundTag nbt, HashSet<ChainData> chainDataSet) {
        nbt.putString(SOURCE_ITEM_KEY, BuiltInRegistries.ITEM.getKey(getSourceItem()).toString());
        BlockPos relativeTo = ((HangingEntity) this).getPos();

        ListTag linksTag = new ListTag();
        for (ChainData chainData : chainDataSet) {
            Either<UUID, BlockPos> either = chainData.unresolvedChainData;
            if (chainData.chainHolder instanceof ChainKnotEntity chainKnotEntity) {
                either = Either.right(chainKnotEntity.getPos().subtract(relativeTo));
            } else if (chainData.chainHolder != null) {
                either = Either.left(chainData.chainHolder.getUUID());
            }

            if (either != null) {
                String sourceItem = BuiltInRegistries.ITEM.getKey(chainData.sourceItem).toString();
                linksTag.add(either.map(uuid -> {
                    CompoundTag nbtCompound = new CompoundTag();
                    nbtCompound.putUUID("UUID", uuid);
                    nbtCompound.putString(SOURCE_ITEM_KEY, sourceItem);
                    nbtCompound.putFloat("Slack", chainData.customSlack);
                    writeBuntings(nbtCompound, chainData);
                    writeBanners(nbtCompound, chainData);
                    writeHangings(nbtCompound, chainData);
                    return nbtCompound;
                }, blockPos -> {
                    CompoundTag nbtCompound = new CompoundTag();
                    nbtCompound.putInt("RelX", blockPos.getX());
                    nbtCompound.putInt("RelY", blockPos.getY());
                    nbtCompound.putInt("RelZ", blockPos.getZ());
                    nbtCompound.putString(SOURCE_ITEM_KEY, sourceItem);
                    nbtCompound.putFloat("Slack", chainData.customSlack);
                    writeBuntings(nbtCompound, chainData);
                    writeBanners(nbtCompound, chainData);
                    writeHangings(nbtCompound, chainData);
                    return nbtCompound;
                }));
            }
        }
        if (!linksTag.isEmpty()) {
            nbt.put(CHAINS_NBT_KEY, linksTag);
        }
    }

    private static void writeBuntings(CompoundTag nbt, ChainData chainData) {
        if (!chainData.buntings.isEmpty()) {
            ListTag buntingList = new ListTag();
            for (ChainData.BuntingEntry entry : chainData.buntings) {
                CompoundTag bt = new CompoundTag();
                bt.putFloat("T", entry.t());
                bt.putString("Color", entry.color().getName());
                buntingList.add(bt);
            }
            nbt.put("Buntings", buntingList);
        }
    }

    private static void writeBanners(CompoundTag nbt, ChainData chainData) {
        if (!chainData.banners.isEmpty()) {
            ListTag bannerList = new ListTag();
            for (ChainData.BannerEntry entry : chainData.banners) {
                CompoundTag bt = new CompoundTag();
                bt.putFloat("T", entry.t());
                bt.put("Data", entry.data());
                bannerList.add(bt);
            }
            nbt.put("Banners", bannerList);
        }
    }

    private static void writeHangings(CompoundTag nbt, ChainData chainData) {
        if (!chainData.hangings.isEmpty()) {
            ListTag hangingList = new ListTag();
            for (ChainData.HangingEntry entry : chainData.hangings) {
                CompoundTag ht = new CompoundTag();
                ht.putFloat("T", entry.t());
                ht.putString("Block", entry.blockId().toString());
                hangingList.add(ht);
            }
            nbt.put("Hangings", hangingList);
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
        public final List<BuntingEntry> buntings = new ArrayList<>();
        public final List<BannerEntry> banners = new ArrayList<>();
        public final List<HangingEntry> hangings = new ArrayList<>();
        @Nullable
        private final Entity chainHolder;
        @Nullable
        public Either<UUID, BlockPos> unresolvedChainData;
        public float customSlack = -1f;
        public int warningTickCounter = 0;
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

        public record BuntingEntry(float t, DyeColor color) {
        }

        public record BannerEntry(float t, DyeColor color, CompoundTag data) {
        }

        public record HangingEntry(float t, ResourceLocation blockId) {
        }
    }
}
