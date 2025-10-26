package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.loot.ILootItemConditionRegistry;
import gollorum.signpost.minecraft.loot.PermissionCheck;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public class LootItemConditionRegistryImpl implements ILootItemConditionRegistry {


    public static final LootItemConditionType permissionCheck = PermissionCheck.createConditionType();

    public LootItemConditionType getPermissionCheck(){
        return permissionCheck;
    }

    public static void register(){
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "permission_check"), permissionCheck);
    }

}
