package gollorum.signpost.compat;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.EventDispatcher;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import io.netty.buffer.ByteBuf;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneVisibility;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.block.WaystoneBlock;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WaystoneImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class WaystonesAdapter implements ExternalWaystoneLibrary.Adapter {

    private WaystonesAdapter() {}
    private static WaystonesAdapter instance;

    public static void register() {
        instance = new WaystonesAdapter();
        ExternalWaystoneLibrary.onInitialize().addListener(ex -> { ex.registerAdapter(instance); });
        PacketHandler.onInitializeDo(packetHandler -> {
            packetHandler.register(RequestEvent.INSTANCE, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "waystones_adapter_request"));
            packetHandler.register(new ReplyEvent(), ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "waystones_adapter_reply"));
            return true;
        });
    }

    private final EventDispatcher.Impl.WithPublicDispatch<Collection<ExternalWaystone>> onReply = new EventDispatcher.Impl.WithPublicDispatch<>();

    @Override
    public String typeTag() {
        return "waystones";
    }

    @Override
    public void requestKnownWaystones(Consumer<Collection<ExternalWaystone>> consumer) {
        onReply.addListener(consumer);
        PacketHandler.getInstance().sendToServer(RequestEvent.INSTANCE);
    }

    @Override
    public Optional<ExternalWaystone> getData(WaystoneHandle handle) {
        return handle instanceof Handle ? getData((Handle) handle).map(w -> w) : Optional.empty();
    }

    private Optional<WaystoneWaystone> getData(Handle handle) {
        return WaystonesAPI.getWaystone(Signpost.getServerInstance(), handle.id).map(WaystoneWaystone::new);
    }

    private static final String notActivatedKey = "gui.waystones.inventory.no_waystones_activated";

    @Override
    public Optional<Component> cannotTeleportToBecause(Player player, WaystoneHandle handle) {
        if((!(handle instanceof Handle))) return Optional.empty();
        return getData((Handle)handle)
            .flatMap(waystone -> waystone.wrapped.getVisibility() == WaystoneVisibility.GLOBAL
                || PlayerWaystoneManager.isWaystoneActivated(player, waystone.wrapped)
                ? Optional.<Component>empty()
                : Optional.of((Component) Component.translatable(notActivatedKey)));
    }

    @Override
    public MapCodec<? extends WaystoneHandle> getCodec() {
        return ExternalWaystone.Handle.MAP_CODEC;
    }

    @Override
    public StreamCodec<ByteBuf, ? extends WaystoneHandle> getStreamCodec() {
        return Handle.STREAM_CODEC;
    }

    public record WaystoneWaystone(Waystone wrapped) implements ExternalWaystone {

        @Override
        public String name() {
            return wrapped.getName().getString();
        }

        @Override
        public WaystoneLocationData loc() {
            WorldLocation blockPos = new WorldLocation(wrapped.getPos(), wrapped.getDimension().location());
            return new WaystoneLocationData(blockPos, Vector3.fromBlockPos(blockPos.blockPos().relative(spawnInDirection(blockPos))));
        }

        private Direction spawnInDirection(WorldLocation blockPos) {
            Level world = TileEntityUtils.toWorld(blockPos.world(), false).orElse(null);
            BlockState state = world != null ? world.getBlockState(blockPos.blockPos()) : null;
            if (state == null || !state.hasProperty(WaystoneBlock.FACING)) return Direction.NORTH;
            Direction direction = state.getValue(WaystoneBlock.FACING);
            List<Direction> directionCandidates = Lists.newArrayList(direction, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH);

            for (Direction candidate : directionCandidates) {
                BlockPos offsetPos = blockPos.blockPos().relative(candidate);
                BlockPos offsetPosUp = offsetPos.above();
                if (!world.getBlockState(offsetPos).isSuffocating(world, offsetPos) && !world.getBlockState(offsetPosUp).isSuffocating(world, offsetPosUp)) {
                    return candidate;
                }
            }
            return direction;
        }

        @Override
        public Handle handle() {
            return new WaystonesAdapter.Handle(wrapped.getWaystoneUid());
        }
    }

    public static record Handle(UUID id) implements ExternalWaystone.Handle {

        public static final MapCodec<Handle> CODEC =
            UUIDUtil.CODEC.xmap(Handle::new, Handle::id)
                .fieldOf("id");

        public static final StreamCodec<ByteBuf, Handle> STREAM_CODEC =
            UUIDUtil.STREAM_CODEC.map(Handle::new, Handle::id);

        @Override
        public String typeTag() {
            return instance.typeTag();
        }

        @Override
        public String modMark() {
            return "(Waystones)";
        }

        @Override
        public String noTeleportLangKey() {
            return LangKeys.noTeleportWaystoneMod;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Handle that = (Handle) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

    }

    public static final class RequestEvent implements PacketHandler.Event.ForServer<RequestEvent> {
        public static final RequestEvent INSTANCE = new RequestEvent();
        
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RequestEvent> codec() {
            return StreamCodec.unit(RequestEvent.INSTANCE);
        }

        @Override
        public Class<RequestEvent> getMessageClass() {
            return RequestEvent.class;
        }

        @Override
        public void handle(RequestEvent message, PacketHandler.Context.Server context) {
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
                new ReplyEvent.Packet(PlayerWaystoneManager.getActivatedWaystones(context.getPlayer())
                    .stream()
                    .map(WaystoneWaystone::new)
                    .collect(Collectors.toList()))
            );
        }
    }

    public static final class ReplyEvent implements PacketHandler.Event<ReplyEvent.Packet> {

        public static final record Packet(Collection<WaystoneWaystone> waystones) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.collection(
                    ArrayList::new,
                    WaystoneImpl.STREAM_CODEC.map(WaystoneWaystone::new, WaystoneWaystone::wrapped)
                ), Packet::waystones,
                Packet::new
            );
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Packet> codec() {
            return Packet.STREAM_CODEC;
        }

        @Override
        public Class<Packet> getMessageClass() {
            return Packet.class;
        }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            instance.onReply.dispatch(new ArrayList<>(message.waystones), true);
        }

    }

}