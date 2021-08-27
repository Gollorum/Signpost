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
		if (player.getUniqueID().equals(playerHandle.id)) {
			if (!player.isCreative())
				player.inventory.func_234564_a_(
					i -> i.getItem().equals(cost.getItem()),
					cost.getCount(),
					player.container.func_234641_j_()
				);
		} else {
			Signpost.LOGGER.error(
				"Tried to apply cost but the player was not the expected one (expected {}, got {})",
				playerHandle.id,
				player.getUniqueID()
			);
		}
	}

	public static void makePlayerPay(PlayerEntity player, ItemStack cost) {
		if (!player.isCreative())
			player.inventory.func_234564_a_(
				i -> i.getItem().equals(cost.getItem()),
				cost.getCount(),
				player.container.func_234641_j_()
			);
	}

}
