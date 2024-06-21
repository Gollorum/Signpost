package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.storage.loot.RegisteredWaystoneLootNbtProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.providers.nbt.LootNbtProviderType;

public class LootProviderRegistry {

    public static final LootNbtProviderType RegisteredWaystone = RegisteredWaystoneLootNbtProvider.providerType;

    public static void register(){
        Registry.register(BuiltInRegistries.LOOT_NBT_PROVIDER_TYPE, new ResourceLocation(Signpost.MOD_ID, "waystone"), RegisteredWaystone);
    }

}
