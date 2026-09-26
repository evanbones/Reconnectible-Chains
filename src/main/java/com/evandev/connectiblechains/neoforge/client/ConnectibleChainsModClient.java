package com.evandev.connectiblechains.neoforge.client;

//? if neoforge {
/*import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.evandev.connectiblechains.entity.ModEntityTypes;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.ChainBreakC2SPacket;
import com.evandev.connectiblechains.networking.packet.DecorationRemoveC2SPacket;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
//?}
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
//? if >=26.1 {
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?} else {
/^import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
^///?}

public class ConnectibleChainsModClient {
    private static void sendToServer(CustomPacketPayload payload) {
        //? if >=26.1 {
        ClientPacketDistributor.sendToServer(payload);
        //?} else {
        /^PacketDistributor.sendToServer(payload);
        ^///?}
    }

    @EventBusSubscriber(modid = CommonClass.MODID, value = Dist.CLIENT)
    public static class ClientModBusEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            if (ClientInitializer.getInstance() == null) {
                new ClientInitializer().onInitializeClient();
            }
        }

        @SubscribeEvent
        //? if >=26.1 {
        public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        //?} else {
        /^public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        ^///?}
            if (ClientInitializer.getInstance() == null) {
                new ClientInitializer().onInitializeClient();
            }
            //? if >=26.1 {
            event.addListener(
                    Identifier.fromNamespaceAndPath(CommonClass.MODID, "chain_textures"),
                    ClientInitializer.getInstance().getChainTextureManager()
            );
            //?} else {
            /^event.registerReloadListener(ClientInitializer.getInstance().getChainTextureManager());
            ^///?}
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntityTypes.CHAIN_KNOT.get(), ctx -> {
                ChainKnotEntityRenderer renderer = new ChainKnotEntityRenderer(ctx);
                ClientInitializer.getInstance().setChainKnotEntityRenderer(renderer);
                return renderer;
            });
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ClientInitializer.CHAIN_KNOT, ClientInitializer::getChainKnotLayerDefinition);
        }
    }

    @EventBusSubscriber(modid = CommonClass.MODID, value = Dist.CLIENT)
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
                sendToServer(ChainBreakC2SPacket.INSTANCE);
            }
        }

        @SubscribeEvent
        public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
            if (ChainRaycastHelper.tryRemoveDecoration(event.getEntity(), event.getHand())) {
                sendToServer(DecorationRemoveC2SPacket.INSTANCE);
                //? if <=26.2
                event.getEntity().swing(event.getHand());
                //? if >26.2
                //event.getEntity().swing(event.getHand(), net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            }
        }

        //? if >=26.1 {
        @SubscribeEvent
        public static void onRightClickBlockEmpty(PlayerInteractEvent.RightClickBlock event) {
            if (ChainRaycastHelper.tryRemoveDecoration(event.getEntity(), event.getHand())) {
                sendToServer(DecorationRemoveC2SPacket.INSTANCE);
                //? if <=26.2
                event.getEntity().swing(event.getHand());
                //? if >26.2
                //event.getEntity().swing(event.getHand(), net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
        //?}
    }
}
*///?}
