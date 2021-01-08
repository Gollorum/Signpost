package gollorum.signpost.minecraft.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Optional;

public class SideUtils {

	public static Optional<PlayerEntity> getClientPlayer() {
		return Minecraft.getInstance().player != null
			? Optional.of(Minecraft.getInstance().player)
			: Optional.empty();
	}

}
