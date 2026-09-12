package gollorum.signpost.networking;

import gollorum.signpost.Signpost;
import gollorum.signpost.SignpostNeoforge;
import gollorum.signpost.compat.Compat;
import gollorum.signpost.compat.WaystonesAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Supplier;

public class NeoForgePacketHandler extends PacketHandler {

    public static void initialize(IEventBus bus) {
        instance = new NeoForgePacketHandler();
        instance.init();
        instance.register(new SignpostNeoforge.JoinServerEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "join_server"));
        for (var entry : Compat.getEvents().entrySet()) {
            instance.register(entry.getValue(), entry.getKey());
        }
        bus.register(NeoForgePacketHandler.class);
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Signpost.MOD_ID);

        for (var tuple : instance.events) {
            ((NeoForgePacketHandler) instance).registerCommon(tuple._1(), tuple._2(), registrar);
        }

    }

    // NeoForge 21.1 has no separate client payload-handler event: one bidirectional registration takes
    // both handlers, and DirectionalPayloadHandler picks by packet flow. The client branch is only ever
    // invoked on a client, so Context.Client - which reaches into Minecraft - stays off the server.
    private <T> void registerCommon(Event<T> event, ResourceLocation id, PayloadRegistrar registrar) {
        var type = new CustomPacketPayload.Type<Payload<T>>(id);
        registrar.playBidirectional(
            type,
            event.codec().map(
                message -> new Payload<T>(type, event, message),
                payload -> payload.message
            ),
            new DirectionalPayloadHandler<Payload<T>>(
                (payload, context) -> context.enqueueWork(() ->
                    payload.event.handle(payload.message, new Context.Client())
                ),
                (payload, context) -> context.enqueueWork(() ->
                    payload.event.handle(payload.message, new Context.Server((ServerPlayer) context.player()))
                )
            ));
    }

    private <T> Payload<T> toPayload(T message) {
        var tuple = eventMap.get(message.getClass());
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