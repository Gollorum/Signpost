package gollorum.signpost.minecraft.config;

import gollorum.signpost.minecraft.commands.BlockRestrictions;
import net.minecraftforge.common.ForgeConfigSpec;

public class PermissionConfig {

	public final ForgeConfigSpec.ConfigValue<Integer> editLockedWaystoneCommandPermissionLevel;
	public final ForgeConfigSpec.ConfigValue<Integer> editLockedSignCommandPermissionLevel;

	public final ForgeConfigSpec.ConfigValue<Integer> defaultMaxWaystonesPerPlayer;
	public final ForgeConfigSpec.ConfigValue<Integer> defaultMaxSignpostsPerPlayer;

	public PermissionConfig(ForgeConfigSpec.Builder builder) {
		editLockedWaystoneCommandPermissionLevel = builder.define("edit_locked_waystones_command_permission_level", 3);
		editLockedSignCommandPermissionLevel = builder.define("edit_locked_signs_command_permission_level", 3);
		defaultMaxWaystonesPerPlayer = builder
			.comment("-1 = infinite", "Change via /signpost " + BlockRestrictions.commandName + " waystones set <Amount>")
			.define("max_waystones_per_player", -1);
		defaultMaxSignpostsPerPlayer = builder
			.comment("-1 = infinite", "Change via /signpost " + BlockRestrictions.commandName + " signposts set <Amount>")
			.define("max_signposts_per_player", -1);

	}
}
