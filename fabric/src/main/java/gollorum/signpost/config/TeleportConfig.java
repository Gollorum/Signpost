package gollorum.signpost.config;

import gollorum.signpost.minecraft.config.ITeleportConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class TeleportConfig implements ITeleportConfig {

    public final ModConfigSpec.BooleanValue enableTeleport;
    public final ModConfigSpec.IntValue maximumDistance;
    public final ModConfigSpec.BooleanValue enforceDiscovery;
    public final ModConfigSpec.BooleanValue enableAcrossDimensions;
    public final ModConfigSpec.BooleanValue allowVehicle;
    public final ModConfigSpec.BooleanValue allowLead;

    public final ModConfigSpec.ConfigValue<String> costItem;
    public final ModConfigSpec.ConfigValue<Integer> constantPayment;
    public final ModConfigSpec.ConfigValue<Integer> distancePerPayment;

    @Override
    public int distancePerPayment() { return distancePerPayment.get(); }

    @Override
    public int constantPayment() { return constantPayment.get(); }

    @Override
    public String costItem() { return costItem.get(); }

    @Override
    public boolean allowLead() { return allowLead.get(); }

    @Override
    public boolean allowVehicle() { return allowVehicle.get(); }

    @Override
    public boolean enableAcrossDimensions() { return enableAcrossDimensions.get(); }

    @Override
    public boolean enforceDiscovery() { return enforceDiscovery.get(); }

    @Override
    public int maximumDistance() { return maximumDistance.get(); }

    @Override
    public boolean enableTeleport() { return enableTeleport.get(); }

    public TeleportConfig(ModConfigSpec.Builder builder) {
        enableTeleport = builder.define("enable", true);
        maximumDistance = builder.comment("-1 = infinite")
            .defineInRange("max_distance", -1, -1, Integer.MAX_VALUE);
        enforceDiscovery = builder.define("enforce_discovery", true);
        enableAcrossDimensions = builder.define("enable_across_dimensions", true);
        allowVehicle = builder.define("teleport_vehicle", true);
        allowLead = builder.define("teleport_leaded_mobs", true);

        builder.push("cost");
        costItem = builder.comment("If \"item\" is a valid item (e.g. minecraft:ender_pearl), players will have to pay an amount of",
            "constant_payment + distance / distance_per_payment",
            "of this item when using a sign post to teleport.",
            "A negative distance_per_payment will set the second part of the sum to 0."
        ).define("item", "");
        constantPayment = builder.define("constant_payment", 1);
        distancePerPayment = builder.define("distance_per_payment", -1);
        builder.pop();
    }
}
