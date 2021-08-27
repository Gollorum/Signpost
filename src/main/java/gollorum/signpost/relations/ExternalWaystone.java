package gollorum.signpost.relations;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.utils.WorldLocation;

public interface ExternalWaystone {

    String name();
    WorldLocation loc();

    Handle handle();

    public static interface Handle extends WaystoneHandle {

        // Will be shown in the gui to distinguish it from own waystones.
        String modMark();

        // External waystones usually have their own teleport / payment system, which should be used instead.
        // To tell the player, a message will be displayed when they try to use a sign. This is the lang key.
        String noTeleportLangKey();

    }
}
