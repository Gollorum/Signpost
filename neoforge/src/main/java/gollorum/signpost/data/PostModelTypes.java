package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.data.ModelTypeRegistry;
import gollorum.signpost.minecraft.utils.Texture;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PostModelTypes implements DataProvider {

    private final PackOutput packOutput;
    private final CompletableFuture<HolderLookup.Provider> registryAccess;

    public PostModelTypes(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registryAccess) {
        this.packOutput = packOutput;
        this.registryAccess = registryAccess;
    }

    public static void run(BootstrapContext<PostBlock.ModelType> context) {
        for (var modelType : getAll(context.lookup(Registries.ITEM))) {
            context.register(
                modelType.getKey(),
                modelType.value
            );
        }
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        var pathProvider = packOutput.createRegistryElementsPathProvider(ModelTypeRegistry.REGISTRY_KEY);
        return registryAccess.thenCompose(provider ->
            CompletableFuture.allOf(getAll(provider.lookupOrThrow(Registries.ITEM)).stream().map(modelType ->
                DataProvider.saveStable(
                    cachedOutput,
                    provider,
                    PostBlock.ModelType.CODEC,
                    modelType.value,
                    pathProvider.json(modelType.getKey().identifier())
                )).toArray(CompletableFuture[]::new)
            )
        );
    }

    @Override
    public String getName() {
        return "Signpost Post Model Types";
    }

    /**
     * @param mapColor the colour this type draws as on a map. Every type states it outright; the model type
     *                 carries it because it used to come from the {@code Block.Properties} of the block each
     *                 type had of its own before 2.04. For the sixteen types that existed back then the value
     *                 is the one those blocks used, and changing it would recolour signposts already placed in
     *                 existing worlds - so leave those alone. New types are free to pick whatever fits.
     */
    private static FakeHolder mkModelType(
        String name,
        Identifier postTexture, Identifier mainTexture, Identifier secondaryTexture,
        Ingredient signIngredient, Ingredient baseIngredient, Ingredient addSignIngredient,
        PostBlock.MaterialType materialType, MapColor mapColor
    ) {
        return new FakeHolder(name,
            new PostBlock.ModelType(
                materialType, addSignIngredient,
                expand(postTexture), expand(mainTexture), expand(secondaryTexture),
                Optional.of(mapColor)
            ),
            signIngredient,
            baseIngredient
        );
    }

    private static Texture expand(Identifier loc){
        return new Texture(net.minecraft.resources.Identifier.fromNamespaceAndPath(
            loc.getNamespace(),
            loc.getPath().startsWith("block/") ? loc.getPath() : "block/"+loc.getPath()
        ));
    }

    public interface TaglistResolver<T> {
        HolderSet.Named<T> getOrThrow(TagKey<T> tagKey);
    }

    public static FakeHolder oak(TaglistResolver<Item> items) {
        return mkModelType("oak",
            Identifier.parse("oak_log"),
            Identifier.parse("stripped_oak_log"),
            Identifier.parse("oak_log"),
            Ingredient.of(net.minecraft.world.item.Items.OAK_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.OAK_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.OAK_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.WOOD
        );
    }

    public static FakeHolder iron(TaglistResolver<Item> items) {
        return mkModelType("iron",
            Identifier.parse("iron_block"),
            Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "iron"),
            Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "iron_dark"),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.SIGNS)),
            Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT),
            Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT),
            PostBlock.MaterialType.Metal,
            MapColor.METAL
        );
    }

    public static FakeHolder stone(TaglistResolver<Item> items) {
        return mkModelType("stone",
            Identifier.parse("stone"),
            Identifier.parse("stone"),
            Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "stone_dark"),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.SIGNS)),
            Ingredient.of(net.minecraft.world.item.Items.STONE),
            Ingredient.of(net.minecraft.world.item.Items.STONE),
            PostBlock.MaterialType.Stone,
            MapColor.STONE
        );
    }

    public static FakeHolder redMushroom(TaglistResolver<Item> items) {
        return mkModelType("red_mushroom",
            Identifier.parse("red_mushroom_block"),
            Identifier.parse("mushroom_stem"),
            Identifier.parse("red_mushroom_block"),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.SIGNS)),
            Ingredient.of(net.minecraft.world.item.Items.RED_MUSHROOM_BLOCK),
            Ingredient.of(net.minecraft.world.item.Items.RED_MUSHROOM),
            PostBlock.MaterialType.Mushroom,
            MapColor.COLOR_RED
        );
    }

    public static List<FakeHolder> getAll(HolderGetter<Item> items) {
        var ret = new ArrayList<FakeHolder>();

        ret.add(mkModelType("acacia",
            net.minecraft.resources.Identifier.parse("acacia_log"),
            net.minecraft.resources.Identifier.parse("stripped_acacia_log"),
            net.minecraft.resources.Identifier.parse("acacia_log"),
            net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.ACACIA_SIGN),
            net.minecraft.world.item.crafting.Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.ACACIA_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.ACACIA_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.COLOR_ORANGE
        ));
        ret.add(mkModelType("birch",
            Identifier.parse("birch_log"),
            Identifier.parse("stripped_birch_log"),
            Identifier.parse("birch_log"),
            Ingredient.of(net.minecraft.world.item.Items.BIRCH_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.BIRCH_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.BIRCH_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.SAND
        ));
        ret.add(iron(items::getOrThrow));
        ret.add(mkModelType("jungle",
            Identifier.parse("jungle_log"),
            Identifier.parse("stripped_jungle_log"),
            Identifier.parse("jungle_log"),
            Ingredient.of(net.minecraft.world.item.Items.JUNGLE_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.JUNGLE_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.JUNGLE_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.DIRT
        ));
        ret.add(oak(items::getOrThrow));
        ret.add(mkModelType("darkoak",
            Identifier.parse("dark_oak_log"),
            Identifier.parse("stripped_dark_oak_log"),
            Identifier.parse("dark_oak_log"),
            Ingredient.of(net.minecraft.world.item.Items.DARK_OAK_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.DARK_OAK_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.DARK_OAK_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.COLOR_BROWN
        ));
        ret.add(mkModelType("spruce",
            Identifier.parse("spruce_log"),
            Identifier.parse("stripped_spruce_log"),
            Identifier.parse("spruce_log"),
            Ingredient.of(net.minecraft.world.item.Items.SPRUCE_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.SPRUCE_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.SPRUCE_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.PODZOL
        ));
        ret.add(mkModelType("mangrove",
            Identifier.parse("mangrove_log"),
            Identifier.parse("stripped_mangrove_log"),
            Identifier.parse("mangrove_log"),
            Ingredient.of(net.minecraft.world.item.Items.MANGROVE_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.MANGROVE_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.MANGROVE_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.COLOR_RED
        ));
        ret.add(mkModelType("bamboo",
            Identifier.parse("bamboo_block"),
            Identifier.parse("stripped_bamboo_block"),
            Identifier.parse("bamboo_block"),
            Ingredient.of(net.minecraft.world.item.Items.BAMBOO_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.BAMBOO_BLOCKS)),
            Ingredient.of(net.minecraft.world.item.Items.BAMBOO_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.COLOR_YELLOW
        ));
        ret.add(mkModelType("cherry",
            Identifier.parse("cherry_log"),
            Identifier.parse("stripped_cherry_log"),
            Identifier.parse("cherry_log"),
            Ingredient.of(net.minecraft.world.item.Items.CHERRY_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.CHERRY_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.CHERRY_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.TERRACOTTA_WHITE
        ));
        ret.add(stone(items::getOrThrow));
        ret.add(redMushroom(items::getOrThrow));
        ret.add(mkModelType("brown_mushroom",
            Identifier.parse("brown_mushroom_block"),
            Identifier.parse("mushroom_stem"),
            Identifier.parse("brown_mushroom_block"),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.SIGNS)),
            Ingredient.of(net.minecraft.world.item.Items.BROWN_MUSHROOM_BLOCK),
            Ingredient.of(net.minecraft.world.item.Items.BROWN_MUSHROOM),
            PostBlock.MaterialType.Mushroom,
            MapColor.DIRT
        ));
        ret.add(mkModelType("warped",
            Identifier.parse("warped_stem"),
            Identifier.parse("stripped_warped_stem"),
            Identifier.parse("warped_stem"),
            Ingredient.of(net.minecraft.world.item.Items.WARPED_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.WARPED_STEMS)),
            Ingredient.of(net.minecraft.world.item.Items.WARPED_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.WARPED_STEM
        ));
        ret.add(mkModelType("crimson",
            Identifier.parse("crimson_stem"),
            Identifier.parse("stripped_crimson_stem"),
            Identifier.parse("crimson_stem"),
            Ingredient.of(net.minecraft.world.item.Items.CRIMSON_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.CRIMSON_STEMS)),
            Ingredient.of(Items.CRIMSON_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.CRIMSON_STEM
        ));
        Ingredient sandstone = Ingredient.of(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SMOOTH_SANDSTONE);
        ret.add(mkModelType("sandstone",
            Identifier.parse("sandstone"),
            Identifier.parse("red_sandstone_bottom"),
            Identifier.parse("sandstone_bottom"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            sandstone,
            sandstone,
            PostBlock.MaterialType.Stone,
            MapColor.STONE
        ));
        Ingredient redSandstone = Ingredient.of(Blocks.RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE);
        ret.add(mkModelType("red_sandstone",
            Identifier.parse("red_sandstone"),
            Identifier.parse("sandstone_bottom"),
            Identifier.parse("red_sandstone_bottom"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            redSandstone,
            redSandstone,
            PostBlock.MaterialType.Stone,
            MapColor.COLOR_ORANGE
        ));
        ret.add(mkModelType("pale_oak",
            Identifier.parse("pale_oak_log"),
            Identifier.parse("stripped_pale_oak_log"),
            Identifier.parse("pale_oak_log"),
            Ingredient.of(Items.PALE_OAK_SIGN),
            Ingredient.of(items.getOrThrow(ItemTags.PALE_OAK_LOGS)),
            Ingredient.of(Items.PALE_OAK_SIGN),
            PostBlock.MaterialType.Wood,
            MapColor.QUARTZ
        ));
        Ingredient granite = Ingredient.of(Blocks.GRANITE, Blocks.POLISHED_GRANITE);
        ret.add(mkModelType("granite",
            Identifier.parse("granite"),
            Identifier.parse("polished_diorite"),
            Identifier.parse("granite"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            granite,
            granite,
            PostBlock.MaterialType.Stone,
            MapColor.DIRT
        ));
        Ingredient diorite = Ingredient.of(Blocks.DIORITE, Blocks.POLISHED_DIORITE);
        ret.add(mkModelType("diorite",
            Identifier.parse("diorite"),
            Identifier.parse("stripped_pale_oak_log"),
            Identifier.parse("diorite"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            diorite,
            diorite,
            PostBlock.MaterialType.Stone,
            MapColor.QUARTZ
        ));
        Ingredient andesite = Ingredient.of(Blocks.ANDESITE, Blocks.POLISHED_ANDESITE);
        ret.add(mkModelType("andesite",
            Identifier.parse("andesite"),
            Identifier.parse("polished_diorite"),
            Identifier.parse("andesite"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            andesite,
            andesite,
            PostBlock.MaterialType.Stone,
            MapColor.STONE
        ));
        Ingredient tuff = Ingredient.of(Blocks.TUFF, Blocks.POLISHED_TUFF, Blocks.TUFF_BRICKS, Blocks.CHISELED_TUFF, Blocks.CHISELED_TUFF_BRICKS);
        ret.add(mkModelType("tuff",
            Identifier.parse("chiseled_tuff_bricks"),
            Identifier.parse("polished_diorite"),
            Identifier.parse("chiseled_tuff_bricks"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            tuff,
            tuff,
            PostBlock.MaterialType.Stone,
            MapColor.TERRACOTTA_GRAY
        ));
        Ingredient deepslate = Ingredient.of(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.CHISELED_DEEPSLATE);
        ret.add(mkModelType("deepslate",
            Identifier.parse("deepslate_tiles"),
            Identifier.parse("polished_basalt_side"),
            Identifier.parse("deepslate_tiles"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            deepslate,
            deepslate,
            PostBlock.MaterialType.Stone,
            MapColor.DEEPSLATE
        ));
        Ingredient basalt = Ingredient.of(Blocks.BASALT, Blocks.POLISHED_BASALT, Blocks.SMOOTH_BASALT);
        ret.add(mkModelType("basalt",
            Identifier.parse("basalt_side"),
            Identifier.parse("polished_basalt_side"),
            Identifier.parse("basalt_side"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            basalt,
            basalt,
            PostBlock.MaterialType.Stone,
            MapColor.COLOR_BLACK
        ));
        Ingredient blackstone = Ingredient.of(Blocks.BLACKSTONE, Blocks.GILDED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS);
        ret.add(mkModelType("blackstone",
            Identifier.parse("blackstone"),
            Identifier.parse("polished_basalt_side"),
            Identifier.parse("blackstone"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            blackstone,
            blackstone,
            PostBlock.MaterialType.Stone,
            MapColor.COLOR_BLACK
        ));
        Ingredient netherBricks = Ingredient.of(Blocks.NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS, Blocks.RED_NETHER_BRICKS);
        ret.add(mkModelType("nether_bricks",
            Identifier.parse("nether_bricks"),
            Identifier.parse("netherrack"),
            Identifier.parse("nether_bricks"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            netherBricks,
            netherBricks,
            PostBlock.MaterialType.Stone,
            MapColor.NETHER
        ));
        Ingredient prismarine = Ingredient.of(Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE);
        ret.add(mkModelType("prismarine",
            Identifier.parse("dark_prismarine"),
            Identifier.parse("prismarine_bricks"),
            Identifier.parse("dark_prismarine"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            prismarine,
            prismarine,
            PostBlock.MaterialType.Stone,
            MapColor.COLOR_CYAN
        ));
        ret.add(mkModelType("amethyst",
            Identifier.parse("amethyst_block"),
            Identifier.parse("pearlescent_froglight_side"),
            Identifier.parse("amethyst_block"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            Ingredient.of(Blocks.AMETHYST_BLOCK),
            Ingredient.of(Items.AMETHYST_SHARD),
            PostBlock.MaterialType.Stone,
            MapColor.COLOR_PURPLE
        ));

        return ret;
    }

    public record FakeHolder(String name, PostBlock.ModelType value, Ingredient signIngredient, Ingredient baseIngredient) {//implements Holder<PostBlock.ModelType> {
        public ResourceKey<PostBlock.ModelType> getKey() {
            return ResourceKey.create(
                ModelTypeRegistry.REGISTRY_KEY,
                Identifier.fromNamespaceAndPath(Signpost.MOD_ID, name));
        }
    }
}
