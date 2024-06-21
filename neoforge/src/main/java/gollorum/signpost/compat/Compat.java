package gollorum.signpost.compat;

import gollorum.signpost.platform.Services;

public class Compat {

    public static final String WaystonesId = "waystones";
    public static final String AntiqueAtlasId = "antiqueatlas";
    public static final String RepurposedStructuresId = "repurposed_structures";

    public static void register() {
        if(Services.PLATFORM.isModLoaded(Compat.WaystonesId))
            WaystonesAdapter.register();

//        if(Services.PLATFORM.isModLoaded.isLoaded(Compat.AntiqueAtlasId))
//            AntiqueAtlasAdapter.register();

        if(Services.PLATFORM.isModLoaded(Compat.RepurposedStructuresId))
            RepurposedStructuresAdapter.register();
    }

}
