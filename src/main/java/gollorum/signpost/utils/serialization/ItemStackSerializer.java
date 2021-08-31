package gollorum.signpost.utils.serialization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class ItemStackSerializer implements CompoundSerializable<ItemStack> {

	public static final ItemStackSerializer Instance = new ItemStackSerializer();

	private ItemStackSerializer() {}

	@Override
	public CompoundTag write(ItemStack itemStack, CompoundTag compound) {
		compound.put("ItemStack", itemStack.save(new CompoundTag()));
		return compound;
	}

	@Override
	public boolean isContainedIn(CompoundTag compound) {
		return compound.contains("ItemStack");
	}

	@Override
	public ItemStack read(CompoundTag compound) {
		Tag readCompound = compound.get("ItemStack");
		if(readCompound instanceof CompoundTag)
			return ItemStack.of((CompoundTag) readCompound);
		else return ItemStack.EMPTY;
	}

	@Override
	public Class<ItemStack> getTargetClass() {
		return ItemStack.class;
	}

	@Override
	public void write(ItemStack itemStack, FriendlyByteBuf buffer) {
		buffer.writeItem(itemStack);
	}

	@Override
	public ItemStack read(FriendlyByteBuf buffer) {
		return buffer.readItem();
	}

}
