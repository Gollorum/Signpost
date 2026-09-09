package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.loot.ILootItemConditionRegistry;
import gollorum.signpost.minecraft.loot.PermissionCheck;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemConditionRegistryImpl implements ILootItemConditionRegistry {


    public static final MapCodec<? extends LootItemCondition> permissionCheck = PermissionCheck.createConditionCodec();

    public MapCodec<? extends LootItemCondition> getPermissionCheck(){
        return permissionCheck;
    }

    public static void register(){
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "permission_check"), permissionCheck);
    }

}
