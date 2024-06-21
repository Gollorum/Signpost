package gollorum.signpost.minecraft.worldgen;

import gollorum.signpost.WaystoneHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface IWaystoneDiscoveryEventListener {

    void initialize();
    void registerNew(WaystoneHandle.Vanilla handle, ServerLevel world, BlockPos pos);

}
