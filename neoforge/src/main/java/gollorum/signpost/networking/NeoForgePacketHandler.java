package gollorum.signpost.networking;

import gollorum.signpost.Signpost;
import gollorum.signpost.utils.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NeoForgePacketHandler extends PacketHandler {

    private PayloadRegistrar registrar;
    private final Map<Class<?>, Tuple<Event<?>, ResourceLocation>> events = new HashMap<>();

    public static void initialize(IEventBus bus) {
        bus.register(NeoForgePacketHandler.class);
        instance = new NeoForgePacketHandler();
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar  registrar = event.registrar(Signpost.MOD_ID);
        ((NeoForgePacketHandler) instance).registrar = registrar;
        instance.init();
    }

    @Override
    public <T> void register(Event<T> event, ResourceLocation id){
        events.put(event.getMessageClass(), new Tuple<>(event, id));
        var type = new CustomPacketPayload.Type<Payload<T>>(id);
        registrar.playBidirectional(
            type,
            event.codec().map(
                message -> new Payload<T>(type, event, message),
                payload -> payload.message
            ),
            NeoForgePacketHandler::handle
        );
    }

    private static <T> void handle(Payload<T> payload, IPayloadContext context) {
        context.enqueueWork(() ->
            payload.event.handle(payload.message, context.flow().isClientbound()
                ? new Context.Client()
                : new Context.Server((ServerPlayer) context.player())));
    }

    private <T> Payload<T> toPayload(T message) {
        var tuple = events.get(message.getClass());
        return new Payload<>(new CustomPacketPayload.Type<>(tuple._2()), (Event<T>) tuple._1(), message);
    }

    @Override
    public <T> void sendToServer(T message) {
        PacketDistributor.sendToServer(toPayload(message));
    }

    @Override
    public <T> void sendToPlayer(ServerPlayer target, T message) {
        PacketDistributor.sendToPlayer(target, toPayload(message));
    }

    @Override
    public <T> void sendToTracing(ServerLevel world, BlockPos pos, Supplier<T> t) {
        if(world == null) Signpost.LOGGER.warn("No world to notify mutation");
        else if(pos == null) Signpost.LOGGER.warn("No position to notify mutation");
        else PacketDistributor.sendToPlayersTrackingChunk(world, new ChunkPos(pos), toPayload(t.get()));
    }

    @Override
    public <T> void sendToTracing(BlockEntity tile, Supplier<T> t) {
        sendToTracing((ServerLevel) tile.getLevel(), tile.getBlockPos(), t);
    }

    @Override
    public <T> void sendToAll(T message) {
        PacketDistributor.sendToAllPlayers(toPayload(message));
    }

    private record Payload<T>(Type<Payload<T>> type, PacketHandler.Event<T> event, T message) implements CustomPacketPayload { }

}