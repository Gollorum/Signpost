package gollorum.signpost.utils.serialization;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;

public final class ItemStackSerializer implements CompoundSerializable<ItemStack> {

	public static final ItemStackSerializer Instance = new ItemStackSerializer();

	private ItemStackSerializer() {}

	@Override
	public void writeTo(net.minecraft.item.ItemStack itemStack, CompoundNBT compound, String keyPrefix) {
		compound.put(keyPrefix, itemStack.write(new CompoundNBT()));
	}

	@Override
	public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
		return compound.contains(keyPrefix);
	}

	@Override
	public net.minecraft.item.ItemStack read(CompoundNBT compound, String keyPrefix) {
		INBT readCompound = compound.get(keyPrefix);
		if(readCompound instanceof CompoundNBT)
			return net.minecraft.item.ItemStack.read((CompoundNBT) readCompound);
		else return ItemStack.EMPTY;
	}

	@Override
	public void writeTo(ItemStack itemStack, PacketBuffer buffer) {
		buffer.writeItemStack(itemStack);
	}

	@Override
	public ItemStack readFrom(PacketBuffer buffer) {
		return buffer.readItemStack();
	}

}
