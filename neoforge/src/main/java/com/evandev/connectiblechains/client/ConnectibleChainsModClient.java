package com.evandev.connectiblechains.client;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.evandev.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.evandev.connectiblechains.entity.ModEntityTypes;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.ChainBreakC2SPacket;
import com.evandev.connectiblechains.networking.packet.DecorationRemoveC2SPacket;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class ConnectibleChainsModClient {
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
        public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
            CommonClass.runtimeConfig.copyFrom(CommonClass.fileConfig);
            if (ClientInitializer.getInstance() != null) {
                ClientInitializer.getInstance().getChainKnotEntityRenderer()
                        .ifPresent(r -> r.getChainRenderer().purge());
            }
        }

        @SubscribeEvent
        public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
            if (ChainRaycastHelper.tryBreakChain(event.getEntity())) {
                PacketDistributor.sendToServer(ChainBreakC2SPacket.INSTANCE);
            }
        }

        @SubscribeEvent
        public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
            if (ChainRaycastHelper.tryRemoveDecoration(event.getEntity(), event.getHand())) {
                PacketDistributor.sendToServer(DecorationRemoveC2SPacket.INSTANCE);
                event.getEntity().swing(event.getHand());
            }
        }
    }
}
