package gollorum.signpost.minecraft.utils;

import gollorum.signpost.Signpost;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Inventory {

	private static List<ItemStack> getAllItemStack(Player player) {
		List<ItemStack> ret = new ArrayList<>();
		ret.add(player.getMainHandItem());
		ret.add(player.getOffhandItem());
		ret.addAll(player.getInventory().items);
		return ret;
	}

	public static int getItemCount(Player player, Item item) {
		int count = 0;
		for(ItemStack currentStack : getAllItemStack(player)) {
			if(currentStack.getItem().equals(item))
				count += currentStack.getCount();
		}
		return count;
	}

	public static void tryPay(Player player, ItemStack itemStack, Consumer<Player> onSuccess) {
		if(!player.isCreative()) {
			if(Inventory.tryConsume(player, itemStack))
				onSuccess.accept(player);
			else player.sendSystemMessage(Component.translatable(
				LangKeys.tooExpensive,
				itemStack.getCount(),
				Component.translatable(itemStack.getItem().getDescriptionId())
			));
		} else onSuccess.accept(player);
	}

	public static boolean tryConsume(Player player, ItemStack itemStack) {
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
