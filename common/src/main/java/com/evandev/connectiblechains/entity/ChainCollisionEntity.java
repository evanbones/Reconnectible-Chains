package com.evandev.connectiblechains.entity;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.util.MathHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChainCollisionEntity extends Entity implements ChainLinkEntity {
    private static final float COLLIDER_SPACING = 1.5f;

    @Nullable
    private Chainable.ChainData link;
    private Entity chainedEntity;
    private Item linkSourceItem;

    public ChainCollisionEntity(Level level, double x, double y, double z, Entity chainedEntity, @NotNull Chainable.ChainData link) {
        this(ModEntityTypes.CHAIN_COLLISION.get(), level);
        this.link = link;
        this.setPos(x, y, z);
        this.linkSourceItem = link.sourceItem;
        this.chainedEntity = chainedEntity;
    }

    public ChainCollisionEntity(EntityType<ChainCollisionEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static <E extends Entity & Chainable> void createCollision(E chainedEntity, Chainable.ChainData chainData) {
        if (chainedEntity.level().isClientSide()) return;

        ServerLevel serverWorld = (ServerLevel) chainedEntity.level();

        if (!CommonClass.runtimeConfig.isCollisionsEnabled()) {
            destroyCollision(serverWorld, chainData);
            return;
        }

        chainData.collisionStorage.removeIf(id -> serverWorld.getEntity(id) == null);

        if (!chainData.collisionStorage.isEmpty()) return;

        Entity chainHolder = chainedEntity.getChainHolder(chainData);
        if (chainHolder == null) return;

        double distance = chainedEntity.distanceTo(chainHolder);
        double step = COLLIDER_SPACING * Math.sqrt(Math.pow(ModEntityTypes.CHAIN_COLLISION.get().getWidth(), 2) * 2) / distance;
        double v = step;
        double centerHoldout = ModEntityTypes.CHAIN_COLLISION.get().getWidth() / distance;

        while (v < 0.5 - centerHoldout) {
            Entity collider1 = spawnCollision(false, chainedEntity, chainData, v);
            if (collider1 != null) chainData.collisionStorage.add(collider1.getId());
            Entity collider2 = spawnCollision(true, chainedEntity, chainData, v);
            if (collider2 != null) chainData.collisionStorage.add(collider2.getId());
            v += step;
        }

        Entity centerCollider = spawnCollision(false, chainedEntity, chainData, 0.5);
        if (centerCollider != null) chainData.collisionStorage.add(centerCollider.getId());
    }

    public static <E extends Entity & Chainable> ChainCollisionEntity spawnCollision(boolean reverse, E chainedEntity, Chainable.ChainData chainData, double distancePercentage) {
        if (!(chainedEntity.level() instanceof ServerLevel serverWorld)) return null;

        Entity chainHolder = chainedEntity.getChainHolder(chainData);
        assert chainHolder != null;

        Vec3 srcPos = chainedEntity.getChainPos(1);
        Vec3 dstPos;
        if (chainHolder instanceof ChainKnotEntity chainKnotEntity) {
            dstPos = chainKnotEntity.getChainPos(1);
        } else {
            dstPos = chainHolder.getLeashOffset(1).add(chainHolder.position());
        }

        Vec3 tmp = dstPos;
        if (reverse) {
            dstPos = srcPos;
            srcPos = tmp;
        }

        double distance = srcPos.distanceTo(dstPos);
        double x = Mth.lerp(distancePercentage, srcPos.x(), dstPos.x());
        double y = srcPos.y() + MathHelper.drip2((distancePercentage * distance), distance, dstPos.y() - srcPos.y(), chainData.getSlack());
        double z = Mth.lerp(distancePercentage, srcPos.z(), dstPos.z());

        y += -ModEntityTypes.CHAIN_COLLISION.get().getHeight() + 2 / 16f;

        ChainCollisionEntity c = new ChainCollisionEntity(serverWorld, x, y, z, chainedEntity, chainData);
        if (serverWorld.addFreshEntity(c)) {
            return c;
        } else {
            CommonClass.LOGGER.warn("Tried to summon collision entity for a chain, failed to do so");
            return null;
        }
    }

    public static void destroyCollision(ServerLevel level, Chainable.ChainData chainData) {
        for (Integer entityId : chainData.collisionStorage) {
            Entity e = level.getEntity(entityId);
            if (e instanceof ChainCollisionEntity) {
                e.discard();
            } else if (e != null) {
                CommonClass.LOGGER.warn("Collision storage contained reference to {} (#{}) which is not a collision entity.", e, entityId);
            }
        }
        chainData.collisionStorage.clear();
    }

    public @Nullable Chainable.ChainData getLink() {
        return link;
    }

    public @NotNull Item getLinkSourceItem() {
        return linkSourceItem;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (this.link == null || !this.link.isAlive()) {
            this.discard();
            return;
        }
        if (this.chainedEntity == null || this.chainedEntity.isRemoved()) {
            this.discard();
            return;
        }
        if (this.chainedEntity instanceof Chainable chainable) {
            if (!chainable.getChainDataSet().contains(this.link)) {
                this.discard();
                return;
            }
            Entity chainHolder = chainable.getChainHolder(this.link);
            if (chainHolder == null || chainHolder.isRemoved()) {
                this.discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(@NotNull Entity attacker) {
        if (!super.skipAttackInteraction(attacker))
            playSound(Chainable.getSourceBlockSoundGroup(getLinkSourceItem()).getHitSound(), 0.5F, 1.0F);
        return false;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (level().isClientSide) return false;

        if (source.getEntity() instanceof Player player) {
            if (!player.getMainHandItem().is(Items.SHEARS)) return false;

            if (getLink() == null) {
                this.discard();
                return false;
            }

            if (chainedEntity instanceof Chainable chainable) {
                CommonClass.LOGGER.debug("Dropping chain ({}) due to left-click with shears.", getLink());
                chainable.detachChain(getLink());

                if (!player.isCreative()) {
                    player.getMainHandItem().hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        player.getItemInHand(hand).is(Items.SHEARS);
        return InteractionResult.PASS;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        int id = BuiltInRegistries.ITEM.getId(linkSourceItem);
        return new ClientboundAddEntityPacket(this, id);
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        int rawChainItemSourceId = packet.getData();
        linkSourceItem = BuiltInRegistries.ITEM.byId(rawChainItemSourceId);
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return new ItemStack(linkSourceItem);
    }
}