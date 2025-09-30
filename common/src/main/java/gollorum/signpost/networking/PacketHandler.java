package gollorum.signpost.networking;

import gollorum.signpost.Signpost;
import gollorum.signpost.Teleport;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.minecraft.gui.RequestWaystoneGui;
import gollorum.signpost.utils.EventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

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

    private boolean hasBeenInitialized = false;

    private static final EventDispatcher.Impl.WithPublicDispatch<PacketHandler> onInitialize = new EventDispatcher.Impl.WithPublicDispatch<>();
    public static void onInitializeDo(EventDispatcher.Listener<PacketHandler> action) {
        if (instance == null || !instance.hasBeenInitialized) onInitialize.addListener(action);
        else action.accept(instance);
    }

    protected void init(){
        register(new PostTile.PartAddedEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "part_added"));
        register(new PostTile.PartMutatedEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "part_mutated"));
        register(new PostTile.PartRemovedEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "part_removed"));
        register(new PostTile.UpdateAllPartsEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "update_all_parts"));
        register(new Teleport.Request(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "teleport_request"));
        register(new Teleport.RequestGui(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "teleport_request_gui"));
        register(new RequestSignGui(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_sign_gui"));
        register(new RequestSignGui.ForNewSign(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_sign_gui_for_new_sign"));
        register(new RequestWaystoneGui(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "request_waystone_gui"));
        hasBeenInitialized = true;
        onInitialize.dispatch(this, true);
    }

    public abstract <T> void register(Event<T> event, ResourceLocation id);

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
