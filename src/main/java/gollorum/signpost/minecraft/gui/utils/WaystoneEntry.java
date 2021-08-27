package gollorum.signpost.minecraft.gui.utils;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.WorldLocation;

import java.util.Objects;

public final class WaystoneEntry {

    public final String entryName;
    public final String displayName;
    public final WaystoneHandle handle;
    public final WorldLocation loc;

    public WaystoneEntry(String entryName, String displayName, WaystoneHandle handle, WorldLocation loc) {
        this.entryName = entryName;
        this.displayName = displayName;
        this.handle = handle;
        this.loc = loc;
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
