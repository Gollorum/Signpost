package gollorum.signpost.minecraft.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class TeleportConfig {

	public final ForgeConfigSpec.BooleanValue enableTeleport;
	public final ForgeConfigSpec.IntValue maximumDistance;
	public final ForgeConfigSpec.BooleanValue enforceDiscovery;
	public final ForgeConfigSpec.BooleanValue enableAcrossDimensions;

	public final ForgeConfigSpec.ConfigValue<String> costItem;
	public final ForgeConfigSpec.ConfigValue<Integer> constantPayment;
	public final ForgeConfigSpec.ConfigValue<Integer> distancePerPayment;

	public TeleportConfig(ForgeConfigSpec.Builder builder) {
		enableTeleport = builder.define("enable", true);
		maximumDistance = builder.comment("-1 = infinite")
			.defineInRange("max_distance", -1, -1, Integer.MAX_VALUE);
		enforceDiscovery = builder.define("enforce_discovery", true);
		enableAcrossDimensions = builder.define("enable_across_dimensions", true);

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
