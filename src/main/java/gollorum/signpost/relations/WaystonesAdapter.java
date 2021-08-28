package gollorum.signpost.relations;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.networking.ReflectionEvent;
import gollorum.signpost.utils.EventDispatcher;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class WaystonesAdapter implements ExternalWaystoneLibrary.Adapter {

    private WaystonesAdapter() {}
    private static WaystonesAdapter instance;

    public static void register() {
        instance = new WaystonesAdapter();
        ExternalWaystoneLibrary.onInitialize().addListener(ex -> ex.registerAdapter(instance));
        PacketHandler.onInitialize().addListener(unit -> {
            PacketHandler.register(new RequestEvent(), -100);
            PacketHandler.register(new ReplyEvent(), -101);
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
        PacketHandler.sendToServer(new RequestEvent());
    }

    @Override
    public WaystoneHandle read(PacketBuffer buffer) {
        return new Handle(buffer.readUUID());
    }

    @Override
    public WaystoneHandle read(CompoundNBT compound) {
        return new Handle(compound.getUUID("id"));
    }

    public static class Waystone implements ExternalWaystone {

        public final IWaystone wrapped;

        public Waystone(IWaystone wrapped) {this.wrapped = wrapped;}

        @Override
        public String name() {
            return wrapped.getName();
        }

        @Override
        public WorldLocation loc() {
            return new WorldLocation(wrapped.getPos(), wrapped.getDimension().location());
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
        public void write(PacketBuffer buffer) {
            StringSerializer.instance.write(instance.typeTag(), buffer);
            buffer.writeUUID(id);
        }

        @Override
        public void write(CompoundNBT compound) {
            compound.putString("type", instance.typeTag());
            compound.putUUID("id", id);
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

    public static final class RequestEvent extends ReflectionEvent<RequestEvent> {

        @Override
        public Class<RequestEvent> getMessageClass() {
            return RequestEvent.class;
        }

        @Override
        public void handle(RequestEvent message, NetworkEvent.Context context) {
            PacketHandler.send(
                PacketDistributor.PLAYER.with(context::getSender),
                new ReplyEvent.Packet(PlayerWaystoneManager.getWaystones(context.getSender())
                    .stream()
                    .map(Waystone::new)
                    .collect(Collectors.toList()))
            );
        }
    }

    public static final class ReplyEvent implements PacketHandler.Event<ReplyEvent.Packet> {

        public static final class Packet {
            public Collection<Waystone> waystones;
            public Packet(Collection<Waystone> waystones) {
                this.waystones = waystones;
            }
        }

        @Override
        public Class<Packet> getMessageClass() {
            return Packet.class;
        }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            buffer.writeInt(message.waystones.size());
            for(Waystone waystone : message.waystones)
                net.blay09.mods.waystones.core.Waystone.write(buffer, waystone.wrapped);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            int size = buffer.readInt();
            List<Waystone> waystones = new ArrayList<>();
            for(int i = 0; i < size; i++) waystones.add(new Waystone(net.blay09.mods.waystones.core.Waystone.read(buffer)));
            return new Packet(waystones);
        }

        @Override
        public void handle(Packet message, NetworkEvent.Context context) {
            instance.onReply.dispatch(new ArrayList<>(message.waystones), true);
        }

    }

}
