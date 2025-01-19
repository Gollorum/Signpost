package gollorum.signpost.minecraft.storage;

import gollorum.signpost.WaystoneHandle;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.UnaryOperator;

public class ItemComponents {

    public static final DataComponentType<WaystoneHandle.Vanilla> WAYSTONE_HANDLE = register("Handle",
        builder -> builder
            .networkSynchronized(WaystoneHandle.Vanilla.BufferSerializer)
            .persistent(WaystoneHandle.Vanilla.vanillaCodec)
    );

    private static <T> DataComponentType<T> register(String key, UnaryOperator<DataComponentType.Builder<T>> apply) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, key, apply.apply(DataComponentType.builder()).build());
    }

}
