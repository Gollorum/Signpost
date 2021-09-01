package gollorum.signpost.minecraft.config;

import gollorum.signpost.minecraft.commands.BlockRestrictions;
import net.minecraftforge.common.ForgeConfigSpec;

public class PermissionConfig {

	public final ForgeConfigSpec.ConfigValue<Integer> editLockedWaystoneCommandPermissionLevel;
	public final ForgeConfigSpec.ConfigValue<Integer> editLockedSignCommandPermissionLevel;
	public final ForgeConfigSpec.ConfigValue<Integer> teleportPermissionLevel;
	public final ForgeConfigSpec.ConfigValue<Integer> discoverPermissionLevel;
	public final ForgeConfigSpec.ConfigValue<Integer> setBlockResPermissionLevel;
	public final ForgeConfigSpec.ConfigValue<Integer> listPermissionLevel;

	public final ForgeConfigSpec.ConfigValue<Integer> defaultMaxWaystonesPerPlayer;
	public final ForgeConfigSpec.ConfigValue<Integer> defaultMaxSignpostsPerPlayer;

	public PermissionConfig(ForgeConfigSpec.Builder builder) {
		teleportPermissionLevel = builder.define("teleport_command_permission_level", 3);
		discoverPermissionLevel = builder.define("discover_command_permission_level", 3);
		setBlockResPermissionLevel = builder.define("block_restrictions_set_command_permission_level", 3);
		listPermissionLevel = builder.define("list_command_permission_level", 3);

		editLockedWaystoneCommandPermissionLevel = builder.define("edit_locked_waystones_permission_level", 3);
		editLockedSignCommandPermissionLevel = builder.define("edit_locked_signs_permission_level", 3);
		defaultMaxWaystonesPerPlayer = builder
			.comment("-1 = infinite", "Change via /signpost " + BlockRestrictions.commandName + " waystones set <Amount>")
			.define("max_waystones_per_player", -1);
		defaultMaxSignpostsPerPlayer = builder
			.comment("-1 = infinite", "Change via /signpost " + BlockRestrictions.commandName + " signposts set <Amount>")
			.define("max_signposts_per_player", -1);

	}
}
