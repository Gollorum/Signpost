package gollorum.signpost;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.registry.BlockRegistry;
import gollorum.signpost.minecraft.registry.ItemRegistry;
import gollorum.signpost.minecraft.registry.TileEntityRegistry;
import gollorum.signpost.minecraft.rendering.PostRenderer;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.ServerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    public Signpost() {
        MinecraftForge.EVENT_BUS.register(this);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.register(this);
        BlockRegistry.register(modBus);
        ItemRegistry.register(modBus);
        TileEntityRegistry.register(modBus);
        Delay.register(modBus);
    }

    @SubscribeEvent
    public void setup(final FMLCommonSetupEvent event) {
        PacketHandler.initialize();
    }

    @SubscribeEvent
    public void serverAboutToStart(FMLServerAboutToStartEvent e) {
        serverInstance = e.getServer();
        WaystoneLibrary.initialize();
    }

    @SubscribeEvent
    public void doClientStuff(final FMLClientSetupEvent event) {
        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().gameSettings);
        ClientRegistry.bindTileEntityRenderer(PostTile.type, PostRenderer::new);
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) { }

    @SubscribeEvent
    public void onServerStopped(FMLServerStoppedEvent event) {
        serverInstance = null;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if(event.getWorld() instanceof ServerWorld &&
            event.getWorld().getDimension().getType().equals(DimensionType.OVERWORLD) &&
            !WaystoneLibrary.getInstance().hasStorageBeenSetup()
        ) WaystoneLibrary.getInstance().setupStorage((ServerWorld) event.getWorld());
    }

}
