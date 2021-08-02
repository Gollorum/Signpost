package gollorum.signpost;

import gollorum.signpost.minecraft.block.BlockEventListener;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.data.DataGeneration;
import gollorum.signpost.minecraft.registry.BlockRegistry;
import gollorum.signpost.minecraft.registry.ItemRegistry;
import gollorum.signpost.minecraft.registry.RecipeRegistry;
import gollorum.signpost.minecraft.registry.TileEntityRegistry;
import gollorum.signpost.minecraft.rendering.PostRenderer;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.ServerType;
import gollorum.signpost.worldgen.Villages;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Dimension;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

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

    public Signpost() {
        Config.register();

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

        Villages.instance.initialize();
    }

    private static class ModBusEvents {

        @SubscribeEvent
        public void setup(final FMLCommonSetupEvent event) {
            PacketHandler.initialize();
            PacketHandler.register(new JoinServerEvent());
            WaystoneLibrary.registerNetworkPackets();
        }

        @SubscribeEvent
        public void doClientStuff(final FMLClientSetupEvent event) {
            ClientRegistry.bindTileEntityRenderer(PostTile.type, PostRenderer::new);
        }

    }

    private static class ForgeEvents {

        @SubscribeEvent
        public void serverAboutToStart(FMLServerAboutToStartEvent e) {
            serverInstance = e.getServer();
            WaystoneLibrary.initialize();
            BlockRestrictions.initialize();
            Villages.instance.reset();
        }

        @SubscribeEvent
        public void joinServer(PlayerEvent.PlayerLoggedInEvent e) {
            if(!e.getPlayer().world.isRemote && serverInstance.isDedicatedServer())
                PacketHandler.send(
                    PacketDistributor.PLAYER.with((() -> (ServerPlayerEntity) e.getPlayer())),
                    new JoinServerEvent.Package()
                );
        }

        @SubscribeEvent
        public void onServerStopped(FMLServerStoppedEvent event) {
            serverInstance = null;
        }

        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load event) {
            if (event.getWorld() instanceof ServerWorld &&
                ((ServerWorld) event.getWorld()).getDimensionKey().equals(Dimension.OVERWORLD)) {
                ServerWorld world = (ServerWorld) event.getWorld();
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
        public void encode(Package message, PacketBuffer buffer) { }

        @Override
        public Package decode(PacketBuffer buffer) { return new Package(); }

        @Override
        public void handle(
            Package message, Supplier<NetworkEvent.Context> context
        ) {
            WaystoneLibrary.initialize();
        }

    }

}
