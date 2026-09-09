package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.loot.ILootItemConditionRegistry;
import gollorum.signpost.minecraft.loot.PermissionCheck;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LootItemConditionRegistry implements ILootItemConditionRegistry {

    private static final DeferredRegister<MapCodec<? extends LootItemCondition>> Register =
        DeferredRegister.create(BuiltInRegistries.LOOT_CONDITION_TYPE.key(), Signpost.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends LootItemCondition>, MapCodec<PermissionCheck>> permissionCheck =
        Register.register("permission_check", PermissionCheck::createConditionCodec);

    public MapCodec<? extends LootItemCondition> getPermissionCheck(){
        return permissionCheck.get();
    }

    public static void register(IEventBus bus){
        Register.register(bus);
    }

}
