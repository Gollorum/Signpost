package gollorum.signpost.minecraft.loot;

import gollorum.signpost.Signpost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;

import java.util.function.BiConsumer;

public class LootEntries {

    public static LootPoolEntryType POST_BLOCK_LOOT =
        new LootPoolEntryType(PostBlockPartDropLoot.MAP_CODEC);

    public static void register(BiConsumer<ResourceLocation, LootPoolEntryType> register) {
        register.accept(ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "post_data"), POST_BLOCK_LOOT);
    }

}
