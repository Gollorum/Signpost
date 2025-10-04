package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.loot.RegisteredWaystoneLootDataFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class LootProviderRegistry {

    private static final DeferredRegister<LootItemFunctionType<?>> Register =
        DeferredRegister.create(BuiltInRegistries.LOOT_FUNCTION_TYPE.key(), Signpost.MOD_ID);

    public static final RegistryObject<LootItemFunctionType<RegisteredWaystoneLootDataFunction>> RegisteredWaystone =
        Register.register("waystone", () -> RegisteredWaystoneLootDataFunction.TYPE);

    public static void register(IEventBus bus){
        Register.register(bus);
    }

}
