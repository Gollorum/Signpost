package gollorum.signpost.minecraft.loot;

import gollorum.signpost.Signpost;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;

import java.util.function.BiConsumer;

public class LootEntries {

    public static LootPoolEntryType POST_BLOCK_LOOT =
        new LootPoolEntryType(PostBlockPartDropLoot.MAP_CODEC);

    public static void register(BiConsumer<Identifier, LootPoolEntryType> register) {
        register.accept(Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "post_data"), POST_BLOCK_LOOT);
    }

}
