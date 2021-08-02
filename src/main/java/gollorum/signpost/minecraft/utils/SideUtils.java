package gollorum.signpost.minecraft.utils;

import gollorum.signpost.Signpost;
import gollorum.signpost.utils.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.Dimension;
import net.minecraft.world.World;
import net.minecraft.world.storage.DimensionSavedDataManager;

import java.util.Optional;
import java.util.stream.StreamSupport;

public class SideUtils {

	public static Optional<PlayerEntity> getClientPlayer() {
		return Minecraft.getInstance().player != null
			? Optional.of(Minecraft.getInstance().player)
			: Optional.empty();
	}

}
