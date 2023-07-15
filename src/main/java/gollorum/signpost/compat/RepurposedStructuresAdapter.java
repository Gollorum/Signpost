package gollorum.signpost.compat;

import com.telepathicgrunt.repurposedstructures.misc.structurepiececounter.JSONConditionsRegistry;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.config.Config;
import net.minecraft.resources.ResourceLocation;

public class RepurposedStructuresAdapter {

    public static void register() {
        JSONConditionsRegistry.RS_JSON_CONDITIONS_REGISTRY.get().register(
            new ResourceLocation(Signpost.MOD_ID, "config"),
            () -> !Config.ServerConfig.isLoaded() || Config.Server.worldGen.isVillageGenerationEnabled()
        );
    }

}
