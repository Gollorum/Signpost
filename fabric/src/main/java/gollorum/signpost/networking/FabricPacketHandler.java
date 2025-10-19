package gollorum.signpost.networking;

import gollorum.signpost.Signpost;
import gollorum.signpost.utils.Tuple;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class FabricPacketHandler extends PacketHandler {

    private final Map<Class<?>, Tuple<Event<?>, ResourceLocation>> events = new HashMap<>();

    public static void initialize() {
        instance = new FabricPacketHandler();
        instance.init();
    }

    @Override
    public <T> void register(Event<T> event, ResourceLocation id){
        events.put(event.getMessageClass(), new Tuple<>(event, id));
        var type = new CustomPacketPayload.Type<Payload<T>>(id);
        PayloadTypeRegistry.playC2S().register(
            type,
            event.codec().map(
                message -> new Payload<T>(type, event, message),
                payload -> payload.message
            )
        );

        // TODO: Is that legal on servers?
        ClientPlayNetworking.registerGlobalReceiver(type, FabricPacketHandler::handleOnClient);
        ServerPlayNetworking.registerGlobalReceiver(type, FabricPacketHandler::handleOnServer);
    }

    private static <T> void handleOnClient(Payload<T> payload, ClientPlayNetworking.Context context) {
        payload.event.handle(payload.message, new Context.Client());
    }
    private static <T> void handleOnServer(Payload<T> payload, ServerPlayNetworking.Context context) {
        payload.event.handle(payload.message, new Context.Server(context.player()));
    }

    private <T> Payload<T> toPayload(T message) {
        var tuple = events.get(message.getClass());
        return new Payload<>(new CustomPacketPayload.Type<>(tuple._2()), (Event<T>) tuple._1(), message);
    }

    @Override
    public <T> void sendToServer(T message) {
        ClientPlayNetworking.send(toPayload(message));
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