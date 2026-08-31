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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

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
                    pathProvider.json(modelType.getKey().location())
                )).toArray(CompletableFuture[]::new)
            )
        );
    }

    @Override
    public String getName() {
        return "Signpost Post Model Types";
    }

    private static FakeHolder mkModelType(
        String name,
        ResourceLocation postTexture, ResourceLocation mainTexture, ResourceLocation secondaryTexture,
        Ingredient signIngredient, Ingredient baseIngredient, Ingredient addSignIngredient,
        PostBlock.MaterialType materialType
    ) {
        return new FakeHolder(name, 
            new PostBlock.ModelType(
                materialType, signIngredient,
                expand(postTexture), expand(mainTexture), expand(secondaryTexture)
            ),
            baseIngredient,
            addSignIngredient
        );
    }

    private static Texture expand(ResourceLocation loc){
        return new Texture(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            loc.getNamespace(),
            loc.getPath().startsWith("block/") ? loc.getPath() : "block/"+loc.getPath()
        ));
    }

    public interface TaglistResolver<T> {
        HolderSet.Named<T> getOrThrow(TagKey<T> tagKey);
    }

    public static FakeHolder oak(TaglistResolver<Item> items) {
        return mkModelType("oak",
            ResourceLocation.parse("oak_log"),
            ResourceLocation.parse("stripped_oak_log"),
            ResourceLocation.parse("oak_log"),
            Ingredient.of(net.minecraft.world.item.Items.OAK_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.OAK_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.OAK_SIGN),
            PostBlock.MaterialType.Wood
        );
    }

    public static FakeHolder iron(TaglistResolver<Item> items) {
        return mkModelType("iron",
            ResourceLocation.parse("iron_block"),
            ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "iron"),
            ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "iron_dark"),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.SIGNS)),
            Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT),
            Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT),
            PostBlock.MaterialType.Metal
        );
    }

    public static FakeHolder stone(TaglistResolver<Item> items) {
        return mkModelType("stone",
            ResourceLocation.parse("stone"),
            ResourceLocation.parse("stone"),
            ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "stone_dark"),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.SIGNS)),
            Ingredient.of(net.minecraft.world.item.Items.STONE),
            Ingredient.of(net.minecraft.world.item.Items.STONE),
            PostBlock.MaterialType.Stone
        );
    }

    public static FakeHolder redMushroom(TaglistResolver<Item> items) {
        return mkModelType("red_mushroom",
            ResourceLocation.parse("red_mushroom_block"),
            ResourceLocation.parse("mushroom_stem"),
            ResourceLocation.parse("red_mushroom_block"),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.SIGNS)),
            Ingredient.of(net.minecraft.world.item.Items.RED_MUSHROOM_BLOCK),
            Ingredient.of(net.minecraft.world.item.Items.RED_MUSHROOM),
            PostBlock.MaterialType.Mushroom
        );
    }

    public static List<FakeHolder> getAll(HolderLookup.RegistryLookup<Item> items) {
        var ret = new ArrayList<FakeHolder>();

        ret.add(mkModelType("acacia",
            net.minecraft.resources.ResourceLocation.parse("acacia_log"),
            net.minecraft.resources.ResourceLocation.parse("stripped_acacia_log"),
            net.minecraft.resources.ResourceLocation.parse("acacia_log"),
            net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.ACACIA_SIGN),
            net.minecraft.world.item.crafting.Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.ACACIA_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.ACACIA_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("birch",
            ResourceLocation.parse("birch_log"),
            ResourceLocation.parse("stripped_birch_log"),
            ResourceLocation.parse("birch_log"),
            Ingredient.of(net.minecraft.world.item.Items.BIRCH_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.BIRCH_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.BIRCH_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(iron(items::getOrThrow));
        ret.add(mkModelType("jungle",
            ResourceLocation.parse("jungle_log"),
            ResourceLocation.parse("stripped_jungle_log"),
            ResourceLocation.parse("jungle_log"),
            Ingredient.of(net.minecraft.world.item.Items.JUNGLE_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.JUNGLE_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.JUNGLE_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(oak(items::getOrThrow));
        ret.add(mkModelType("darkoak",
            ResourceLocation.parse("dark_oak_log"),
            ResourceLocation.parse("stripped_dark_oak_log"),
            ResourceLocation.parse("dark_oak_log"),
            Ingredient.of(net.minecraft.world.item.Items.DARK_OAK_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.DARK_OAK_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.DARK_OAK_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("spruce",
            ResourceLocation.parse("spruce_log"),
            ResourceLocation.parse("stripped_spruce_log"),
            ResourceLocation.parse("spruce_log"),
            Ingredient.of(net.minecraft.world.item.Items.SPRUCE_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.SPRUCE_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.SPRUCE_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("mangrove",
            ResourceLocation.parse("mangrove_log"),
            ResourceLocation.parse("stripped_mangrove_log"),
            ResourceLocation.parse("mangrove_log"),
            Ingredient.of(net.minecraft.world.item.Items.MANGROVE_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.MANGROVE_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.MANGROVE_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("bamboo",
            ResourceLocation.parse("bamboo_block"),
            ResourceLocation.parse("stripped_bamboo_block"),
            ResourceLocation.parse("bamboo_block"),
            Ingredient.of(net.minecraft.world.item.Items.BAMBOO_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.BAMBOO_BLOCKS)),
            Ingredient.of(net.minecraft.world.item.Items.BAMBOO_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("cherry",
            ResourceLocation.parse("cherry_log"),
            ResourceLocation.parse("stripped_cherry_log"),
            ResourceLocation.parse("cherry_log"),
            Ingredient.of(net.minecraft.world.item.Items.CHERRY_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.CHERRY_LOGS)),
            Ingredient.of(net.minecraft.world.item.Items.CHERRY_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(stone(items::getOrThrow));
        ret.add(redMushroom(items::getOrThrow));
        ret.add(mkModelType("brown_mushroom",
            ResourceLocation.parse("brown_mushroom_block"),
            ResourceLocation.parse("mushroom_stem"),
            ResourceLocation.parse("brown_mushroom_block"),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.SIGNS)),
            Ingredient.of(net.minecraft.world.item.Items.BROWN_MUSHROOM_BLOCK),
            Ingredient.of(net.minecraft.world.item.Items.BROWN_MUSHROOM),
            PostBlock.MaterialType.Mushroom
        ));
        ret.add(mkModelType("warped",
            ResourceLocation.parse("warped_stem"),
            ResourceLocation.parse("stripped_warped_stem"),
            ResourceLocation.parse("warped_stem"),
            Ingredient.of(net.minecraft.world.item.Items.WARPED_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.WARPED_STEMS)),
            Ingredient.of(net.minecraft.world.item.Items.WARPED_SIGN),
            PostBlock.MaterialType.Wood
        ));
        ret.add(mkModelType("crimson",
            ResourceLocation.parse("crimson_stem"),
            ResourceLocation.parse("stripped_crimson_stem"),
            ResourceLocation.parse("crimson_stem"),
            Ingredient.of(net.minecraft.world.item.Items.CRIMSON_SIGN),
            Ingredient.of(items.getOrThrow(net.minecraft.tags.ItemTags.CRIMSON_STEMS)),
            Ingredient.of(Items.CRIMSON_SIGN),
            PostBlock.MaterialType.Wood
        ));
        Ingredient sandstone = Ingredient.of(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SMOOTH_SANDSTONE);
        ret.add(mkModelType("sandstone",
            ResourceLocation.parse("sandstone"),
            ResourceLocation.parse("stripped_jungle_log"),
            ResourceLocation.parse("sandstone_bottom"),
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
                ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, name));
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
//        public boolean is(ResourceLocation resourceLocation) {
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
//                ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, name)));
//        }
//
//        @Override
//        public Optional<ResourceKey<PostBlock.ModelType>> unwrapKey() {
//            return Optional.of(ResourceKey.create(
//                ModelTypeRegistry.REGISTRY_KEY,
//                ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, name)));
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
