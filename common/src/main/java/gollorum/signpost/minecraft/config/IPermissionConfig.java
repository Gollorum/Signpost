package gollorum.signpost.minecraft.config;

public interface IPermissionConfig {
	int editLockedWaystoneCommandPermissionLevel();
	int editLockedSignCommandPermissionLevel();
	int teleportPermissionLevel();
	int discoverPermissionLevel();
	int listPermissionLevel();

	int pickUnownedWaystonePermissionLevel();
}
