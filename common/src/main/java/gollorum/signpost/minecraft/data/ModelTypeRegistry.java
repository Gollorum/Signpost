package gollorum.signpost.minecraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.utils.Texture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Ingredient;
import io.netty.buffer.ByteBuf;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ModelTypeRegistry {

    public static final ResourceKey<Registry<PostBlock.ModelType>> REGISTRY_KEY = ResourceKey.createRegistryKey(
        Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "post_model_types")
    );

    /**
     * The codec for a model type reference stored inside a sign part.
     *
     * <p>Signposts written before 2.04 name the model type as a bare {@code "spruce"} rather than as an id.
     * {@link Identifier}'s own codec would read that as {@code minecraft:spruce}, so this one completes a
     * namespace-less name with Signpost's namespace instead of Minecraft's. Writing always emits the full id.
     */
    public static final Codec<ResourceKey<PostBlock.ModelType>> KEY_CODEC = Codec.STRING.comapFlatMap(
        name -> {
            try {
                return DataResult.success(ResourceKey.create(REGISTRY_KEY, name.indexOf(':') < 0
                    ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, name)
                    : Identifier.parse(name)));
            } catch (Exception e) {
                return DataResult.error(() -> "Not a valid post model type: " + name);
            }
        },
        key -> key.identifier().toString()
    );

    public static final StreamCodec<ByteBuf, ResourceKey<PostBlock.ModelType>> KEY_STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.map(
            name -> ResourceKey.create(REGISTRY_KEY, Identifier.parse(name)),
            key -> key.identifier().toString()
        );

    /**
     * Finds the model type a post part's texture belongs to, which is how the type of a signpost written
     * before 2.04 is recovered when it carries no sign to read it off. Restricted to the material the block
     * already is, so a texture shared with another material cannot pull the post across.
     */
    public static Optional<ResourceKey<PostBlock.ModelType>> findByPostTexture(
        HolderLookup.Provider registryAccess,
        Texture postTexture,
        PostBlock.MaterialType materialType
    ) {
        return registryAccess.lookup(REGISTRY_KEY).stream()
            .flatMap(HolderLookup::listElements)
            .filter(holder -> holder.value().materialType() == materialType
                && holder.value().postTexture().identifier().equals(postTexture.identifier()))
            .findFirst()
            .flatMap(Holder.Reference::unwrapKey);
    }

    public static Stream<PostBlock.ModelType> getAllModelTypes(HolderLookup.Provider registryAccess) {
        return registryAccess.lookupOrThrow(REGISTRY_KEY).listElements().map(Holder.Reference::value);
    }

    public static Stream<Holder<PostBlock.ModelType>> getAllModelTypeHolders(HolderLookup.Provider registryAccess) {
        return registryAccess.lookupOrThrow(REGISTRY_KEY).listElements().map(it -> it);
    }

    public static final PostBlock.ModelType WOOD_FALLBACK = new PostBlock.ModelType(
        PostBlock.MaterialType.Wood,
        Ingredient.of(net.minecraft.world.item.Items.OAK_SIGN),
        new Texture(Identifier.parse("block/oak_log")),
        new Texture(Identifier.parse("block/stripped_oak_log")),
        new Texture(Identifier.parse("block/oak_log")),
        Optional.empty()
    );

    public static final PostBlock.ModelType STONE_FALLBACK = new PostBlock.ModelType(
        PostBlock.MaterialType.Stone,
        Ingredient.of(net.minecraft.world.item.Items.STONE),
        new Texture(Identifier.parse("block/stone")),
        new Texture(Identifier.parse("block/stone")),
        new Texture(Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/stone_dark")),
        Optional.empty()
    );

    public static final PostBlock.ModelType METAL_FALLBACK = new PostBlock.ModelType(
        PostBlock.MaterialType.Metal,
        Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT),
        new Texture(Identifier.parse("block/iron_block")),
        new Texture(Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/iron")),
        new Texture(Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/iron_dark")),
        Optional.empty()
    );

    public static final PostBlock.ModelType MUSHROOM_FALLBACK = new PostBlock.ModelType(
        PostBlock.MaterialType.Mushroom,
        Ingredient.of(net.minecraft.world.item.Items.RED_MUSHROOM),
        new Texture(Identifier.parse("block/red_mushroom_block")),
        new Texture(Identifier.parse("block/mushroom_stem")),
        new Texture(Identifier.parse("block/red_mushroom_block")),
        Optional.empty()
    );

    public static PostBlock.ModelType getOrFallbackModelType(
        HolderLookup.Provider registryAccess,
        ResourceKey<PostBlock.ModelType> key,
        Supplier<PostBlock.MaterialType> materialType
    ) {
        if (key != null) {
            var lookup = registryAccess.lookup(REGISTRY_KEY);
            if (lookup.isPresent()) {
                var found = lookup.get().get(key);
                if (found.isPresent()) return found.get().value();
            }
        }
        return fallbackModelType(materialType.get());
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
