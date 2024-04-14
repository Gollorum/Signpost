package gollorum.signpost.networking;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.Teleport;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.minecraft.gui.RequestWaystoneGui;
import gollorum.signpost.platform.Services;
import gollorum.signpost.utils.EventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public abstract class PacketHandler {

    public static PacketHandler getInstance() { return Services.PACKET_HANDLER; }

    public static interface Context {
        public static record Server(ServerPlayer sender) implements Context { }
        public static record Client() implements Context { }
    }

    private static final Event<?>[] EVENTS = new Event<?>[]{
        new PostTile.PartAddedEvent(),
        new PostTile.PartMutatedEvent(),
        new PostTile.PartRemovedEvent(),
        new PostTile.UpdateAllPartsEvent(),
        new Teleport.Request(),
        new Teleport.RequestGui(),
        new RequestSignGui(),
        new RequestSignGui.ForNewSign(),
        new RequestWaystoneGui(),
        new BlockRestrictions.NotifyCountChanged(),
    };

    private static final EventDispatcher.Impl.WithPublicDispatch<Unit> onInitialize = new EventDispatcher.Impl.WithPublicDispatch<>();
    public static EventDispatcher<Unit> onInitialize() { return onInitialize; }

    public void initialize(){
        for(Event<?> event : EVENTS){
            register(event);
        }
        onInitialize.dispatch(Unit.INSTANCE, false);
    }

    public abstract <T> void register(Event<T> event);

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
