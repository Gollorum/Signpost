package gollorum.signpost.minecraft.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.OptionalCompoundSerializer;
import gollorum.signpost.utils.serialization.ResourceLocationSerializer;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record Texture(ResourceLocation location, Optional<Tint> tint){
    public Texture(ResourceLocation location) { this(location, Optional.empty()); }

    @Override
    public String toString() {
        return null;
    }

    public static Texture readFrom(Tag tag, HolderLookup.Provider provider) {
        if(tag instanceof CompoundTag compound) return CompundSerializer.decode(compound, provider);
        else return new Texture(ResourceLocation.tryParse(tag.getAsString()));
    }

    public static final Codec<Texture> CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceLocation.CODEC.fieldOf("ResourceLocation").forGetter(Texture::location),
        OptionalCompoundSerializer.from(Tint.Serialization.CODEC).fieldOf("Tint").forGetter(Texture::tint)
    ).apply(i, Texture::new));

    public static final StreamCodec<ByteBuf, Texture> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, Texture::location,
        ByteBufCodecs.optional(Tint.Serialization.STREAM_CODEC), Texture::tint,
        Texture::new
    );

}