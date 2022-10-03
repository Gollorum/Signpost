package gollorum.signpost.compat;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.EventDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ExternalWaystoneLibrary {

    private static final EventDispatcher.Impl.WithPublicDispatch<ExternalWaystoneLibrary> _onInitialize = new EventDispatcher.Impl.WithPublicDispatch<>();
    public static EventDispatcher<ExternalWaystoneLibrary> onInitialize() { return _onInitialize; }

    private static ExternalWaystoneLibrary instance;
    public static ExternalWaystoneLibrary getInstance() { return instance; }

    private ExternalWaystoneLibrary() {}
    public static void initialize() {
        instance = new ExternalWaystoneLibrary();
        _onInitialize.dispatch(instance, false);
    }

    private final List<Adapter> adapters = new ArrayList<>();

    public Optional<WaystoneHandle> read(String type, FriendlyByteBuf buffer) {
        for(Adapter adapter : adapters) if(adapter.typeTag().equals(type)) return Optional.of(adapter.read(buffer));
        return Optional.empty();
    }

    public Optional<WaystoneHandle> read(String type, CompoundTag compound) {
        for(Adapter adapter : adapters) if(adapter.typeTag().equals(type)) return Optional.of(adapter.read(compound));
        return Optional.empty();
    }

    public void registerAdapter(Adapter adapter) { adapters.add(adapter); }

    // call this on the client
    // consumer might be called multiple times, once per adapter.
    public void requestKnownWaystones(Consumer<Collection<ExternalWaystone>> consumer) {
        for(Adapter adapter : adapters) adapter.requestKnownWaystones(consumer);
    }

    public Optional<ExternalWaystone> getData(WaystoneHandle handle) {
        return adapters.stream().flatMap(a -> a.getData(handle).stream()).findFirst();
    }

    public Optional<Component> cannotTeleportToBecause(ServerPlayer player, WaystoneHandle handle) {
        return adapters.stream().flatMap(a -> a.cannotTeleportToBecause(player, handle).stream()).findFirst();
    }

    public interface Adapter {
        String typeTag();

        // call this on the client
        void requestKnownWaystones(Consumer<Collection<ExternalWaystone>> consumer);

        WaystoneHandle read(FriendlyByteBuf buffer);
        WaystoneHandle read(CompoundTag compound);

        Optional<ExternalWaystone> getData(WaystoneHandle handle);
        Optional<Component> cannotTeleportToBecause(ServerPlayer player, WaystoneHandle handle);
    }

}
