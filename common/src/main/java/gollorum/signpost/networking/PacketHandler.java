package gollorum.signpost.networking;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.Signpost;
import gollorum.signpost.Teleport;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.minecraft.gui.RequestWaystoneGui;
import gollorum.signpost.platform.Services;
import gollorum.signpost.utils.EventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public abstract class PacketHandler {

    protected static PacketHandler instance = null;
    public static PacketHandler getInstance() { return instance; }

    public interface Context {
        interface FromClient extends Context {
            Player getSender();
        }
        interface Client extends Context { }

        record Server(ServerPlayer sender) implements Context, FromClient {
            @Override
            public Player getSender() {
                return sender;
            }
        }
        record ClientFromServer() implements Context, Client { }
        record ClientFromClient(Player sender) implements Context, Client, FromClient {
            @Override
            public Player getSender() {
                return sender;
            }
        }
    }

    private static final EventDispatcher.Impl.WithPublicDispatch<PacketHandler> onInitialize = new EventDispatcher.Impl.WithPublicDispatch<>();
    public static void onInitializeDo(EventDispatcher.Listener<PacketHandler> action) {
        if(instance == null) onInitialize.addListener(action);
        else action.accept(instance);
    }

    protected void init(){
        register(new PostTile.PartAddedEvent(), new ResourceLocation(Signpost.MOD_ID, "part_added"));
        register(new PostTile.PartMutatedEvent(), new ResourceLocation(Signpost.MOD_ID, "part_mutated"));
        register(new PostTile.PartRemovedEvent(), new ResourceLocation(Signpost.MOD_ID, "part_removed"));
        register(new PostTile.UpdateAllPartsEvent(), new ResourceLocation(Signpost.MOD_ID, "update_all_parts"));
        register(new Teleport.Request(), new ResourceLocation(Signpost.MOD_ID, "teleport_request"));
        register(new Teleport.RequestGui(), new ResourceLocation(Signpost.MOD_ID, "teleport_request_gui"));
        register(new RequestSignGui(), new ResourceLocation(Signpost.MOD_ID, "request_sign_gui"));
        register(new RequestSignGui.ForNewSign(), new ResourceLocation(Signpost.MOD_ID, "request_sign_gui_for_new_sign"));
        register(new RequestWaystoneGui(), new ResourceLocation(Signpost.MOD_ID, "request_waystone_gui"));
        register(new BlockRestrictions.NotifyCountChanged(), new ResourceLocation(Signpost.MOD_ID, "block_restrictions_notify_count_changed"));
        onInitialize.dispatch(this, true);
    }

    public abstract <T> void register(Event<T> event, ResourceLocation id);

    public abstract <T> void sendToServer(T message);

    public abstract <T> void sendToPlayer(ServerPlayer target, T message);

    public abstract <T> void sendToTracing(Level world, BlockPos pos, Supplier<T> t);

    public abstract <T> void sendToTracing(BlockEntity tile, Supplier<T> t);

    public abstract <T> void sendToAll(T message);

    public static interface Event<T> {
        Class<T> getMessageClass();
        void encode(T message, FriendlyByteBuf buffer);
        T decode(FriendlyByteBuf buffer);
        void handle(T message, Context context);

        interface FromClient<T> extends Event<T> {
            @Override
            default void handle(T message, Context context) {
                if(context instanceof Context.FromClient client){
                    handle(message, client);
                } else throw new RuntimeException("Tried to handle event originating from server as if it came from client");
            }
            void handle(T message, Context.FromClient context);
        }

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
