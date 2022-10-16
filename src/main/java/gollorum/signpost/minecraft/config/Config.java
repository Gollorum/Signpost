package gollorum.signpost.minecraft.config;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.utils.Tuple;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;
import java.util.stream.Collectors;

public class Config {

	public static final Server Server;
	public static final ForgeConfigSpec ServerConfig;

	public static final Common Common;
	private static final ForgeConfigSpec CommonConfig;

	public static final Client Client;
	private static final ForgeConfigSpec ClientConfig;

	static {
		Tuple<Server, ForgeConfigSpec> serverTuple = Tuple.from(new ForgeConfigSpec.Builder().configure(Server::new));
		Server = serverTuple._1;
		ServerConfig = serverTuple._2;
		Tuple<Common, ForgeConfigSpec> commonTuple = Tuple.from(new ForgeConfigSpec.Builder().configure(Common::new));
		Common = commonTuple._1;
		CommonConfig = commonTuple._2;
		Tuple<Client, ForgeConfigSpec> clientTuple = Tuple.from(new ForgeConfigSpec.Builder().configure(Client::new));
		Client = clientTuple._1;
		ClientConfig = clientTuple._2;
	}

	public static void register() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig);
	}

	public static class Server {

		public final TeleportConfig teleport;
		public final WorldGenConfig worldGen;
		public final PermissionConfig permissions;

		public final ForgeConfigSpec.ConfigValue<List<? extends String>> allowedWaystones;

		public Server(ForgeConfigSpec.Builder builder) {
			builder.push("teleport");
			teleport = new TeleportConfig(builder);
			builder.pop();
			allowedWaystones = builder.comment("You can define which waystone models are enabled.",
				"Disabled types are still in the game but cannot be crafted and disappear from the creative menu.",
				"The available variants are: " +
					ModelWaystone.variants.stream()
						.map(v -> "\"" + v.name + "\"")
						.collect(Collectors.joining(", ")),
				"Check out the curseforge page to see what they look like: https://www.curseforge.com/minecraft/mc-mods/signpost/pages/waystone-models"
			).worldRestart()
			.defineList(
				"allowed_waystone_models",
				ModelWaystone.variants.stream().map(v -> v.name).collect(Collectors.toList()),
				n -> n instanceof String &&
					ModelWaystone.variants.contains(new ModelWaystone.Variant((String) n, null, null, 0
			)));

			builder.push("permissions");
			permissions = new PermissionConfig(builder);
			builder.pop();

			builder.push("world_gen");
			worldGen = new WorldGenConfig(builder, true);
			builder.pop();
		}

	}

	public static class Common {

		public final WorldGenConfig worldGenDefaults;

		public Common(ForgeConfigSpec.Builder builder) {

			builder.push("world_gen_defaults");
			worldGenDefaults = new WorldGenConfig(builder, false);
			builder.pop();

		}

	}

	public static class Client {

		public final ForgeConfigSpec.BooleanValue enableConfirmationScreen;
		public final ForgeConfigSpec.BooleanValue enableWaystoneLimitNotifications;
		public final ForgeConfigSpec.BooleanValue enableSignpostLimitNotifications;

		public Client(ForgeConfigSpec.Builder builder) {
			builder.push("teleport");

			enableConfirmationScreen = builder
				.comment(
					"Defines whether the confirmation screen pops when using a sign to teleport.",
					"CAUTION 1: The necessary items will be removed without notice if costs are involved.",
					"CAUTION 2: The only way to edit a sign with destination is through this screen.",
					"This should probably never be turned off. Why did I make it an option? No idea."
				).define("enable_confirmation_screen", true);
			enableWaystoneLimitNotifications = builder
				.comment("Choose whether you want to receive a notification on how many waystones you have left to place (if it is limited by the server).")
				.define("enable_waystone_limit_notifications", true);
			enableSignpostLimitNotifications = builder
				.comment("Choose whether you want to receive a notification on how many signposts you have left to place (if it is limited by the server).")
				.define("enable_signpost_limit_notifications", true);
			builder.pop();
		}

	}

}
