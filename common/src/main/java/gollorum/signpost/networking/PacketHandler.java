package gollorum.signpost.networking;

import gollorum.signpost.Signpost;
import gollorum.signpost.Teleport;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.minecraft.gui.RequestWaystoneGui;
import gollorum.signpost.utils.EventDispatcher;
import gollorum.signpost.utils.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;
import java.util.function.Supplier;

public abstract class PacketHandler {

    protected static PacketHandler instance = null;
    public static PacketHandler getInstance() { return instance; }

    public interface Context {
        HolderLookup.Provider getHolderLookupProvider();

        record Server(ServerPlayer sender) implements Context {
            @Override
            public HolderLookup.Provider getHolderLookupProvider(){
                return sender.registryAccess();
            }
        }
        record Client() implements Context {
            @Override
            public HolderLookup.Provider getHolderLookupProvider(){
                return net.minecraft.client.Minecraft.getInstance().player.registryAccess();
            }
        }
    }

    protected final List<Tuple<Event<?>, ResourceLocation>> events = new ArrayList<>();
    protected final Map<Class<?>, Tuple<Event<?>, ResourceLocation>> eventMap = new HashMap<>();

    protected void init() {
        register(new PostTile.PartAddedEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "part_added"));
        register(new PostTile.PartMutatedEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "part_mutated"));
        register(new PostTile.PartRemovedEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "part_removed"));
        register(new PostTile.UpdateAllPartsEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "update_all_parts"));
        register(new Teleport.Request(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "teleport_request"));
        register(new Teleport.RequestGui(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "teleport_request_gui"));
        register(new RequestSignGui(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_sign_gui"));
        register(new RequestSignGui.ForNewSign(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_sign_gui_for_new_sign"));
        register(new RequestWaystoneGui(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_waystone_gui"));
        register(new WaystoneLibrary.RequestAllWaystoneNamesEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_all_waystone_names"));
        register(new WaystoneLibrary.DeliverAllWaystoneNamesEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "deliver_all_waystone_names"));
        register(new WaystoneLibrary.RequestAllWaystonesEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_all_waystones"));
        register(new WaystoneLibrary.DeliverAllWaystonesEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "deliver_all_waystones"));
        register(new WaystoneLibrary.WaystoneUpdatedEventEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "waystone_updated_event"));
        register(new WaystoneLibrary.RequestWaystoneLocationEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_waystone_location"));
        register(new WaystoneLibrary.DeliverWaystoneLocationEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "deliver_waystone_location"));
        register(new WaystoneLibrary.RequestWaystoneAtLocationEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_waystone_at_location"));
        register(new WaystoneLibrary.DeliverWaystoneAtLocationEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "deliver_waystone_at_location"));
        register(new WaystoneLibrary.DeliverIdEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "deliver_id"));
        register(new WaystoneLibrary.RequestIdEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_id"));
    }

    public final <T> void register(Event<T> event, ResourceLocation id) {
        events.add(new Tuple<>(event, id));
        eventMap.put(event.getMessageClass(), new Tuple<>(event, id));
    }

    public abstract <T> void sendToServer(T message);

    public abstract <T> void sendToPlayer(ServerPlayer target, T message);

    public abstract <T> void sendToTracing(ServerLevel world, BlockPos pos, Supplier<T> t);

    public abstract <T> void sendToTracing(BlockEntity tile, Supplier<T> t);

    public abstract <T> void sendToAll(T message);

    public static interface Event<T> {
        Class<T> getMessageClass();
        void handle(T message, Context context);

        StreamCodec<RegistryFriendlyByteBuf, T> codec();

        interface ForClient<T> extends Event<T> {
            @Override
            default void handle(T message, Context context) {
                if(context instanceof Context.Client client){
                    handle(message, client);
                } else throw new RuntimeException("Tried to handle client event on server");
            }
            void handle(T message, Context.Client context);
        }

        interface ForServer<T> extends Event<T> {
            @Override
            default void handle(T message, Context context) {
                if(context instanceof Context.Server server){
                    handle(message, server);
                } else throw new RuntimeException("Tried to handle server event on client");
            }
            void handle(T message, Context.Server context);
        }
    }
}
