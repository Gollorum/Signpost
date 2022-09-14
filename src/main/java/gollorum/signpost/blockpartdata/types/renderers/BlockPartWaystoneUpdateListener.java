package gollorum.signpost.blockpartdata.types.renderers;

import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.events.WaystoneUpdatedEvent;
import gollorum.signpost.utils.BlockPart;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class BlockPartWaystoneUpdateListener {

    private static final BlockPartWaystoneUpdateListener instance = new BlockPartWaystoneUpdateListener();
    public static BlockPartWaystoneUpdateListener getInstance() { return instance; }

    private final WeakHashMap<BlockPart<?>, WeakReference<Consumer<WaystoneUpdatedEvent>>> listeners = new WeakHashMap<>();

    public void addListener(BlockPart<?> part, Consumer<WaystoneUpdatedEvent> onUpdate) {
        listeners.put(part, new WeakReference<>(onUpdate));
    }

    private BlockPartWaystoneUpdateListener(){}

    public void initialize() {
        WaystoneLibrary.getInstance().updateEventDispatcher.addListener(event -> {
            for (Map.Entry<BlockPart<?>, WeakReference<Consumer<WaystoneUpdatedEvent>>> entry : listeners.entrySet().stream().toList()) {
                Consumer<WaystoneUpdatedEvent> onUpdate = entry.getValue().get();
                if(onUpdate == null)
                    listeners.remove(entry.getKey());
                else onUpdate.accept(event);
            }
        });
    }

}
