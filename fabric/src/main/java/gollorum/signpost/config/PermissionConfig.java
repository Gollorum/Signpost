package gollorum.signpost.config;

import gollorum.signpost.minecraft.config.IPermissionConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class PermissionConfig implements ConfigData, IPermissionConfig {
    public int editLockedWaystoneCommandPermissionLevel = 3;
    public int editLockedSignCommandPermissionLevel = 3;
    public int teleportPermissionLevel = 2;
    public int discoverPermissionLevel = 2;
    public int listPermissionLevel = 2;
    @ConfigEntry.Gui.Tooltip
    public int pickUnownedWaystonePermissionLevel = 0;

    @Override
    public int editLockedWaystoneCommandPermissionLevel() {
        return editLockedWaystoneCommandPermissionLevel;
    }

    @Override
    public int editLockedSignCommandPermissionLevel() {
        return editLockedSignCommandPermissionLevel;
    }

    @Override
    public int teleportPermissionLevel() {
        return teleportPermissionLevel;
    }

    @Override
    public int discoverPermissionLevel() {
        return discoverPermissionLevel;
    }

    @Override
    public int listPermissionLevel() {
        return listPermissionLevel;
    }

    @Override
    public int pickUnownedWaystonePermissionLevel() {
        return pickUnownedWaystonePermissionLevel;
    }
}
