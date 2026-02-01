package gollorum.signpost.compat;

import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.platform.Services;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

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

    public static Map<Identifier, PacketHandler.Event<?>> getEvents() {
        var map = new HashMap<Identifier, PacketHandler.Event<?>>();
        if(Services.PLATFORM.isModLoaded(Compat.WaystonesId))
            map.putAll(WaystonesAdapter.getEvents());

        return map;
    }

}
