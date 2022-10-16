package gollorum.signpost.compat;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.config.Config;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class RepurposedStructuresAdapter {

    public static void register() {
        Registry.REGISTRY.getOptional(new ResourceLocation(Compat.RepurposedStructuresId, "json_conditions"))
            .ifPresent(registry -> Registry.register(
                (Registry<Supplier<Boolean>>)registry,
                new ResourceLocation(Signpost.MOD_ID, "config"),
                Config.Server.worldGen::isVillageGenerationEnabled
            ));
    }

}
