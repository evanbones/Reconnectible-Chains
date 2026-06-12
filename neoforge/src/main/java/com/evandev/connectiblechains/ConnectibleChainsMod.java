package com.evandev.connectiblechains;

import com.evandev.connectiblechains.client.ClientConfigSetup;
import com.evandev.connectiblechains.command.ConnectChainCommand;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.platform.NeoForgeNetworkHelper;
import com.evandev.connectiblechains.platform.NeoForgeRegistryHelper;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(CommonClass.MODID)
public class ConnectibleChainsMod {
    public ConnectibleChainsMod(IEventBus modBus, ModContainer modContainer) {
        NeoForgeRegistryHelper.ENTITIES.register(modBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientConfigSetup.register(modContainer);
        }

        CommonClass.init();

        modBus.addListener(NeoForgeNetworkHelper::register);

        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = ChainItemCallbacks.chainUseEvent(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (result.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CommonClass.fileConfig.syncToClient(player);
        }
    }

    private void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (ChainRaycastHelper.tryPlaceBunting(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (ChainRaycastHelper.tryRemoveBunting(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (ChainRaycastHelper.tryAdjustSlack(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ConnectChainCommand.register(event.getDispatcher());
    }

}