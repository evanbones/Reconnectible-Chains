package com.evandev.connectiblechains.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public interface ChainLinkEntity {
    private static <E extends Entity & ChainLinkEntity> InteractionResult onDamageFrom(E self, DamageSource source, SoundEvent hitSound) {
        if (self.level().isClientSide) {
            return InteractionResult.PASS;
        }
        // SERVER-SIDE
        if (self.isInvulnerable()) {
            return InteractionResult.FAIL;
        }

        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return InteractionResult.SUCCESS;
        }

        if (source.getEntity() instanceof Player player) {
            if (player.getMainHandItem().is(Items.SHEARS)) {
                if (!player.isCreative()) {
                    player.getMainHandItem().hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }

        if (!source.is(DamageTypeTags.IS_PROJECTILE)) {
            self.playSound(hitSound, 0.5F, 1.0F);
        }
        return InteractionResult.FAIL;
    }

    default InteractionResult onDamageFrom(DamageSource source, SoundEvent hitSound) {
        return onDamageFrom((Entity & ChainLinkEntity) this, source, hitSound);
    }
}