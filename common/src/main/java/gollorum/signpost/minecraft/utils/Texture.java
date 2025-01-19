package gollorum.signpost.minecraft.utils;

import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ResourceLocationSerializer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

    public static final CompoundSerializable<Texture> CompundSerializer = new CompoundSerializable<>() {
        @Override
        public void encode(CompoundTag compound, Texture texture, HolderLookup.Provider provider) {
            ResourceLocationSerializer.Instance.encode(compound, texture.location, provider);
            compound.put("Tint", Tint.Serialization.instance.optional().encode(texture.tint, provider));
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return ResourceLocationSerializer.Instance.isContainedIn(compound) && compound.contains("Tint");
        }

        @Override
        public Texture decode(CompoundTag compound, HolderLookup.Provider provider) {
            return new Texture(
                ResourceLocationSerializer.Instance.decode(compound, provider),
                Tint.Serialization.instance.optional().decode(compound.getCompound("Tint"), provider)
            );
        }
    };

    public static final BufferSerializable<Texture> BufferSerializer = new BufferSerializable<>() {

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Texture texture) {
            ResourceLocationSerializer.Instance.encode(buffer, texture.location);
            Tint.Serialization.instance.optional().encode(buffer, texture.tint);
        }

        @Override
        public Texture decode(RegistryFriendlyByteBuf buffer) {
            return new Texture(
                ResourceLocationSerializer.Instance.decode(buffer),
                Tint.Serialization.instance.optional().decode(buffer)
            );
        }

        @Override
        public Class<Texture> getTargetClass() {
            return Texture.class;
        }
    };
}