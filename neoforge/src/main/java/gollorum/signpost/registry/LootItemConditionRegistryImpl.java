package gollorum.signpost.registry;

import gollorum.signpost.minecraft.loot.ILootItemConditionRegistry;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public class LootItemConditionRegistryImpl implements ILootItemConditionRegistry {

    public LootItemConditionType getPermissionCheck(){
        return LootItemConditionRegistry.permissionCheck.get();
    }

}
