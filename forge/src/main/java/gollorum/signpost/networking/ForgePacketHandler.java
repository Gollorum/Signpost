package gollorum.signpost.networking;

import gollorum.signpost.Signpost;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ForgePacketHandler extends PacketHandler {

    private static final int PROTOCOL_VERSION = 0;
    private static SimpleChannel channel;

    public static void initialize() {
        channel = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "main"))
            .acceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .simpleChannel();
        instance = new ForgePacketHandler();
        instance.init();
    }

    public ForgePacketHandler() { super(); }

    @Override
    public <T> void register(Event<T> event, ResourceLocation id){
        register(event.getMessageClass(),
                 (message, buffer) -> event.encode(buffer, message),
                 event::decode, event::handle);
    }

    public <T> void register(
        Class<T> messageClass,
        BiConsumer<T, RegistryFriendlyByteBuf> encode,
        Function<RegistryFriendlyByteBuf, T> decode,
        BiConsumer<T, Context> handle
    ){
        channel.messageBuilder(messageClass, NetworkProtocol.PLAY)
            .encoder(encode)
            .decoder(decode)
            .consumerMainThread(handle(handle))
            .add();
    }

    private static <T> BiConsumer<T, CustomPayloadEvent.Context> handle(BiConsumer<T, Context> handle) {
        return (message, context) -> {
            context.enqueueWork(() -> {
                if(context.getConnection().getReceiving() == PacketFlow.CLIENTBOUND)
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handle.accept(message,
                        context.getSender() == null
                            ? new Context.ClientFromServer()
                            : new Context.ClientFromClient(context.getSender())));
                else handle.accept(message, new Context.Server(context.getSender()));
            });
            context.setPacketHandled(true);
        };
    }

    @Override
    public <T> void sendToServer(T message) {
        channel.send(message, PacketDistributor.SERVER.noArg());
    }

    @Override
    public <T> void sendToPlayer(ServerPlayer target, T message) {
        channel.send(message, PacketDistributor.PLAYER.with(target));
    }

    @Override
    public <T> void sendToTracing(Level world, BlockPos pos, Supplier<T> t) {
        if(world == null) Signpost.LOGGER.warn("No world to notify mutation");
        else if(pos == null) Signpost.LOGGER.warn("No position to notify mutation");
        else channel.send(t.get(), PacketDistributor.TRACKING_CHUNK.with(world.getChunkAt(pos)));
    }

    @Override
    public <T> void sendToTracing(BlockEntity tile, Supplier<T> t) {
        sendToTracing(tile.getLevel(), tile.getBlockPos(), t);
    }

    @Override
    public <T> void sendToAll(T message) {
        channel.send(message, PacketDistributor.ALL.noArg());
    }

}