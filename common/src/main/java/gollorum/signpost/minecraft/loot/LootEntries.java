package gollorum.signpost.minecraft.loot;

import com.mojang.serialization.MapCodec;
import gollorum.signpost.Signpost;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

import java.util.function.BiConsumer;

public class LootEntries {

    // 26.1 unrolled the loot types: LOOT_POOL_ENTRY_TYPE holds the MapCodec itself, so the
    // LootPoolEntryType wrapper this used to construct no longer exists.
    public static MapCodec<PostBlockPartDropLoot> POST_BLOCK_LOOT =
        PostBlockPartDropLoot.MAP_CODEC;

    public static void register(BiConsumer<Identifier, MapCodec<? extends LootPoolEntryContainer>> register) {
        register.accept(Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "post_data"), POST_BLOCK_LOOT);
    }

}
