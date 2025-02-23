package gollorum.signpost.networking;

import gollorum.signpost.Signpost;
import gollorum.signpost.utils.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
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
        registrar.playBidirectional(type, event.map(
            message -> new Payload<T>(type, event, message),
            payload -> payload.message
            ),
//            var message = event.decode(buffer);
//            return new Payload<>(id, event, message);
        }, NeoForgePacketHandler::handle);
    }

    private static <T> void handle(Payload<T> payload, IPayloadContext context) {
        context.workHandler().submitAsync(() ->
            payload.event.handle(payload.message, context.flow().isClientbound()
                ? context.player()
                    .<Context>map(Context.ClientFromClient::new)
                    .orElseGet(Context.ClientFromServer::new)
                : new Context.Server((ServerPlayer) context.player().get())));
    }

    private <T> Payload<T> toPayload(T message) {
        var tuple = events.get(message.getClass());
        return new Payload<>(tuple._2, (Event<T>) tuple._1, message);
    }

    @Override
    public <T> void sendToServer(T message) {
        PacketDistributor.SERVER.noArg().send(toPayload(message));
    }

    @Override
    public <T> void sendToPlayer(ServerPlayer target, T message) {
        PacketDistributor.PLAYER.with(target).send(toPayload(message));
    }

    @Override
    public <T> void sendToTracing(Level world, BlockPos pos, Supplier<T> t) {
        if(world == null) Signpost.LOGGER.warn("No world to notify mutation");
        else if(pos == null) Signpost.LOGGER.warn("No position to notify mutation");
        else PacketDistributor.TRACKING_CHUNK.with(world.getChunkAt(pos)).send(toPayload(t.get()));
    }

    @Override
    public <T> void sendToTracing(BlockEntity tile, Supplier<T> t) {
        sendToTracing(tile.getLevel(), tile.getBlockPos(), t);
    }

    @Override
    public <T> void sendToAll(T message) {
        PacketDistributor.ALL.noArg().send(toPayload(message));
    }

    private record Payload<T>(Type<Payload<T>> type, PacketHandler.Event<T> event, T message) implements CustomPacketPayload { }

}