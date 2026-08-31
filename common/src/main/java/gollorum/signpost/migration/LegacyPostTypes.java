package gollorum.signpost.migration;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The post blocks and items Signpost registered up to 2.03.x, back when every post type was its own block.
 *
 * <p>None of them is registered any more. They are translated away by {@link SignpostDataFixes}, which is only
 * possible because Signpost never shipped for the current Minecraft version: every world that can contain
 * these ids was therefore written by an older Minecraft, so Minecraft's own data fixer chain runs over it and
 * mod fixes hung off the end of that chain get their turn. A world already on the current Minecraft data
 * version is never re-fixed - the renames are idempotent anyway, so a second pass would be harmless.
 *
 * <p>{@code post_stone} is deliberately absent: that id is reused as the {@link PostBlock.MaterialType#Stone}
 * block and its old model type is that material's default, so nothing about it has to change.
 */
public final class LegacyPostTypes {

    public record Entry(PostBlock.MaterialType materialType, String modelTypeName) {
        public String currentBlockId() {
            return Signpost.MOD_ID + ":" + materialType.blockRegistryName;
        }
        public String modelTypeId() {
            return Signpost.MOD_ID + ":" + modelTypeName;
        }
    }

    private static void put(Map<String, Entry> into, String legacyName, PostBlock.MaterialType materialType, String modelTypeName) {
        into.put(Signpost.MOD_ID + ":" + PostBlock.REGISTRY_NAME + "_" + legacyName, new Entry(materialType, modelTypeName));
    }

    /** Keyed by the full legacy id; the block and the item always shared it. */
    public static final Map<String, Entry> BY_LEGACY_ID;
    static {
        var map = new LinkedHashMap<String, Entry>();
        put(map, "oak", PostBlock.MaterialType.Wood, "oak");
        put(map, "birch", PostBlock.MaterialType.Wood, "birch");
        put(map, "spruce", PostBlock.MaterialType.Wood, "spruce");
        put(map, "jungle", PostBlock.MaterialType.Wood, "jungle");
        put(map, "dark_oak", PostBlock.MaterialType.Wood, "darkoak");
        put(map, "acacia", PostBlock.MaterialType.Wood, "acacia");
        put(map, "mangrove", PostBlock.MaterialType.Wood, "mangrove");
        put(map, "bamboo", PostBlock.MaterialType.Wood, "bamboo");
        put(map, "cherry", PostBlock.MaterialType.Wood, "cherry");
        put(map, "warped", PostBlock.MaterialType.Wood, "warped");
        put(map, "crimson", PostBlock.MaterialType.Wood, "crimson");
        put(map, "iron", PostBlock.MaterialType.Metal, "iron");
        put(map, "sandstone", PostBlock.MaterialType.Stone, "sandstone");
        put(map, "brown_mushroom", PostBlock.MaterialType.Mushroom, "brown_mushroom");
        put(map, "red_mushroom", PostBlock.MaterialType.Mushroom, "red_mushroom");
        BY_LEGACY_ID = Map.copyOf(map);
    }

    /**
     * The recipe id a post type has always had. Recipe ids are player data too - they are what the recipe book
     * remembers - so the recipe for a spruce post stays {@code signpost:post_spruce} even though nothing is
     * called that any more. Only {@code darkoak} would otherwise have drifted, its block having been
     * {@code post_dark_oak} while its model type is {@code darkoak}.
     */
    public static String recipeIdFor(String modelTypeName) {
        for (var entry : BY_LEGACY_ID.entrySet())
            if (entry.getValue().modelTypeName().equals(modelTypeName))
                return entry.getKey().substring(Signpost.MOD_ID.length() + 1);
        return PostBlock.REGISTRY_NAME + "_" + modelTypeName;
    }

    /** Maps a legacy block or item id onto the material block that replaced it; anything else is left alone. */
    public static String rename(String id) {
        Entry entry = BY_LEGACY_ID.get(id);
        return entry == null ? id : entry.currentBlockId();
    }

    private LegacyPostTypes() {}

}
