package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.loot.PermissionCheck;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class LootItemConditionRegistry {

    private static final DeferredRegister<MapCodec<? extends LootItemCondition>> Register =
        DeferredRegister.create(BuiltInRegistries.LOOT_CONDITION_TYPE.key(), Signpost.MOD_ID);

    public static final RegistryObject<MapCodec<PermissionCheck>> permissionCheck =
        Register.register("permission_check", PermissionCheck::createConditionCodec);

    public static void register(BusGroup bus){
        Register.register(bus);
    }
}
