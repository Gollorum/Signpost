package gollorum.signpost.compat;

import net.minecraftforge.fml.ModList;

public class Compat {

    public static final String WaystonesId = "waystones";
    public static final String AntiqueAtlasId = "antiqueatlas";
    public static final String RepurposedStructuresId = "repurposed_structures";

    public static void register() {
//        if(ModList.get().isLoaded(Compat.WaystonesId))
//            WaystonesAdapter.register();

//        if(ModList.get().isLoaded(Compat.AntiqueAtlasId))
//            AntiqueAtlasAdapter.register();

        if(ModList.get().isLoaded(Compat.RepurposedStructuresId))
            RepurposedStructuresAdapter.register();
    }

}
