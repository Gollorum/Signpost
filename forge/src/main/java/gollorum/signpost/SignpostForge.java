package gollorum.signpost;

import gollorum.signpost.compat.Compat;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.config.Config;
import gollorum.signpost.registry.BlockEventListener;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.data.DataGeneration;
import gollorum.signpost.minecraft.rendering.PostRenderer;
import gollorum.signpost.minecraft.worldgen.JigsawDeserializers;
import gollorum.signpost.registry.WaystoneDiscoveryEventListener;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.registry.*;
import gollorum.signpost.worldgen.Villages;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Consumer;

@Mod(Signpost.MOD_ID)
public class SignpostForge {

    private final Consumer<MinecraftServer> serverSetter;
    
    public SignpostForge() {
        serverSetter = Signpost.init();

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
        CreativeModeTabRegistry.register(modBus);
        WaystoneDiscoveryEventListener.register(forgeBus);
        Config.INSTANCE.register();

        LootProviderRegistry.register(modBus);
        LootItemConditionRegistry.register(modBus);

        MiscRegistry.register(modBus);

        Compat.register();
    }
    private static class ModBusEvents {

        @SubscribeEvent
        public void setup(final FMLCommonSetupEvent event) {
            PacketHandler.getInstance().initialize();
            PacketHandler.getInstance().register(new JoinServerEvent());
            ExternalWaystoneLibrary.initialize();
            WaystoneLibrary.registerNetworkPackets();
            JigsawDeserializers.register();
//            if(ModList.get().isLoaded(Compat.AntiqueAtlasId))
//                AntiqueAtlasAdapter.registerNetworkPacket();
        }

        @SubscribeEvent
        public void doClientStuff(final FMLClientSetupEvent event) {
            BlockEntityRenderers.register(PostTile.getBlockEntityType(), PostRenderer::new);
        }

    }

    private class ForgeEvents {

        @SubscribeEvent
        public void serverAboutToStart(ServerAboutToStartEvent e) {
            serverSetter.accept(e.getServer());
            WaystoneLibrary.initialize();
            BlockRestrictions.initialize();
            Villages.instance.initialize(e.getServer().registryAccess());
            WaystoneDiscoveryEventListener.initialize();
        }

        @SubscribeEvent
        public void joinServer(PlayerEvent.PlayerLoggedInEvent e) {
            if(!e.getEntity().level().isClientSide && Signpost.getServerInstance().isDedicatedServer())
                PacketHandler.getInstance().sendToPlayer(
                    (ServerPlayer) e.getEntity(),
                    new JoinServerEvent.Package()
                );
        }

        @SubscribeEvent
        public void onServerStopped(ServerStoppedEvent event) {
            serverSetter.accept(null);
        }

        @SubscribeEvent
        public void onWorldLoad(LevelEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel world &&
                ((ServerLevel) event.getLevel()).dimension().equals(Level.OVERWORLD)) {
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
