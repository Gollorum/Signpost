package gollorum.signpost.minecraft.utils;

import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ResourceLocationSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record Texture(ResourceLocation location, Optional<Tint> tint){
    public Texture(ResourceLocation location) { this(location, Optional.empty()); }

    @Override
    public String toString() {
        return null;
    }

    public static Texture readFrom(Tag tag) {
        if(tag instanceof CompoundTag compound) return Serializer.read(compound);
        else return new Texture(ResourceLocation.tryParse(tag.getAsString()));
    }

    public static final CompoundSerializable<Texture> Serializer = new CompoundSerializable<>() {
        @Override
        public CompoundTag write(Texture texture, CompoundTag compound) {
            ResourceLocationSerializer.Instance.write(texture.location, compound);
            compound.put("Tint", Tint.Serialization.instance.optional().write(texture.tint));
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return ResourceLocationSerializer.Instance.isContainedIn(compound) && compound.contains("Tint");
        }

        @Override
        public Texture read(CompoundTag compound) {
            return new Texture(
                ResourceLocationSerializer.Instance.read(compound),
                Tint.Serialization.instance.optional().read(compound.getCompound("Tint"))
            );
        }

        @Override
        public void write(Texture texture, FriendlyByteBuf buffer) {
            ResourceLocationSerializer.Instance.write(texture.location, buffer);
            Tint.Serialization.instance.optional().write(texture.tint, buffer);
        }

        @Override
        public Texture read(FriendlyByteBuf buffer) {
            return new Texture(
                ResourceLocationSerializer.Instance.read(buffer),
                Tint.Serialization.instance.optional().read(buffer)
            );
        }

        @Override
        public Class<Texture> getTargetClass() {
            return Texture.class;
        }
    };
}