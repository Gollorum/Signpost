package gollorum.signpost.minecraft.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public interface ILootItemConditionRegistry {
    // 26.1 unrolled the loot types: the registry holds the MapCodec directly, and the
    // LootItemConditionType wrapper is gone.
    MapCodec<? extends LootItemCondition> getPermissionCheck();
}
