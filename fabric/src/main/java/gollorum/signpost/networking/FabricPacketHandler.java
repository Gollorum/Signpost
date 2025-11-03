package gollorum.signpost.networking;

import gollorum.signpost.Signpost;
import gollorum.signpost.SignpostFabric;
import gollorum.signpost.compat.Compat;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public class FabricPacketHandler extends PacketHandler {

    private final boolean isClient;

    public static void initialize(boolean isClient) {
        instance = new FabricPacketHandler(isClient);
        instance.init();
        instance.register(new SignpostFabric.JoinServerEvent(), net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "join_server"));
        for (var entry : Compat.getEvents().entrySet()) {
            instance.register(entry.getValue(), entry.getKey());
        }

        for (var tuple : instance.events) {
            ((FabricPacketHandler) instance).actuallyRegister(tuple._1(), tuple._2());
        }
    }

    private FabricPacketHandler(boolean isClient) {
        this.isClient = isClient;
    }

    private <T> void actuallyRegister(Event<T> event, ResourceLocation id) {
        var type = new CustomPacketPayload.Type<Payload<T>>(id);
        PayloadTypeRegistry.playC2S().register(
            type,
            event.codec().map(
                message -> new Payload<T>(type, event, message),
                payload -> payload.message
            )
        );
        PayloadTypeRegistry.playS2C().register(
            type,
            event.codec().map(
                message -> new Payload<T>(type, event, message),
                payload -> payload.message
            )
        );

        if (isClient)
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(type, FabricPacketHandler::handleOnClient);
        ServerPlayNetworking.registerGlobalReceiver(type, FabricPacketHandler::handleOnServer);
    }

    private static <T> void handleOnClient(Payload<T> payload, net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context context) {
        payload.event.handle(payload.message, new Context.Client());
    }
    private static <T> void handleOnServer(Payload<T> payload, ServerPlayNetworking.Context context) {
        payload.event.handle(payload.message, new Context.Server(context.player()));
    }

    private <T> Payload<T> toPayload(T message) {
        var tuple = eventMap.get(message.getClass());
        return new Payload<>(new CustomPacketPayload.Type<>(tuple._2()), (Event<T>) tuple._1(), message);
    }

    @Override
    public <T> void sendToServer(T message) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(toPayload(message));
    }

    @Override
    public <T> void sendToPlayer(ServerPlayer target, T message) {
        ServerPlayNetworking.send(target, toPayload(message));
    }

    @Override
    public <T> void sendToTracing(ServerLevel world, BlockPos pos, Supplier<T> t) {
        if(world == null) Signpost.LOGGER.warn("No world to notify mutation");
        else if(pos == null) Signpost.LOGGER.warn("No position to notify mutation");
        else {
            var payload = toPayload(t.get());
            for(ServerPlayer player : world.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    @Override
    public <T> void sendToTracing(BlockEntity tile, Supplier<T> t) {
        sendToTracing((ServerLevel) tile.getLevel(), tile.getBlockPos(), t);
    }

    @Override
    public <T> void sendToAll(T message) {
        assert Signpost.getServerType().isServer;
        Signpost.getServerInstance().getPlayerList().broadcastAll(
            ServerPlayNetworking.createS2CPacket(toPayload(message))
        );
    }

    private record Payload<T>(Type<Payload<T>> type, PacketHandler.Event<T> event, T message) implements CustomPacketPayload { }

}