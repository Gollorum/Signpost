package gollorum.signpost.minecraft.utils;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class SideUtils {

	public static Optional<Player> getClientPlayer() {
		return Minecraft.getInstance().player != null
			? Optional.of(Minecraft.getInstance().player)
			: Optional.empty();
	}

	public static void makePlayerPayIfEditor(boolean isRemote, Player sender, PlayerHandle playerHandle, ItemStack cost) {
		Player player = isRemote ? SideUtils.getClientPlayer().get() : sender;
		if (player.getUUID().equals(playerHandle.id)) {
			if (!player.isCreative())
				player.getInventory().clearOrCountMatchingItems(
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

	public static void makePlayerPay(Player player, ItemStack cost) {
		if (!player.isCreative())
			player.getInventory().clearOrCountMatchingItems(
				i -> i.getItem().equals(cost.getItem()),
				cost.getCount(),
				player.inventoryMenu.getCraftSlots()
			);
	}

}
