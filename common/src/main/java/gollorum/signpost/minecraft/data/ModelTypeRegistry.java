package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.utils.Texture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class ModelTypeRegistry {

    public static final ResourceKey<Registry<PostBlock.ModelType>> REGISTRY_KEY = ResourceKey.createRegistryKey(
        ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "post_model_types")
    );

    public static Stream<PostBlock.ModelType> getAllModelTypes(HolderLookup.Provider registryAccess) {
        return registryAccess.lookupOrThrow(REGISTRY_KEY).listElements().map(Holder.Reference::value);
    }

    public static Stream<Holder<PostBlock.ModelType>> getAllModelTypeHolders(HolderLookup.Provider registryAccess) {
        return registryAccess.lookupOrThrow(REGISTRY_KEY).listElements().map(it -> it);
    }

    public static final PostBlock.ModelType WOOD_FALLBACK = new PostBlock.ModelType(
        PostBlock.MaterialType.Wood,
        Ingredient.of(net.minecraft.world.item.Items.OAK_SIGN),
        new Texture(ResourceLocation.parse("block/oak_log")),
        new Texture(ResourceLocation.parse("block/stripped_oak_log")),
        new Texture(ResourceLocation.parse("block/oak_log"))
    );

    public static final PostBlock.ModelType STONE_FALLBACK = new PostBlock.ModelType(
        PostBlock.MaterialType.Stone,
        Ingredient.of(net.minecraft.world.item.Items.STONE),
        new Texture(ResourceLocation.parse("block/stone")),
        new Texture(ResourceLocation.parse("block/stone")),
        new Texture(ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/stone_dark"))
    );

    public static final PostBlock.ModelType METAL_FALLBACK = new PostBlock.ModelType(
        PostBlock.MaterialType.Metal,
        Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT),
        new Texture(ResourceLocation.parse("block/iron_block")),
        new Texture(ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/iron")),
        new Texture(ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/iron_dark"))
    );

    public static final PostBlock.ModelType MUSHROOM_FALLBACK = new PostBlock.ModelType(
        PostBlock.MaterialType.Mushroom,
        Ingredient.of(net.minecraft.world.item.Items.RED_MUSHROOM),
        new Texture(ResourceLocation.parse("block/red_mushroom_block")),
        new Texture(ResourceLocation.parse("block/mushroom_stem")),
        new Texture(ResourceLocation.parse("block/red_mushroom_block"))
    );

    public static PostBlock.ModelType getOrFallbackModelType(
        HolderLookup.Provider registryAccess,
        ResourceKey<PostBlock.ModelType> key,
        Supplier<PostBlock.MaterialType> materialType
    ) {
        return registryAccess.lookupOrThrow(REGISTRY_KEY).get(key).map(Holder::value).orElseGet(() -> fallbackModelType(materialType.get()));
    }

    public static PostBlock.ModelType fallbackModelType(
        PostBlock.MaterialType materialType
    ) {
        return switch (materialType) {
            case Wood -> WOOD_FALLBACK;
            case Stone -> STONE_FALLBACK;
            case Metal -> METAL_FALLBACK;
            case Mushroom -> MUSHROOM_FALLBACK;
        };
    }

}
