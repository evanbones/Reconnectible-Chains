package com.evandev.connectiblechains;

import com.evandev.connectiblechains.client.ClientConfigSetup;
import com.evandev.connectiblechains.client.ClientInitializer;
import com.evandev.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.evandev.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.evandev.connectiblechains.entity.ModEntityTypes;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.ChainBreakC2SPacket;
import com.evandev.connectiblechains.platform.ForgeRegistryHelper;
import com.evandev.connectiblechains.platform.ForgeNetworkHelper;
import com.evandev.connectiblechains.util.ChainRaycastHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CommonClass.MODID)
public class ConnectibleChainsMod {
    public ConnectibleChainsMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgeRegistryHelper.ENTITIES.register(modBus);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientConfigSetup.register(ModLoadingContext.get().getActiveContainer())
        );

        CommonClass.init();

        modBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onRightClickItem);
        MinecraftForge.EVENT_BUS.addListener(this::onRightClickEmpty);
    }

    private void setup(final FMLCommonSetupEvent event) {
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

    private void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (ChainRaycastHelper.tryAdjustSlack(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @Mod.EventBusSubscriber(modid = CommonClass.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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

    @Mod.EventBusSubscriber(modid = CommonClass.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ChainItemCallbacks.infoToolTip(event.getItemStack(), event.getFlags(), event.getToolTip());
        }

        @SubscribeEvent
        public static void onClientJoin(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
            CommonClass.runtimeConfig.copyFrom(CommonClass.fileConfig);
            if (ClientInitializer.getInstance() != null) {
                ClientInitializer.getInstance().getChainKnotEntityRenderer()
                        .ifPresent(r -> r.getChainRenderer().purge());
            }
        }

        @SubscribeEvent
        public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
            if (ChainRaycastHelper.tryBreakChain(event.getEntity())) {
                ForgeNetworkHelper.CHANNEL.sendToServer(new ChainBreakC2SPacket());
            }
        }
    }
}