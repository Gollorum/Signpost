package gollorum.signpost.minecraft.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.OptionalSerializerV1;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record Texture(Identifier identifier, Identifier atlasLocation, Optional<Tint> tint){
    public Texture(Identifier location) { this(location, Optional.empty(), Optional.empty()); }

    private Texture(Identifier location, Optional<Identifier> atlasLocation, Optional<Tint> tint) {
        this(location, atlasLocation.orElse(TextureResource.blockAtlas), tint);
    }

    @Override
    public String toString() {
        return null;
    }

    public Material toMaterial() {
        return new Material(atlasLocation, identifier);
    }

    public static Codec<Texture> codec(int version) {
        return switch (version) {
            case 1 -> CODEC_V1;
            case 2 -> CODEC_V2;
            default -> throw new IllegalArgumentException("Unsupported version: " + version);
        };
    }

    public static final Codec<Texture> CODEC_V1 = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("ResourceLocation").forGetter(Texture::identifier),
        OptionalSerializerV1.of(Tint.Serialization.CODEC).codec().fieldOf("Tint").forGetter(Texture::tint)
    ).apply(i, (loc, tint) -> new Texture(loc, Optional.empty(), tint)));

    public static final Codec<Texture> CODEC_V2 = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("ResourceLocation").forGetter(Texture::identifier),
        Codec.optionalField("AtlasLocation", Identifier.CODEC, true).forGetter(t -> Optional.of(t.atlasLocation)),
        Codec.optionalField("Tint", Tint.Serialization.CODEC, true).forGetter(Texture::tint)
    ).apply(i, Texture::new));

    public static final StreamCodec<ByteBuf, Texture> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, Texture::identifier,
        Identifier.STREAM_CODEC, Texture::atlasLocation,
        ByteBufCodecs.optional(Tint.Serialization.STREAM_CODEC), Texture::tint,
        Texture::new
    );

}