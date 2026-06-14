package com.evandev.connectiblechains.client;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.evandev.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.evandev.connectiblechains.entity.ModEntityTypes;
import com.evandev.connectiblechains.item.ChainItemCallbacks;
import com.evandev.connectiblechains.networking.packet.BannerSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.BuntingSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.evandev.connectiblechains.networking.packet.ChainSlackSyncS2CPacket;
import com.evandev.connectiblechains.networking.packet.ConfigSyncPayload;
import com.evandev.connectiblechains.networking.packet.HangingSyncS2CPacket;
import com.evandev.connectiblechains.platform.FabricClientNetworkHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ConnectibleChainsModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        new ClientInitializer().onInitializeClient();

        ItemTooltipCallback.EVENT.register(ChainItemCallbacks::infoToolTip);

        FabricClientNetworkHelper.registerClientReceiver(ChainAttachS2CPacket.class);
        FabricClientNetworkHelper.registerClientReceiver(ConfigSyncPayload.class);
        FabricClientNetworkHelper.registerClientReceiver(ChainSlackSyncS2CPacket.class);
        FabricClientNetworkHelper.registerClientReceiver(BuntingSyncS2CPacket.class);
        FabricClientNetworkHelper.registerClientReceiver(BannerSyncS2CPacket.class);
        FabricClientNetworkHelper.registerClientReceiver(HangingSyncS2CPacket.class);

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(CommonClass.MODID, "chain_textures");
            }

            @Override
            public @NotNull CompletableFuture<Void> reload(PreparableReloadListener.@NotNull PreparationBarrier synchronizer, @NotNull ResourceManager manager, @NotNull ProfilerFiller prepareProfiler, @NotNull ProfilerFiller applyProfiler, @NotNull Executor prepareExecutor, @NotNull Executor applyExecutor) {
                return ClientInitializer.getInstance().getChainTextureManager().reload(synchronizer, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor);
            }
        });

        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            CommonClass.runtimeConfig.copyFrom(CommonClass.fileConfig);

            ClientInitializer.getInstance().getChainKnotEntityRenderer()
                    .ifPresent(r -> r.getChainRenderer().purge());
        });

        EntityRendererRegistry.register(ModEntityTypes.CHAIN_KNOT.get(), ctx -> {
            ChainKnotEntityRenderer renderer = new ChainKnotEntityRenderer(ctx);
            ClientInitializer.getInstance().setChainKnotEntityRenderer(renderer);
            return renderer;
        });
        EntityRendererRegistry.register(ModEntityTypes.CHAIN_COLLISION.get(), ChainCollisionEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(ClientInitializer.CHAIN_KNOT, ClientInitializer::getChainKnotLayerDefinition);
    }
}