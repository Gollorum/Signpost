package gollorum.signpost.blockpartdata.types.renderers;

import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.events.WaystoneUpdatedEvent;
import gollorum.signpost.utils.BlockPart;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class BlockPartWaystoneUpdateListener {

    private static final BlockPartWaystoneUpdateListener instance = new BlockPartWaystoneUpdateListener();
    public static BlockPartWaystoneUpdateListener getInstance() { return instance; }

    private final WeakHashMap<BlockPart<?>, BiConsumer<BlockPart<?>, WaystoneUpdatedEvent>> listeners = new WeakHashMap<>();

    public <T extends BlockPart<T>> void addListener(T part, BiConsumer<T, WaystoneUpdatedEvent> onUpdate) {
        listeners.put(part, (untypedPart, event) -> onUpdate.accept((T)untypedPart, event));
    }

    private BlockPartWaystoneUpdateListener(){}

    public void initialize() {
        listeners.clear();
        WaystoneLibrary.getInstance().updateEventDispatcher.addListener(event -> {
            for (Map.Entry<BlockPart<?>, BiConsumer<BlockPart<?>, WaystoneUpdatedEvent>> entry : listeners.entrySet().stream().toList())
                entry.getValue().accept(entry.getKey(), event);
        });
    }

}
