package gollorum.signpost.compat;

import com.telepathicgrunt.repurposedstructures.modinit.RSConditionsRegistry;
import gollorum.signpost.config.Config;

import java.util.function.Supplier;

public class RepurposedStructuresAdapter {

    public static void register() {
        RSConditionsRegistry.RS_JSON_CONDITIONS_REGISTRY.register(
            "signpost_config",
            RepurposedStructuresAdapter::villageGenerationCheck
        );
    }

    private static Supplier<Boolean> villageGenerationCheck() {
        return () -> !Config.INSTANCE.ServerConfig.isLoaded() || Config.INSTANCE.Server.worldGen().isVillageGenerationEnabled();
    }

}
