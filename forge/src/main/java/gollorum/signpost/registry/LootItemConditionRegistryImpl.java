package gollorum.signpost.registry;

import gollorum.signpost.minecraft.loot.ILootItemConditionRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemConditionRegistryImpl implements ILootItemConditionRegistry {

    public MapCodec<? extends LootItemCondition> getPermissionCheck(){
        return LootItemConditionRegistry.permissionCheck.get();
    }

}
