package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.storage.loot.RegisteredWaystoneLootDataFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LootProviderRegistry {

    private static final DeferredRegister<LootItemFunctionType<?>> Register =
        DeferredRegister.create(BuiltInRegistries.LOOT_FUNCTION_TYPE.key(), Signpost.MOD_ID);

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<RegisteredWaystoneLootDataFunction>> RegisteredWaystone =
        Register.register("waystone", () -> RegisteredWaystoneLootDataFunction.TYPE);

    public static void register(IEventBus bus){
        Register.register(bus);
    }

}
