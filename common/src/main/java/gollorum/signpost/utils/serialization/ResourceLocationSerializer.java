package gollorum.signpost.utils.serialization;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class ResourceLocationSerializer {

    public static CompoundSerializable<ResourceLocation> COMPOUND = new CompoundSerializable<>() {

        public void encode(CompoundTag compound, ResourceLocation location, HolderLookup.Provider provider) {
            compound.putString("ResourceLocation", location.toString());
        }

        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("ResourceLocation");
        }

        public ResourceLocation decode(CompoundTag compound, HolderLookup.Provider provider) {
            return ResourceLocation.parse(compound.getString("ResourceLocation"));
        }
    };

    public static BufferSerializable<ResourceLocation> BUFFER = new BufferSerializable<>() {

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ResourceLocation resourceLocation) {
            buffer.writeResourceLocation(resourceLocation);
        }

        @Override
        public ResourceLocation decode(RegistryFriendlyByteBuf buffer) {
            return buffer.readResourceLocation();
        }

        @Override
        public Class<ResourceLocation> getTargetClass() {
        return ResourceLocation.class;
    }
    };
}