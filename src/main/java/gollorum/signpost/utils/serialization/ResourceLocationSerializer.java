package gollorum.signpost.utils.serialization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class ResourceLocationSerializer implements CompoundSerializable<ResourceLocation> {

    public static ResourceLocationSerializer Instance = new ResourceLocationSerializer();

    public CompoundTag write(ResourceLocation location, CompoundTag compound) {
        compound.putString("ResourceLocation", location.toString());
        return compound;
    }

    public boolean isContainedIn(CompoundTag compound) {
        return compound.contains("ResourceLocation");
    }

    public ResourceLocation read(CompoundTag compound) {
        return new ResourceLocation(compound.getString("ResourceLocation"));
    }

    @Override
    public void write(ResourceLocation resourceLocation, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(resourceLocation);
    }

    @Override
    public ResourceLocation read(FriendlyByteBuf buffer) {
        return buffer.readResourceLocation();
    }

    @Override
    public Class<ResourceLocation> getTargetClass() {
        return ResourceLocation.class;
    }
}