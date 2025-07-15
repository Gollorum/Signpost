package gollorum.signpost.minecraft.data;

import com.mojang.serialization.Codec;
import gollorum.signpost.WaystoneHandle;
import net.minecraft.core.component.DataComponentType;

public record WaystoneHandleData(WaystoneHandle.Vanilla handle) {

    public static final Codec<WaystoneHandleData> CODEC = WaystoneHandle.Vanilla.CODEC.codec().xmap(
        WaystoneHandleData::new,
        WaystoneHandleData::handle
    );

    public static final DataComponentType<WaystoneHandleData> TYPE =
        new DataComponentType.Builder<WaystoneHandleData>().persistent(WaystoneHandleData.CODEC).build();

}
