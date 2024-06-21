package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.storage.loot.RegisteredWaystoneLootNbtProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.providers.nbt.LootNbtProviderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LootProviderRegistry {

    private static final DeferredRegister<LootNbtProviderType> Register =
        DeferredRegister.create(BuiltInRegistries.LOOT_NBT_PROVIDER_TYPE.key(), Signpost.MOD_ID);

    public static final DeferredHolder<LootNbtProviderType, LootNbtProviderType> RegisteredWaystone =
        Register.register("waystone", () -> RegisteredWaystoneLootNbtProvider.providerType);

    public static void register(IEventBus bus){
        Register.register(bus);
    }

}
