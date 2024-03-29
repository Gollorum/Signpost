package gollorum.signpost.networking;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.Signpost;
import gollorum.signpost.Teleport;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.minecraft.gui.RequestWaystoneGui;
import gollorum.signpost.utils.EventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketHandler {

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

    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel channel;

    private static final EventDispatcher.Impl.WithPublicDispatch<Unit> onInitialize = new EventDispatcher.Impl.WithPublicDispatch<>();
    public static EventDispatcher<Unit> onInitialize() { return onInitialize; }

    public static void initialize(){
        channel = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Signpost.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );
        int id = 0;
        for(Event<?> event : EVENTS){
            register(event, id++);
        }
        onInitialize.dispatch(Unit.INSTANCE, false);
    }

    public static <T> void register(Event<T> event, int id){
        register(event.getMessageClass(), event::encode, event::decode, event::handle, id);
    }

    public static <T> void register(
        Class<T> messageClass,
        BiConsumer<T, FriendlyByteBuf> encode,
        Function<FriendlyByteBuf, T> decode,
        BiConsumer<T, Supplier<NetworkEvent.Context>> handle,
        int id
    ){
        channel.registerMessage(id, messageClass, encode, decode, handle);
    }

    public static <T> void sendToServer(T message) {
        channel.sendToServer(message);
    }

    public static <T> void sendTo(T message, Connection manager, NetworkDirection direction) {
        channel.sendTo(message, manager, direction);
    }

    public static <T> void send(PacketDistributor.PacketTarget target, T message) {
        channel.send(target, message);
    }

    public static <T> void sendToTracing(Level world, BlockPos pos, Supplier<T> t) {
        if(world == null) Signpost.LOGGER.warn("No world to notify mutation");
        else if(pos == null) Signpost.LOGGER.warn("No position to notify mutation");
        else PacketHandler.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(pos)), t.get());
    }

    public static <T> void sendToTracing(BlockEntity tile, Supplier<T> t) {
        sendToTracing(tile.getLevel(), tile.getBlockPos(), t);
    }

    public static <T> void sendToAll(T message) {
        channel.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <T> void reply(T message, NetworkEvent.Context context) {
        channel.reply(message, context);
    }

    public static interface Event<T> {
        Class<T> getMessageClass();
        void encode(T message, FriendlyByteBuf buffer);
        T decode(FriendlyByteBuf buffer);
        void handle(T message, NetworkEvent.Context context);

        default void handle(T message, Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context c = context.get();
            c.enqueueWork(() -> {
                if(c.getDirection().getReceptionSide().isClient())
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handle(message, c));
                else handle(message, c);
            });
            c.setPacketHandled(true);
        }
    }
}
