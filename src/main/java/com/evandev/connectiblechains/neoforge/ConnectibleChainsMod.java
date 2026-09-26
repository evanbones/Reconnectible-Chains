package com.evandev.connectiblechains.neoforge;

//? if neoforge {
/*import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.command.ConnectChainCommand;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.neoforge.client.ClientConfigSetup;
import com.evandev.connectiblechains.platform.neoforge.NeoForgeNetworkHelper;
import com.evandev.connectiblechains.platform.neoforge.NeoForgeRegistryHelper;
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
//? if <26.1 {
/^import com.evandev.connectiblechains.networking.packet.DecorationRemoveC2SPacket;
import net.neoforged.neoforge.network.PacketDistributor;
^///?}

@Mod(CommonClass.MODID)
public class ConnectibleChainsMod {
    public ConnectibleChainsMod(IEventBus modBus, ModContainer modContainer) {
        NeoForgeRegistryHelper.ENTITIES.register(modBus);

        //? if >=26.1 {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
        //?} else {
        /^if (FMLEnvironment.dist == Dist.CLIENT) {
        ^///?}
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
        //? if <26.1 {
        /^if (event.getLevel().isClientSide()) {
            if (ChainRaycastHelper.tryRemoveDecoration(event.getEntity(), event.getHand())) {
                PacketDistributor.sendToServer(DecorationRemoveC2SPacket.INSTANCE);
                event.getEntity().swing(event.getHand());
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }

        ^///?}
        InteractionResult result = ChainItemCallbacks.chainUseEvent(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());

        if (result.consumesAction() || result == InteractionResult.FAIL) {
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
        if (ChainRaycastHelper.tryPlaceBanner(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (ChainRaycastHelper.tryPlaceHanging(event.getEntity(), event.getHand())) {
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
*///?}
