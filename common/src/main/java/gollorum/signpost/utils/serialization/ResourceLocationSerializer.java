package gollorum.signpost.utils.serialization;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class ResourceLocationSerializer implements CompoundSerializable<ResourceLocation> {

    public static ResourceLocationSerializer Instance = ResourceLocation.parseSerializer();

    public void encode(CompoundTag compound, ResourceLocation location, HolderLookup.Provider provider) {
        compound.putString("ResourceLocation", location.toString());
        return compound;
    }

    public boolean isContainedIn(CompoundTag compound) {
        return compound.contains("ResourceLocation");
    }

    public ResourceLocation decode(CompoundTag compound, HolderLookup.Provider provider) {
        return ResourceLocation.parse(compound.getString("ResourceLocation"));
    }

    @Override
    public void encode(FriendlyByteBuf buffer, ResourceLocation resourceLocation) {
        buffer.writeResourceLocation(resourceLocation);
    }

    @Override
    public ResourceLocation decode(FriendlyByteBuf buffer) {
        return buffer.readResourceLocation();
    }

    @Override
    public Class<ResourceLocation> getTargetClass() {
        return ResourceLocation.class;
    }
}