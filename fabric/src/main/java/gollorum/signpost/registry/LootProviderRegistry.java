package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.storage.loot.RegisteredWaystoneLootDataFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.providers.nbt.LootNbtProviderType;

public class LootProviderRegistry {

    public static final LootItemFunctionType<RegisteredWaystoneLootDataFunction> RegisteredWaystone = RegisteredWaystoneLootDataFunction.TYPE;

    public static void register() {
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "waystone"), RegisteredWaystone);
    }

}
