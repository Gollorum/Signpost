package gollorum.signpost.minecraft.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.storage.loot.PermissionCheck;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class LootItemConditionRegistry {

    private static final DeferredRegister<LootItemConditionType> Register =
        DeferredRegister.create(BuiltInRegistries.LOOT_CONDITION_TYPE.key(), Signpost.MOD_ID);

    public static final RegistryObject<LootItemConditionType> permissionCheck =
        Register.register("permission_check", PermissionCheck::createConditionType);

    public static void register(IEventBus bus){
        Register.register(bus);
    }

}
