package gollorum.signpost.networking;

import gollorum.signpost.Signpost;
import gollorum.signpost.Teleport;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketHandler {

    private static final Event<?>[] EVENTS = new Event<?>[]{
        new PostTile.PartAddedEvent(),
        new PostTile.PartMutatedEvent(),
        new PostTile.PartRemovedEvent(),
        new Teleport.Request(),
    };

    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel channel;

    private static int id;

    public static void initialize(){
        channel = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Signpost.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );
        for(Event<?> event : EVENTS){
            register(event);
        }
    }

    public static <T> void register(Event<T> event){
        register(event.getMessageClass(), event::encode, event::decode, event::handle);
    }

    public static <T> void register(
        Class<T> messageClass,
        BiConsumer<T, PacketBuffer> encode,
        Function<PacketBuffer, T> decode,
        BiConsumer<T, Supplier<NetworkEvent.Context>> handle
    ){
        channel.registerMessage(id++, messageClass, encode, decode, handle);
    }

    public static <T> void sendToServer(T message) {
        channel.sendToServer(message);
    }

    public static <T> void sendTo(T message, NetworkManager manager, NetworkDirection direction) {
        channel.sendTo(message, manager, direction);
    }

    public static <T> void send(PacketDistributor.PacketTarget target, T message) {
        channel.send(target, message);
    }

    public static <T> void sendToTracing(World world, BlockPos pos, Supplier<T> t) {
        if(world == null) Signpost.LOGGER.warn("No world to notify mutation");
        else if(pos == null) Signpost.LOGGER.warn("No position to notify mutation");
        else PacketHandler.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(pos)), t.get());
    }

    public static <T> void sendToTracing(TileEntity tile, Supplier<T> t) {
        sendToTracing(tile.getWorld(), tile.getPos(), t);
    }

    public static <T> void sendToAll(T message) {
        channel.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <T> void reply(T message, NetworkEvent.Context context) {
        channel.reply(message, context);
    }

    public static interface Event<T> {
        Class<T> getMessageClass();
        void encode(T message, PacketBuffer buffer);
        T decode(PacketBuffer buffer);
        void handle(T message, Supplier<NetworkEvent.Context> context);
    }
}
