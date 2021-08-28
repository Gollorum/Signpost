package gollorum.signpost.utils.serialization;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;

public final class ItemStackSerializer implements CompoundSerializable<ItemStack> {

	public static final ItemStackSerializer Instance = new ItemStackSerializer();

	private ItemStackSerializer() {}

	@Override
	public CompoundNBT write(net.minecraft.item.ItemStack itemStack, CompoundNBT compound) {
		compound.put("ItemStack", itemStack.save(new CompoundNBT()));
		return compound;
	}

	@Override
	public boolean isContainedIn(CompoundNBT compound) {
		return compound.contains("ItemStack");
	}

	@Override
	public net.minecraft.item.ItemStack read(CompoundNBT compound) {
		INBT readCompound = compound.get("ItemStack");
		if(readCompound instanceof CompoundNBT)
			return net.minecraft.item.ItemStack.of((CompoundNBT) readCompound);
		else return ItemStack.EMPTY;
	}

	@Override
	public Class<ItemStack> getTargetClass() {
		return ItemStack.class;
	}

	@Override
	public void write(ItemStack itemStack, PacketBuffer buffer) {
		buffer.writeItem(itemStack);
	}

	@Override
	public ItemStack read(PacketBuffer buffer) {
		return buffer.readItem();
	}

}
