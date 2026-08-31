package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.data.ModelTypeRegistry;
import gollorum.signpost.minecraft.utils.Texture;
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
        for (var modelType : getAll(context.registryLookup(Registries.ITEM).orElseThrow())) {
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
     * What each built-in type used to draw as on a map, back when it was its own block with its own
     * {@code Block.Properties}. Taken straight off the old PropertiesUtil values so that no existing signpost
     * changes colour.
     */
    private static final Map<String, MapColor> mapColors = Map.ofEntries(
        Map.entry("oak", MapColor.WOOD),
        Map.entry("darkoak", MapColor.COLOR_BROWN),
        Map.entry("spruce", MapColor.PODZOL),
        Map.entry("birch", MapColor.SAND),
        Map.entry("jungle", MapColor.DIRT),
        Map.entry("acacia", MapColor.COLOR_ORANGE),
        Map.entry("mangrove", MapColor.COLOR_RED),
        Map.entry("bamboo", MapColor.COLOR_YELLOW),
        Map.entry("cherry", MapColor.TERRACOTTA_WHITE),
        Map.entry("warped", MapColor.WARPED_STEM),
        Map.entry("crimson", MapColor.CRIMSON_STEM),
        Map.entry("stone", MapColor.STONE),
        Map.entry("sandstone", MapColor.STONE),
        Map.entry("iron", MapColor.METAL),
        Map.entry("red_mushroom", MapColor.COLOR_RED),
        Map.entry("brown_mushroom", MapColor.DIRT)
    );

    private static FakeHolder mkModelType(
        String name,
        Identifier postTexture, Identifier mainTexture, Identifier secondaryTexture,
        Ingredient signIngredient, Ingredient baseIngredient, Ingredient addSignIngredient,
        PostBlock.MaterialType materialType
    ) {
        return new FakeHolder(name, 
            new PostBlock.ModelType(
                materialType, signIngredient,
                expand(postTexture), expand(mainTexture), expand(secondaryTexture),
                Optional.ofNullable(mapColors.get(name))
            ),
            baseIngredient,
            addSignIngredient
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
            PostBlock.MaterialType.Wood
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
            PostBlock.MaterialType.Metal
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
            PostBlock.MaterialType.Stone
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
            PostBlock.MaterialType.Mushroom
        );
    }

    public static List<FakeHolder> getAll(HolderLookup.RegistryLookup<Item> items) {
        var ret = new ArrayList<FakeHolder>();

        ret.add(mkModelType("acacia",
            net.minecraft.resources.Identifier.parse("acacia_log"),
            net.minecraft.resources.Identifier.parse("stripped_acacia_log"),
            net.minecraft.resources.Identifier.parse("acacia_log"),
            net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.ACACIA_SIGN),
            net.minecraft.world.item.crafting.Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.ACACIA_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.ACACIA_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("birch",
            Identifier.parse("birch_log"),
            Identifier.parse("stripped_birch_log"),
            Identifier.parse("birch_log"),
            Ingredient.of(net.minecraft.world.item.Items.BIRCH_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.BIRCH_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.BIRCH_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(iron(items::getOrThrow));
        ret.add(mkModelType("jungle",
            Identifier.parse("jungle_log"),
            Identifier.parse("stripped_jungle_log"),
            Identifier.parse("jungle_log"),
            Ingredient.of(net.minecraft.world.item.Items.JUNGLE_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.JUNGLE_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.JUNGLE_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(oak(items::getOrThrow));
        ret.add(mkModelType("darkoak",
            Identifier.parse("dark_oak_log"),
            Identifier.parse("stripped_dark_oak_log"),
            Identifier.parse("dark_oak_log"),
            Ingredient.of(net.minecraft.world.item.Items.DARK_OAK_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.DARK_OAK_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.DARK_OAK_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("spruce",
            Identifier.parse("spruce_log"),
            Identifier.parse("stripped_spruce_log"),
            Identifier.parse("spruce_log"),
            Ingredient.of(net.minecraft.world.item.Items.SPRUCE_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.SPRUCE_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.SPRUCE_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("mangrove",
            Identifier.parse("mangrove_log"),
            Identifier.parse("stripped_mangrove_log"),
            Identifier.parse("mangrove_log"),
            Ingredient.of(net.minecraft.world.item.Items.MANGROVE_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.MANGROVE_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.MANGROVE_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("bamboo",
            Identifier.parse("bamboo_block"),
            Identifier.parse("stripped_bamboo_block"),
            Identifier.parse("bamboo_block"),
            Ingredient.of(net.minecraft.world.item.Items.BAMBOO_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.BAMBOO_BLOCKS)),
            Ingredient.of(net.minecraft.world.item.Items.BAMBOO_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("cherry",
            Identifier.parse("cherry_log"),
            Identifier.parse("stripped_cherry_log"),
            Identifier.parse("cherry_log"),
            Ingredient.of(net.minecraft.world.item.Items.CHERRY_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.CHERRY_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.CHERRY_SIGN),
            PostBlock.MaterialType.Wood
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
            PostBlock.MaterialType.Mushroom
        ));
        ret.add(mkModelType("warped",
            Identifier.parse("warped_stem"),
            Identifier.parse("stripped_warped_stem"),
            Identifier.parse("warped_stem"),
            Ingredient.of(net.minecraft.world.item.Items.WARPED_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.WARPED_STEMS)),
            Ingredient.of(net.minecraft.world.item.Items.WARPED_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("crimson",
            Identifier.parse("crimson_stem"),
            Identifier.parse("stripped_crimson_stem"),
            Identifier.parse("crimson_stem"),
            Ingredient.of(net.minecraft.world.item.Items.CRIMSON_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.CRIMSON_STEMS)),
            Ingredient.of(Items.CRIMSON_SIGN),
            PostBlock.MaterialType.Wood
        ));
        Ingredient sandstone = Ingredient.of(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SMOOTH_SANDSTONE);
        ret.add(mkModelType("sandstone",
            Identifier.parse("sandstone"),
            Identifier.parse("stripped_jungle_log"),
            Identifier.parse("sandstone_bottom"),
            Ingredient.of(items.getOrThrow(ItemTags.SIGNS)),
            sandstone,
            sandstone,
            PostBlock.MaterialType.Stone
        ));

        return ret;
    }

    public record FakeHolder(String name, PostBlock.ModelType value, Ingredient signIngredient, Ingredient baseIngredient) {//implements Holder<PostBlock.ModelType> {
        public ResourceKey<PostBlock.ModelType> getKey() {
            return ResourceKey.create(
                ModelTypeRegistry.REGISTRY_KEY,
                Identifier.fromNamespaceAndPath(Signpost.MOD_ID, name));
        }
//        @Override
//        public PostBlock.ModelType value() {
//            return value;
//        }
//
//        @Override
//        public boolean isBound() {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public boolean is(Identifier resourceLocation) {
//            return false;
//        }
//
//        @Override
//        public boolean is(ResourceKey<PostBlock.ModelType> resourceKey) {
//            return false;
//        }
//
//        @Override
//        public boolean is(Predicate<ResourceKey<PostBlock.ModelType>> predicate) {
//            return false;
//        }
//
//        @Override
//        public boolean is(TagKey<PostBlock.ModelType> tagKey) {
//            return false;
//        }
//
//        @Override
//        public boolean is(Holder<PostBlock.ModelType> holder) {
//            return false;
//        }
//
//        @Override
//        public Stream<TagKey<PostBlock.ModelType>> tags() {
//            return Stream.empty();
//        }
//
//        @Override
//        public Either<ResourceKey<PostBlock.ModelType>, PostBlock.ModelType> unwrap() {
//            return Either.left(ResourceKey.create(
//                ModelTypeRegistry.REGISTRY_KEY,
//                Identifier.fromNamespaceAndPath(Signpost.MOD_ID, name)));
//        }
//
//        @Override
//        public Optional<ResourceKey<PostBlock.ModelType>> unwrapKey() {
//            return Optional.of(ResourceKey.create(
//                ModelTypeRegistry.REGISTRY_KEY,
//                Identifier.fromNamespaceAndPath(Signpost.MOD_ID, name)));
//        }
//
//        @Override
//        public Kind kind() {
//            return Kind.REFERENCE;
//        }
//
//        @Override
//        public boolean canSerializeIn(HolderOwner<PostBlock.ModelType> holderOwner) {
//            return true;
//        }
    }
}
