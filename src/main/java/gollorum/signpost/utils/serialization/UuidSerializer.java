package gollorum.signpost.utils.serialization;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import java.util.UUID;

public class UuidSerializer implements CompoundSerializable<UUID> {

    public static final UuidSerializer INSTANCE = new UuidSerializer();

    private UuidSerializer(){}

    @Override
    public Class<UUID> getTargetClass() {
        return UUID.class;
    }

    @Override
    public CompoundNBT write(UUID uuid, CompoundNBT compound) {
        compound.putUUID("Id", uuid);
        return compound;
    }

    @Override
    public boolean isContainedIn(CompoundNBT compound) {
        return compound.contains("IdLeast") && compound.contains("IdMost");
    }

    @Override
    public UUID read(CompoundNBT compound) {
        return compound.getUUID("Id");
    }

    @Override
    public void write(UUID uuid, PacketBuffer buffer) {
        buffer.writeUUID(uuid);
    }

    @Override
    public UUID read(PacketBuffer buffer) {
        return buffer.readUUID();
    }
}
