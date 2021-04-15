package gollorum.signpost.minecraft.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class PermissionConfig {

	public final ForgeConfigSpec.ConfigValue<Integer> editLockedWaystoneCommandPermissionLevel;
	public final ForgeConfigSpec.ConfigValue<Integer> editLockedSignCommandPermissionLevel;

	public PermissionConfig(ForgeConfigSpec.Builder builder) {
		editLockedWaystoneCommandPermissionLevel = builder.define("edit_locked_waystones_command_permission_level", 3);
		editLockedSignCommandPermissionLevel = builder.define("edit_locked_signs_command_permission_level", 3);

	}
}
