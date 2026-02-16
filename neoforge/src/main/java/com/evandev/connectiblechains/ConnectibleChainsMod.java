package com.evandev.connectiblechains;

import com.evandev.connectiblechains.client.ClientConfigSetup;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.evandev.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.evandev.connectiblechains.entity.ModEntityTypes;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.platform.NeoForgeNetworkHelper;
import com.evandev.connectiblechains.platform.NeoForgeRegistryHelper;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
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
        if (ChainRaycastHelper.tryAdjustSlack(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @EventBusSubscriber(modid = CommonClass.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModBusEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            if (ClientInitializer.getInstance() == null) {
                new ClientInitializer().onInitializeClient();
            }
        }

        @SubscribeEvent
        public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
            if (ClientInitializer.getInstance() == null) {
                new ClientInitializer().onInitializeClient();
            }
            event.registerReloadListener(ClientInitializer.getInstance().getChainTextureManager());
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntityTypes.CHAIN_KNOT.get(), ctx -> {
                ChainKnotEntityRenderer renderer = new ChainKnotEntityRenderer(ctx);
                ClientInitializer.getInstance().setChainKnotEntityRenderer(renderer);
                return renderer;
            });
            event.registerEntityRenderer(ModEntityTypes.CHAIN_COLLISION.get(), ChainCollisionEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ClientInitializer.CHAIN_KNOT, ClientInitializer::getChainKnotLayerDefinition);
        }
    }

    @EventBusSubscriber(modid = CommonClass.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ChainItemCallbacks.infoToolTip(event.getItemStack(), event.getContext(), event.getFlags(), event.getToolTip());
        }

        @SubscribeEvent
        public static void onClientJoin(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
            CommonClass.runtimeConfig.copyFrom(CommonClass.fileConfig);
            if (ClientInitializer.getInstance() != null) {
                ClientInitializer.getInstance().getChainKnotEntityRenderer()
                        .ifPresent(r -> r.getChainRenderer().purge());
            }
        }
    }
}