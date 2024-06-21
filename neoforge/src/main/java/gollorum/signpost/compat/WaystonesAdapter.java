package gollorum.signpost.compat;

import com.google.common.collect.Lists;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.networking.ReflectionEvent;
import gollorum.signpost.utils.EventDispatcher;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneVisibility;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.block.WaystoneBlock;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WaystoneImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
            packetHandler.register(new RequestEvent(), new ResourceLocation(Signpost.MOD_ID, "waystones_adapter_request"));
            packetHandler.register(new ReplyEvent(), new ResourceLocation(Signpost.MOD_ID, "waystones_adapter_reply"));
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
        PacketHandler.getInstance().sendToServer(new RequestEvent());
    }

    @Override
    public Optional<ExternalWaystone> getData(WaystoneHandle handle) {
        return handle instanceof Handle ? getData((Handle) handle).map(w -> w) : Optional.empty();
    }

    private Optional<WaystoneWaystone> getData(Handle handle) {
        return WaystonesAPI.getWaystone(Signpost.getServerInstance().overworld(), handle.id).map(WaystoneWaystone::new);
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
    public WaystoneHandle read(FriendlyByteBuf buffer) {
        return new Handle(buffer.readUUID());
    }

    @Override
    public WaystoneHandle read(CompoundTag compound) {
        return new Handle(compound.getUUID("id"));
    }

    public static class WaystoneWaystone implements ExternalWaystone {

        public final Waystone wrapped;

        public WaystoneWaystone(Waystone wrapped) {this.wrapped = wrapped;}

        @Override
        public String name() {
            return wrapped.getName().getString();
        }

        @Override
        public WaystoneLocationData loc() {
            WorldLocation blockPos = new WorldLocation(wrapped.getPos(), wrapped.getDimension().location());
            return new WaystoneLocationData(blockPos, Vector3.fromBlockPos(blockPos.blockPos.relative(spawnInDirection(blockPos))));
        }

        private Direction spawnInDirection(WorldLocation blockPos) {
            Level world = TileEntityUtils.toWorld(blockPos.world, false).orElse(null);
            BlockState state = world != null ? world.getBlockState(blockPos.blockPos) : null;
            if(state == null || !state.hasProperty(WaystoneBlock.FACING)) return Direction.NORTH;
            Direction direction = state.getValue(WaystoneBlock.FACING);
            List<Direction> directionCandidates = Lists.newArrayList(direction, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH);

            for (Direction candidate : directionCandidates) {
                BlockPos offsetPos = blockPos.blockPos.relative(candidate);
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

    public static class Handle implements ExternalWaystone.Handle {

        public final UUID id;

        public Handle(UUID id) {this.id = id;}

        @Override
        public String modMark() {
            return "(Waystones)";
        }

        @Override
        public String noTeleportLangKey() {
            return LangKeys.noTeleportWaystoneMod;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            StringSerializer.instance.write(instance.typeTag(), buffer);
            buffer.writeUUID(id);
        }

        @Override
        public CompoundTag write(CompoundTag compound) {
            compound.putString("type", instance.typeTag());
            compound.putUUID("id", id);
            return compound;
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

    public static final class RequestEvent extends ReflectionEvent.ForServer<RequestEvent> {

        @Override
        public Class<RequestEvent> getMessageClass() {
            return RequestEvent.class;
        }

        @Override
        public void handle(RequestEvent message, PacketHandler.Context.Server context) {
            PacketHandler.getInstance().sendToPlayer(
                context.sender(),
                new ReplyEvent.Packet(PlayerWaystoneManager.getActivatedWaystones(context.getSender())
                    .stream()
                    .map(WaystoneWaystone::new)
                    .collect(Collectors.toList()))
            );
        }
    }

    public static final class ReplyEvent implements PacketHandler.Event<ReplyEvent.Packet> {

        public static final class Packet {
            public Collection<WaystoneWaystone> waystones;
            public Packet(Collection<WaystoneWaystone> waystones) {
                this.waystones = waystones;
            }
        }

        @Override
        public Class<Packet> getMessageClass() {
            return Packet.class;
        }

        @Override
        public void encode(Packet message, FriendlyByteBuf buffer) {
            buffer.writeInt(message.waystones.size());
            for(WaystoneWaystone waystone : message.waystones)
                WaystoneImpl.write(buffer, waystone.wrapped);
        }

        @Override
        public Packet decode(FriendlyByteBuf buffer) {
            int size = buffer.readInt();
            List<WaystoneWaystone> waystones = new ArrayList<>();
            for(int i = 0; i < size; i++) waystones.add(new WaystoneWaystone(WaystoneImpl.read(buffer)));
            return new Packet(waystones);
        }

        @Override
        public void handle(Packet message, PacketHandler.Context context) {
            instance.onReply.dispatch(new ArrayList<>(message.waystones), true);
        }

    }

}