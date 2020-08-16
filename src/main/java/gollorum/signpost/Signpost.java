package gollorum.signpost;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.registry.BlockRegistry;
import gollorum.signpost.minecraft.registry.ItemRegistry;
import gollorum.signpost.minecraft.registry.TileEntityRegistry;
import gollorum.signpost.minecraft.rendering.PostRenderer;
import gollorum.signpost.networking.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Signpost.MOD_ID)
public class Signpost {
    public static final String MOD_ID = "signpost";
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public Signpost() {
        MinecraftForge.EVENT_BUS.register(this);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.register(this);
        BlockRegistry.register(modBus);
        ItemRegistry.register(modBus);
        TileEntityRegistry.register(modBus);
    }

    @SubscribeEvent
    public void setup(final FMLCommonSetupEvent event) {
        PacketHandler.initialize();
    }

    @SubscribeEvent
    public void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().gameSettings);
        ClientRegistry.bindTileEntityRenderer(PostTile.type, PostRenderer::new);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {}

}
