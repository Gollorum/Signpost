package gollorum.signpost.minecraft.storage.loot;

import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public interface ILootItemConditionRegistry {
    LootItemConditionType getPermissionCheck();
}
