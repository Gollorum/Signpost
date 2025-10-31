package gollorum.signpost;

import gollorum.signpost.compat.Compat;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.config.Config;
import gollorum.signpost.minecraft.loot.LootEntries;
import gollorum.signpost.networking.ForgePacketHandler;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.rendering.PostRenderer;
import gollorum.signpost.minecraft.worldgen.JigsawDeserializers;
import gollorum.signpost.registry.WaystoneDiscoveryEventListener;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.registry.*;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.worldgen.Villages;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

@Mod(Signpost.MOD_ID)
public class SignpostForge {

    private final Consumer<MinecraftServer> serverSetter;
    
    public SignpostForge(FMLJavaModLoadingContext context) {
        serverSetter = Signpost.init(Config.INSTANCE, Delay.INSTANCE);

        var forgeBus = BusGroup.DEFAULT;
        var modBus = context.getModBusGroup();
        ModBusEvents.register(modBus);
        forgeBus.register(MethodHandles.lookup(), new ForgeEvents());

        BlockRegistry.register(modBus);
        ItemRegistry.register(modBus);
        DataComponentsRegistry.register(modBus);
        RecipeRegistry.register(modBus);
        TileEntityRegistry.register(modBus);
        CreativeModeTabRegistry.register(modBus);
        WaystoneDiscoveryEventListener.register(forgeBus);

        forgeBus.register(MethodHandles.lookup(), Delay.INSTANCE);

        Config.INSTANCE.register(context);

        LootItemConditionRegistry.register(modBus);

        MiscRegistry.register(modBus);

        Compat.register();
    }

    private static class ModBusEvents {

        public static void register(BusGroup busGroup) {
            FMLCommonSetupEvent.getBus(busGroup).addListener(ModBusEvents::setup);
            EntityRenderersEvent.RegisterRenderers.BUS.addListener(ModBusEvents::registerEntityRenderers);
            RegisterEvent.getBus(busGroup).addListener(ModBusEvents::registerStuff);
        }

        public static void setup(final FMLCommonSetupEvent event) {
            ForgePacketHandler.initialize();
            ExternalWaystoneLibrary.initialize();
//            if(ModList.get().isLoaded(Compat.AntiqueAtlasId))
//                AntiqueAtlasAdapter.registerNetworkPacket();
        }

        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(PostTile.getBlockEntityType(), PostRenderer::new);
        }

        public static void registerStuff(RegisterEvent event) {
            JigsawDeserializers.register((loc, elem) -> event.register(Registries.STRUCTURE_POOL_ELEMENT, loc, () -> elem));
            LootEntries.register((loc, elem) -> event.register(Registries.LOOT_POOL_ENTRY_TYPE, loc, () -> elem));
        }

    }

    private class ForgeEvents {

        @SubscribeEvent
        public void serverAboutToStart(ServerAboutToStartEvent e) {
            serverSetter.accept(e.getServer());
//            VillageRegistry.register(e);
            Villages.instance.initialize(e.getServer().registryAccess());
            new WaystoneDiscoveryEventListener().initialize();
        }

        @SubscribeEvent
        public void joinServer(PlayerEvent.PlayerLoggedInEvent e) {
            if(!e.getEntity().level().isClientSide() && Signpost.getServerInstance().isDedicatedServer())
                PacketHandler.getInstance().sendToPlayer(
                    (ServerPlayer) e.getEntity(),
                    JoinServerEvent.Package.INSTANCE
                );
        }

        @SubscribeEvent
        public void onWorldLoad(LevelEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel world &&
                ((ServerLevel) event.getLevel()).dimension().equals(Level.OVERWORLD)) {
                WaystoneLibrary.initializeServer(world);
            }
        }

        @SubscribeEvent
        public void onServerStopped(ServerStoppedEvent event) {
            serverSetter.accept(null);
        }

    }

    public static final class JoinServerEvent implements PacketHandler.Event<JoinServerEvent.Package> {

        public static final class Package {

            public static final Package INSTANCE = new Package();

            public static final StreamCodec<RegistryFriendlyByteBuf, Package> CODEC = StreamCodec.unit(INSTANCE);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Package> codec() {
            return Package.CODEC;
        }

        @Override
        public Class<Package> getMessageClass() { return Package.class; }

        @Override
        public void handle(
            Package message, PacketHandler.Context context
        ) {
            WaystoneLibrary.initializeClient();
        }
    }

}
