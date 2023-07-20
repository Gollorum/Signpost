//package gollorum.signpost.compat;
//
//import com.google.common.collect.Lists;
//import gollorum.signpost.Signpost;
//import gollorum.signpost.WaystoneHandle;
//import gollorum.signpost.minecraft.utils.LangKeys;
//import gollorum.signpost.minecraft.utils.TileEntityUtils;
//import gollorum.signpost.networking.PacketHandler;
//import gollorum.signpost.networking.ReflectionEvent;
//import gollorum.signpost.utils.EventDispatcher;
//import gollorum.signpost.utils.WaystoneLocationData;
//import gollorum.signpost.utils.WorldLocation;
//import gollorum.signpost.utils.math.geometry.Vector3;
//import gollorum.signpost.utils.serialization.StringSerializer;
//import net.blay09.mods.waystones.api.IWaystone;
//import net.blay09.mods.waystones.block.WaystoneBlock;
//import net.blay09.mods.waystones.core.PlayerWaystoneManager;
//import net.blay09.mods.waystones.core.WaystoneManager;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.network.FriendlyByteBuf;
//import net.minecraft.network.chat.Component;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraftforge.network.NetworkEvent;
//import net.minecraftforge.network.PacketDistributor;
//
//import java.util.*;
//import java.util.function.Consumer;
//import java.util.stream.Collectors;
//
//public final class WaystonesAdapter implements ExternalWaystoneLibrary.Adapter {
//
//    private WaystonesAdapter() {}
//    private static WaystonesAdapter instance;
//
//    public static void register() {
//        instance = new WaystonesAdapter();
//        ExternalWaystoneLibrary.onInitialize().addListener(ex -> { ex.registerAdapter(instance); });
//        PacketHandler.onInitialize().addListener(unit -> {
//            PacketHandler.register(new RequestEvent(), -100);
//            PacketHandler.register(new ReplyEvent(), -101);
//        });
//    }
//
//    private final EventDispatcher.Impl.WithPublicDispatch<Collection<ExternalWaystone>> onReply = new EventDispatcher.Impl.WithPublicDispatch<>();
//
//    @Override
//    public String typeTag() {
//        return "waystones";
//    }
//
//    @Override
//    public void requestKnownWaystones(Consumer<Collection<ExternalWaystone>> consumer) {
//        onReply.addListener(consumer);
//        PacketHandler.sendToServer(new RequestEvent());
//    }
//
//    @Override
//    public Optional<ExternalWaystone> getData(WaystoneHandle handle) {
//        return handle instanceof Handle ? getData((Handle) handle).map(w -> w) : Optional.empty();
//    }
//
//    private Optional<Waystone> getData(Handle handle) {
//        return WaystoneManager.get(Signpost.getServerInstance()).getWaystoneById(handle.id).map(Waystone::new);
//    }
//
//    private static final String notActivatedKey = "gui.waystones.inventory.no_waystones_activated";
//
//    @Override
//    public Optional<Component> cannotTeleportToBecause(Player player, WaystoneHandle handle) {
//        if((!(handle instanceof Handle))) return Optional.empty();
//        return getData((Handle)handle)
//            .map(waystone -> waystone.wrapped.isGlobal()
//                || PlayerWaystoneManager.isWaystoneActivated(player, waystone.wrapped)
//                ? Optional.<Component>empty()
//                : Optional.of((Component) Component.translatable(notActivatedKey)))
//            .orElse(Optional.empty());
//    }
//
//    @Override
//    public WaystoneHandle read(FriendlyByteBuf buffer) {
//        return new Handle(buffer.readUUID());
//    }
//
//    @Override
//    public WaystoneHandle read(CompoundTag compound) {
//        return new Handle(compound.getUUID("id"));
//    }
//
//    public static class Waystone implements ExternalWaystone {
//
//        public final IWaystone wrapped;
//
//        public Waystone(IWaystone wrapped) {this.wrapped = wrapped;}
//
//        @Override
//        public String name() {
//            return wrapped.getName();
//        }
//
//        @Override
//        public WaystoneLocationData loc() {
//            WorldLocation blockPos = new WorldLocation(wrapped.getPos(), wrapped.getDimension().location());
//            return new WaystoneLocationData(blockPos, Vector3.fromBlockPos(blockPos.blockPos.relative(spawnInDirection(blockPos))));
//        }
//
//        private Direction spawnInDirection(WorldLocation blockPos) {
//            Level world = TileEntityUtils.toWorld(blockPos.world, false).orElse(null);
//            BlockState state = world != null ? world.getBlockState(blockPos.blockPos) : null;
//            if(state == null || !state.hasProperty(WaystoneBlock.FACING)) return Direction.NORTH;
//            Direction direction = state.getValue(WaystoneBlock.FACING);
//            List<Direction> directionCandidates = Lists.newArrayList(direction, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH);
//
//            for (Direction candidate : directionCandidates) {
//                BlockPos offsetPos = blockPos.blockPos.relative(candidate);
//                BlockPos offsetPosUp = offsetPos.above();
//                if (!world.getBlockState(offsetPos).isSuffocating(world, offsetPos) && !world.getBlockState(offsetPosUp).isSuffocating(world, offsetPosUp)) {
//                    return candidate;
//                }
//            }
//            return direction;
//        }
//
//        @Override
//        public Handle handle() {
//            return new WaystonesAdapter.Handle(wrapped.getWaystoneUid());
//        }
//    }
//
//    public static class Handle implements ExternalWaystone.Handle {
//
//        public final UUID id;
//
//        public Handle(UUID id) {this.id = id;}
//
//        @Override
//        public String modMark() {
//            return "(Waystones)";
//        }
//
//        @Override
//        public String noTeleportLangKey() {
//            return LangKeys.noTeleportWaystoneMod;
//        }
//
//        @Override
//        public void write(FriendlyByteBuf buffer) {
//            StringSerializer.instance.write(instance.typeTag(), buffer);
//            buffer.writeUUID(id);
//        }
//
//        @Override
//        public CompoundTag write(CompoundTag compound) {
//            compound.putString("type", instance.typeTag());
//            compound.putUUID("id", id);
//            return compound;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//            Handle that = (Handle) o;
//            return Objects.equals(id, that.id);
//        }
//
//        @Override
//        public int hashCode() {
//            return id.hashCode();
//        }
//
//    }
//
//    public static final class RequestEvent extends ReflectionEvent<RequestEvent> {
//
//        @Override
//        public Class<RequestEvent> getMessageClass() {
//            return RequestEvent.class;
//        }
//
//        @Override
//        public void handle(RequestEvent message, NetworkEvent.Context context) {
//            PacketHandler.send(
//                PacketDistributor.PLAYER.with(context::getSender),
//                new ReplyEvent.Packet(PlayerWaystoneManager.getWaystones(context.getSender())
//                    .stream()
//                    .map(Waystone::new)
//                    .collect(Collectors.toList()))
//            );
//        }
//    }
//
//    public static final class ReplyEvent implements PacketHandler.Event<ReplyEvent.Packet> {
//
//        public static final class Packet {
//            public Collection<Waystone> waystones;
//            public Packet(Collection<Waystone> waystones) {
//                this.waystones = waystones;
//            }
//        }
//
//        @Override
//        public Class<Packet> getMessageClass() {
//            return Packet.class;
//        }
//
//        @Override
//        public void encode(Packet message, FriendlyByteBuf buffer) {
//            buffer.writeInt(message.waystones.size());
//            for(Waystone waystone : message.waystones)
//                net.blay09.mods.waystones.core.Waystone.write(buffer, waystone.wrapped);
//        }
//
//        @Override
//        public Packet decode(FriendlyByteBuf buffer) {
//            int size = buffer.readInt();
//            List<Waystone> waystones = new ArrayList<>();
//            for(int i = 0; i < size; i++) waystones.add(new Waystone(net.blay09.mods.waystones.core.Waystone.read(buffer)));
//            return new Packet(waystones);
//        }
//
//        @Override
//        public void handle(Packet message, NetworkEvent.Context context) {
//            instance.onReply.dispatch(new ArrayList<>(message.waystones), true);
//        }
//
//    }
//
//}