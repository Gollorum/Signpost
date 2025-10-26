package gollorum.signpost.config;

import gollorum.signpost.minecraft.config.ITeleportConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class TeleportConfig implements ConfigData, ITeleportConfig {
    public boolean enableTeleport = true;
    @ConfigEntry.Gui.Tooltip
    public int maximumDistance = -1;
    public boolean enforceDiscovery = true;
    public boolean enableAcrossDimensions = true;
    public boolean allowVehicle = true;
    public boolean allowLead = true;
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(count=3)
    public Cost cost = new Cost();

    @Override
    public boolean enableTeleport() {
        return enableTeleport;
    }

    @Override
    public int maximumDistance() {
        return maximumDistance;
    }

    @Override
    public boolean enforceDiscovery() {
        return enforceDiscovery;
    }

    @Override
    public boolean enableAcrossDimensions() {
        return enableAcrossDimensions;
    }

    @Override
    public boolean allowVehicle() {
        return allowVehicle;
    }

    @Override
    public boolean allowLead() {
        return allowLead;
    }

    @Override
    public String costItem() {
        return cost.costItem;
    }

    @Override
    public int constantPayment() {
        return cost.constantPayment;
    }

    @Override
    public int distancePerPayment() {
        return cost.distancePerPayment;
    }

    public class Cost {

        @ConfigEntry.Gui.Tooltip
        public String costItem = "";
        @ConfigEntry.Category("cost")
        public int constantPayment = 1;
        @ConfigEntry.Category("cost")
        public int distancePerPayment = -1;

    }
}
