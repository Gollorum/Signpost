package gollorum.signpost.minecraft.config;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public interface IPermissionConfig {
	int editLockedWaystoneCommandPermissionLevel();
	int editLockedSignCommandPermissionLevel();
	int teleportPermissionLevel();
	int discoverPermissionLevel();
	int listPermissionLevel();

	int pickUnownedWaystonePermissionLevel();

	default Permission editLockedWaystoneCommandPermission() {
		return new Permission.HasCommandLevel(PermissionLevel.byId(editLockedWaystoneCommandPermissionLevel()));
	}
	default Permission editLockedSignCommandPermission() {
		return new Permission.HasCommandLevel(PermissionLevel.byId(editLockedSignCommandPermissionLevel()));
	}
	default Permission teleportPermission() {
		return new Permission.HasCommandLevel(PermissionLevel.byId(teleportPermissionLevel()));
	}
	default Permission discoverPermission() {
		return new Permission.HasCommandLevel(PermissionLevel.byId(discoverPermissionLevel()));
	}
	default Permission listPermission() {
		return new Permission.HasCommandLevel(PermissionLevel.byId(listPermissionLevel()));
	}
	default Permission pickUnownedWaystonePermission() {
		return new Permission.HasCommandLevel(PermissionLevel.byId(pickUnownedWaystonePermissionLevel()));
	}
}
