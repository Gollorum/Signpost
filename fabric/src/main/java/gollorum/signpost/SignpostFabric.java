package gollorum.signpost;

import gollorum.signpost.compat.Compat;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.config.Config;
import gollorum.signpost.minecraft.loot.LootEntries;
import gollorum.signpost.minecraft.worldgen.JigsawDeserializers;
import gollorum.signpost.networking.FabricPacketHandler;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.registry.*;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.worldgen.Villages;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class SignpostFabric implements ModInitializer {

    private final Consumer<MinecraftServer> serverSetter;

    public SignpostFabric() {
        AutoConfig.register(Config.class, GsonConfigSerializer::new);
        serverSetter = Signpost.init(Config::get, Delay.INSTANCE);
    }

    @Override
    public void onInitialize() {
        BlockRegistry.register();
        ItemRegistry.register();
        DataComponentsRegistry.register();
        RecipeRegistry.register();
        TileEntityRegistry.register();
        CreativeModeTabRegistry.register();
        WaystoneDiscoveryEventListener.register();

        PacketHandler.onInitializeDo(e -> {
            PacketHandler.getInstance().register(new JoinServerEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "join_server"));
            return true;
        });

        Delay.INSTANCE.register();

        LootProviderRegistry.register();
        LootItemConditionRegistryImpl.register();

        JigsawDeserializers.register((loc, elem) ->
            Registry.register(BuiltInRegistries.STRUCTURE_POOL_ELEMENT, loc, elem));
        LootEntries.register((loc, elem) ->
            Registry.register(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE, loc, elem));

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
        CommandRegistry.register();
        ArgumentTypeInfosInjector.register();
    }

    private class FabricEvents {

        public void serverAboutToStart(MinecraftServer server) {
            serverSetter.accept(server);
//            VillageRegistry.register(e);
            Villages.instance.initialize(server.registryAccess());
            new WaystoneDiscoveryEventListener().initialize();
        }

        public void joinServer(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
            if(Signpost.getServerInstance().isDedicatedServer())
                PacketHandler.getInstance().sendToPlayer(
                    handler.getPlayer(),
                    JoinServerEvent.Package.INSTANCE
                );
        }

        public void onServerStopped(MinecraftServer server) {
            serverSetter.accept(null);
        }

        public void onWorldLoad(MinecraftServer server, ServerLevel world) {
            if(world.dimension().equals(Level.OVERWORLD)) {
                WaystoneLibrary.initializeServer(world);
            }
        }
    }

    private static final class JoinServerEvent implements PacketHandler.Event<JoinServerEvent.Package> {

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
