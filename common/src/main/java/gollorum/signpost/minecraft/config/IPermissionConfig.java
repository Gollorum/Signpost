package gollorum.signpost.minecraft.config;

public interface IPermissionConfig {
	int editLockedWaystoneCommandPermissionLevel();
	int editLockedSignCommandPermissionLevel();
	int teleportPermissionLevel();
	int discoverPermissionLevel();
	int setBlockRestrictionPermissionLevel();
	int listPermissionLevel();

	int pickUnownedWaystonePermissionLevel();

	int defaultMaxWaystonesPerPlayer();
	int defaultMaxSignpostsPerPlayer();
}
