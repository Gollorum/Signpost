package gollorum.signpost.minecraft.utils;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.Optional;

public class SideUtils {

	public static Optional<PlayerEntity> getClientPlayer() {
		return Minecraft.getInstance().player != null
			? Optional.of(Minecraft.getInstance().player)
			: Optional.empty();
	}

	public static void makePlayerPayIfEditor(boolean isRemote, PlayerEntity sender, PlayerHandle playerHandle, ItemStack cost) {
		PlayerEntity player = isRemote ? SideUtils.getClientPlayer().get() : sender;
		if (player.getUUID().equals(playerHandle.id)) {
			if (!player.isCreative())
				player.inventory.clearOrCountMatchingItems(
					i -> i.getItem().equals(cost.getItem()),
					cost.getCount(),
					player.inventoryMenu.getCraftSlots()
				);
		} else {
			Signpost.LOGGER.error(
				"Tried to apply cost but the player was not the expected one (expected {}, got {})",
				playerHandle.id,
				player.getUUID()
			);
		}
	}

	public static void makePlayerPay(PlayerEntity player, ItemStack cost) {
		if (!player.isCreative())
			player.inventory.clearOrCountMatchingItems(
				i -> i.getItem().equals(cost.getItem()),
				cost.getCount(),
				player.inventoryMenu.getCraftSlots()
			);
	}

}
