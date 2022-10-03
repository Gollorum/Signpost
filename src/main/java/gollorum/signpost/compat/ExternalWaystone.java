package gollorum.signpost.compat;

import gollorum.signpost.WaystoneDataBase;
import gollorum.signpost.WaystoneHandle;

public interface ExternalWaystone extends WaystoneDataBase {

    Handle handle();

    public static interface Handle extends WaystoneHandle {

        // Will be shown in the gui to distinguish it from own waystones.
        String modMark();

        // External waystones usually have their own teleport / payment system, which should be used instead.
        // To tell the player, a message will be displayed when they try to use a sign. This is the lang key.
        String noTeleportLangKey();

    }
}
