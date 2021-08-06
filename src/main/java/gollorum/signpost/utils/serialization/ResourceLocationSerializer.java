package gollorum.signpost.utils.serialization;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class ResourceLocationSerializer implements CompoundSerializable<ResourceLocation> {

    public static ResourceLocationSerializer Instance = new ResourceLocationSerializer();

    public CompoundNBT write(net.minecraft.util.ResourceLocation location, CompoundNBT compound) {
        compound.putString("ResourceLocation", location.toString());
        return compound;
    }

    public boolean isContainedIn(CompoundNBT compound) {
        return compound.contains("ResourceLocation");
    }

    public net.minecraft.util.ResourceLocation read(CompoundNBT compound) {
        return new ResourceLocation(compound.getString("ResourceLocation"));
    }

    @Override
    public void write(ResourceLocation resourceLocation, PacketBuffer buffer) {
        buffer.writeResourceLocation(resourceLocation);
    }

    @Override
    public ResourceLocation read(PacketBuffer buffer) {
        return buffer.readResourceLocation();
    }

    @Override
    public Class<ResourceLocation> getTargetClass() {
        return ResourceLocation.class;
    }
}