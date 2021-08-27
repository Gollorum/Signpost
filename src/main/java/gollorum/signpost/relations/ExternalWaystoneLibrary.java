package gollorum.signpost.relations;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.EventDispatcher;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

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

    public Optional<WaystoneHandle> read(String type, PacketBuffer buffer) {
        for(Adapter adapter : adapters) if(adapter.typeTag().equals(type)) return Optional.of(adapter.read(buffer));
        return Optional.empty();
    }

    public Optional<WaystoneHandle> read(String type, CompoundNBT compound) {
        for(Adapter adapter : adapters) if(adapter.typeTag().equals(type)) return Optional.of(adapter.read(compound));
        return Optional.empty();
    }

    public void registerAdapter(Adapter adapter) { adapters.add(adapter); }

    // call this on the client
    // consumer might be called multiple times, once per adapter.
    public void requestKnownWaystones(Consumer<Collection<ExternalWaystone>> consumer) {
        for(Adapter adapter : adapters) adapter.requestKnownWaystones(consumer);
    }

    public interface Adapter {
        String typeTag();

        // call this on the client
        void requestKnownWaystones(Consumer<Collection<ExternalWaystone>> consumer);

        WaystoneHandle read(PacketBuffer buffer);
        WaystoneHandle read(CompoundNBT compound);
    }

}
