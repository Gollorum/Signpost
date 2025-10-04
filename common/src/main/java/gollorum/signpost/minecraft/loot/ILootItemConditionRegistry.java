package gollorum.signpost.minecraft.loot;

import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public interface ILootItemConditionRegistry {
    LootItemConditionType getPermissionCheck();
}
