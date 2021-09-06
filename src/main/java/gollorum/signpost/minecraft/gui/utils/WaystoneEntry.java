package gollorum.signpost.minecraft.gui.utils;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public final class WaystoneEntry {

    public String entryName;
    public String displayName;
    public final WaystoneHandle handle;
    public BlockPos pos;

    public WaystoneEntry(String entryName, String displayName, WaystoneHandle handle, BlockPos pos) {
        this.entryName = entryName;
        this.displayName = displayName;
        this.handle = handle;
        this.pos = pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaystoneEntry that = (WaystoneEntry) o;
        return Objects.equals(entryName, that.entryName) && Objects.equals(displayName, that.displayName) && Objects.equals(handle, that.handle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryName, displayName, handle);
    }

    @Override
    public String toString() {
        return entryName;
    }
}
