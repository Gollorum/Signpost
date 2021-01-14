package gollorum.signpost.minecraft;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

	public static final Server Server;
	private static final ForgeConfigSpec ServerConfig;

	public static final Client Client;
	private static final ForgeConfigSpec ClientConfig;

	static {
		Pair<Server, ForgeConfigSpec> serverPair = new ForgeConfigSpec.Builder().configure(Server::new);
		Server = serverPair.getKey();
		ServerConfig = serverPair.getValue();
		Pair<Client, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(Client::new);
		Client = clientPair.getKey();
		ClientConfig = clientPair.getValue();
	}

	public static void register() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig);
	}

	public static class Server {

		public final ForgeConfigSpec.BooleanValue enableTeleport;
		public final ForgeConfigSpec.IntValue maximumDistance;
		public final ForgeConfigSpec.BooleanValue enforceDiscovery;

		public final ForgeConfigSpec.ConfigValue<String> costItem;
		public final ForgeConfigSpec.ConfigValue<Integer> constantPayment;
		public final ForgeConfigSpec.ConfigValue<Integer> distancePerPayment;

		public Server(ForgeConfigSpec.Builder builder) {
			builder.push("teleport");
			enableTeleport = builder.define("enable", true);
			maximumDistance = builder.defineInRange("max_distance", -1, -1, Integer.MAX_VALUE);
			enforceDiscovery = builder.define("enforce_discovery", true);

			builder.push("cost");
			builder.comment("If \"item\" is a valid item (e.g. minecraft:ender_pearl), players will have to pay an amount of",
				"constant_payment + distance / distance_per_payment",
				"of this item when using a sign post to teleport.",
				"A negative distance_per_payment will set the second part of the sum to 0.");
			costItem = builder.define("item", "");
			constantPayment = builder.define("constant_payment", 1);
			distancePerPayment = builder.define("distance_per_payment", -1);
			builder.pop();
			builder.pop();
		}

	}

	public static class Client {

		public final ForgeConfigSpec.BooleanValue enableConfirmationScreen;

		public Client(ForgeConfigSpec.Builder builder) {
			builder.push("teleport");
			enableConfirmationScreen = builder.define("enable_confirmation_screen", true);
			builder.pop();
		}

	}

}
