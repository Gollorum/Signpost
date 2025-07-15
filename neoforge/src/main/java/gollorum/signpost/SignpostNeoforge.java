package gollorum.signpost;


import gollorum.signpost.compat.Compat;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.config.Config;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.rendering.PostRenderer;
import gollorum.signpost.networking.NeoForgePacketHandler;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.registry.*;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.worldgen.Villages;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.function.Consumer;

@Mod(Signpost.MOD_ID)
public class SignpostNeoforge {

    private final Consumer<MinecraftServer> serverSetter;

    public SignpostNeoforge(IEventBus modBus) {
        serverSetter = Signpost.init(Config.INSTANCE, Delay.INSTANCE);

        IEventBus forgeBus = NeoForge.EVENT_BUS;
        forgeBus.register(new ForgeEvents());
        modBus.register(new ModBusEvents());

        BlockRegistry.register(modBus);
        ItemRegistry.register(modBus);
        DataComponentsRegistry.register(modBus);
        RecipeRegistry.register(modBus);
        TileEntityRegistry.register(modBus);
        BlockEventListener.register(forgeBus);
        CreativeModeTabRegistry.register(modBus);
        WaystoneDiscoveryEventListener.register(forgeBus);

        NeoForgePacketHandler.initialize(modBus);
        PacketHandler.onInitializeDo(e -> {
            PacketHandler.getInstance().register(new JoinServerEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "join_server"));
            return true;
        });

        forgeBus.register(Delay.INSTANCE);

        Config.INSTANCE.register();

        LootProviderRegistry.register(modBus);
        LootItemConditionRegistry.register(modBus);

        MiscRegistry.register(modBus);

        Compat.register();
        JigsawDeserializerRegistry.register(modBus);
    }

    private static class ModBusEvents {

        @SubscribeEvent
        public void setup(final FMLCommonSetupEvent event) {
            ExternalWaystoneLibrary.initialize();
            WaystoneLibrary.registerNetworkPackets();
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
//            VillageRegistry.register(e);
            Villages.instance.initialize(e.getServer().registryAccess());
            new WaystoneDiscoveryEventListener().initialize();
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
        public void encode(RegistryFriendlyByteBuf buffer, Package message) { }

        @Override
        public Package decode(RegistryFriendlyByteBuf buffer) { return new Package(); }

        @Override
        public void handle(
            Package message, PacketHandler.Context context
        ) {
            WaystoneLibrary.initialize();
        }

    }

}
