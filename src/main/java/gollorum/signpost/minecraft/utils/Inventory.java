package gollorum.signpost.minecraft.utils;

import gollorum.signpost.Signpost;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Inventory {

	private static List<ItemStack> getAllItemStack(PlayerEntity player) {
		List<ItemStack> ret = new ArrayList<>();
		ret.add(player.getMainHandItem());
		ret.add(player.getOffhandItem());
		ret.addAll(player.inventory.items);
		return ret;
	}

	public static int getItemCount(PlayerEntity player, Item item) {
		int count = 0;
		for(ItemStack currentStack : getAllItemStack(player)) {
			if(currentStack.getItem().equals(item))
				count += currentStack.getCount();
		}
		return count;
	}

	public static void tryPay(PlayerEntity player, ItemStack itemStack, Consumer<PlayerEntity> onSuccess) {
		if(!player.isCreative()) {
			if(Inventory.tryConsume(player, itemStack))
				onSuccess.accept(player);
			else player.sendMessage(new TranslationTextComponent(
				LangKeys.tooExpensive,
				itemStack.getCount(),
				new TranslationTextComponent(itemStack.getItem().getDescriptionId())
			), Util.NIL_UUID);
		} else onSuccess.accept(player);
	}

	public static boolean tryConsume(PlayerEntity player, ItemStack itemStack) {
		if(itemStack.getCount() <= 0) return true;
		if(getItemCount(player, itemStack.getItem()) >= itemStack.getCount()) {
			int remainingItems = itemStack.getCount();
			for(ItemStack currentStack : getAllItemStack(player)) {
				if(currentStack.getItem().equals(itemStack.getItem())) {
					remainingItems -= currentStack.getCount();
					currentStack.setCount(remainingItems > 0 ? 0 : -remainingItems);
					if(remainingItems <= 0) return true;
				}
			}
			if(remainingItems <= 0) return true;
			else Signpost.LOGGER.error("Tried to consume more items than were present");
		}
		return false;
	}

}
