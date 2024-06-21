package gollorum.signpost;

import gollorum.signpost.compat.Compat;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.config.Config;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.rendering.PostRenderer;
import gollorum.signpost.minecraft.worldgen.JigsawDeserializers;
import gollorum.signpost.networking.FabricPacketHandler;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.registry.*;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.worldgen.Villages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class SignpostFabric implements ModInitializer {

    private final Consumer<MinecraftServer> serverSetter;

    public SignpostFabric() {
        serverSetter = Signpost.init(Config.INSTANCE, Delay.INSTANCE);
    }

    @Override
    public void onInitialize() {
        BlockRegistry.register();
        ItemRegistry.register();
        RecipeRegistry.register();
        TileEntityRegistry.register();
        CreativeModeTabRegistry.register();
        LootProviderRegistry.register();
        LootItemConditionRegistry.register();
        MiscRegistry.register();
        JigsawDeserializers.register();

        WaystoneDiscoveryEventListener.register();
        BlockEventListener.register();
        FabricPacketHandler.initialize();
        PacketHandler.onInitializeDo(e -> {
            PacketHandler.getInstance().register(new JoinServerEvent(), new ResourceLocation(Signpost.MOD_ID, "join_server"));
            return true;
        });

        Delay.INSTANCE.register();
        Config.INSTANCE.register();

        Compat.register();

        ExternalWaystoneLibrary.initialize();
        WaystoneLibrary.registerNetworkPackets();
//            if(ModList.get().isLoaded(Compat.AntiqueAtlasId))
//                AntiqueAtlasAdapter.registerNetworkPacket();

        var events = new FabricEvents();
        ServerLifecycleEvents.SERVER_STARTING.register(events::serverAboutToStart);
        ServerLifecycleEvents.SERVER_STOPPED.register(events::onServerStopped);
        ServerPlayConnectionEvents.JOIN.register(events::joinServer);
        ServerWorldEvents.LOAD.register(events::onWorldLoad);
    }

    @Environment(EnvType.CLIENT)
    private static class SignpostFabricClient implements ClientModInitializer {
        @Override
        public void onInitializeClient() {
            BlockEntityRenderers.register(PostTile.getBlockEntityType(), PostRenderer::new);
        }
    }

    private class FabricEvents {

        public void serverAboutToStart(MinecraftServer server) {
            serverSetter.accept(server);
            WaystoneLibrary.initialize();
            BlockRestrictions.initialize();
            Villages.reset();
//            VillageRegistry.register(e);
            Villages.instance.initialize(server.registryAccess());
            new WaystoneDiscoveryEventListener().initialize();
        }

        public void joinServer(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
            if(Signpost.getServerInstance().isDedicatedServer())
                PacketHandler.getInstance().sendToPlayer(
                    handler.getPlayer(),
                    new JoinServerEvent.Package()
                );
        }

        public void onServerStopped(MinecraftServer server) {
            serverSetter.accept(null);
        }

        public void onWorldLoad(MinecraftServer server, ServerLevel world) {
            if(world.dimension().equals(Level.OVERWORLD)) {
                if(!WaystoneLibrary.getInstance().hasStorageBeenSetup())
                    WaystoneLibrary.getInstance().setupStorage(world);
                if(!BlockRestrictions.getInstance().hasStorageBeenSetup())
                    BlockRestrictions.getInstance().setupStorage(world);
            }
        }
    }

    private static final class JoinServerEvent implements PacketHandler.Event<JoinServerEvent.Package> {

        public static final class Package {}

        @Override
        public Class<Package> getMessageClass() { return Package.class; }

        @Override
        public void encode(Package message, FriendlyByteBuf buffer) { }

        @Override
        public Package decode(FriendlyByteBuf buffer) { return new Package(); }

        @Override
        public void handle(
            Package message, PacketHandler.Context context
        ) {
            WaystoneLibrary.initialize();
        }

    }

}
