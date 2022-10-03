package gollorum.signpost;

import gollorum.signpost.blockpartdata.types.renderers.BlockPartWaystoneUpdateListener;
import gollorum.signpost.compat.AntiqueAtlasAdapter;
import gollorum.signpost.compat.WaystonesAdapter;
import gollorum.signpost.minecraft.block.BlockEventListener;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.commands.WaystoneArgument;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.data.DataGeneration;
import gollorum.signpost.minecraft.registry.*;
import gollorum.signpost.minecraft.rendering.PostRenderer;
import gollorum.signpost.minecraft.worldgen.JigsawDeserializers;
import gollorum.signpost.minecraft.worldgen.WaystoneDiscoveryEventListener;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.utils.EventDispatcher;
import gollorum.signpost.utils.ServerType;
import gollorum.signpost.worldgen.Villages;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Signpost.MOD_ID)
public class Signpost {

    public static final String MOD_ID = "signpost";
    public static final Logger LOGGER = LogManager.getLogger();

    private static MinecraftServer serverInstance;
    public static MinecraftServer getServerInstance() { return serverInstance; }

    public static ServerType getServerType() {
        return serverInstance == null
            ? ServerType.ConnectedClient
            : serverInstance.isDedicatedServer()
                ? ServerType.Dedicated
                : ServerType.HostingClient;
    }


    private static final EventDispatcher.Impl.WithPublicDispatch<ServerLevel> _onServerOverworldLoad = new EventDispatcher.Impl.WithPublicDispatch<>();
    public static final EventDispatcher<ServerLevel> onServerOverworldLoad = _onServerOverworldLoad;

    public Signpost() {

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        forgeBus.register(new ForgeEvents());
        modBus.register(new ModBusEvents());

        BlockRegistry.register(modBus);
        ItemRegistry.register(modBus);
        RecipeRegistry.register(modBus);
        TileEntityRegistry.register(modBus);
        DataGeneration.register(modBus);
        BlockEventListener.register(forgeBus);
        WaystoneDiscoveryEventListener.register(forgeBus);
        Config.register();

        NbtProviderRegistry.register(modBus);
        LootItemConditionRegistry.register(modBus);

        Villages.instance.initialize();
        BlockPartWaystoneUpdateListener.initialize();

        WaystoneArgument.bootstrap();

        if(ModList.get().isLoaded("waystones"))
            WaystonesAdapter.register();

        if(ModList.get().isLoaded("antiqueatlas"))
            AntiqueAtlasAdapter.register();

    }

    private static class ModBusEvents {

        @SubscribeEvent
        public void setup(final FMLCommonSetupEvent event) {
            PacketHandler.initialize();
            PacketHandler.register(new JoinServerEvent(), -50);
            ExternalWaystoneLibrary.initialize();
            WaystoneLibrary.registerNetworkPackets();
            JigsawDeserializers.register();
            if(ModList.get().isLoaded("antiqueatlas"))
                AntiqueAtlasAdapter.registerNetworkPacket();
        }

        @SubscribeEvent
        public void doClientStuff(final FMLClientSetupEvent event) {
            BlockEntityRenderers.register(PostTile.getBlockEntityType(), PostRenderer::new);
        }

    }

    private static class ForgeEvents {

        @SubscribeEvent
        public void serverAboutToStart(ServerAboutToStartEvent e) {
            serverInstance = e.getServer();
            WaystoneLibrary.initialize();
            BlockRestrictions.initialize();
            Villages.instance.reset();
        }

        @SubscribeEvent
        public void joinServer(PlayerEvent.PlayerLoggedInEvent e) {
            if(!e.getPlayer().level.isClientSide && serverInstance.isDedicatedServer())
                PacketHandler.send(
                    PacketDistributor.PLAYER.with((() -> (ServerPlayer) e.getPlayer())),
                    new JoinServerEvent.Package()
                );
        }

        @SubscribeEvent
        public void onServerStopped(ServerStoppedEvent event) {
            serverInstance = null;
        }

        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load event) {
            if (event.getWorld() instanceof ServerLevel world &&
                ((ServerLevel) event.getWorld()).dimension().equals(Level.OVERWORLD)) {
                if(!WaystoneLibrary.getInstance().hasStorageBeenSetup())
                    WaystoneLibrary.getInstance().setupStorage(world);
                if(!BlockRestrictions.getInstance().hasStorageBeenSetup())
                    BlockRestrictions.getInstance().setupStorage(world);
                _onServerOverworldLoad.dispatch(world, false);
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
            Package message, NetworkEvent.Context context
        ) {
            WaystoneLibrary.initialize();
        }

    }

}
