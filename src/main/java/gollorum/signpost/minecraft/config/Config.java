package gollorum.signpost.minecraft.config;

import gollorum.signpost.minecraft.block.ModelWaystone;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

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

		public final TeleportConfig teleport;
		public final WorldGenConfig worldGen;

		public final ForgeConfigSpec.ConfigValue<List<? extends String>> allowedWaystones;
		public final ForgeConfigSpec.ConfigValue<Integer> editLockedWaystoneCommandPermissionLevel;

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
					ModelWaystone.variants.contains(new ModelWaystone.Variant((String) n, null, 0
			)));
			editLockedWaystoneCommandPermissionLevel = builder.define("edit_locked_waystones_command_permission_level", 3);

			builder.push("world_gen");
			worldGen = new WorldGenConfig(builder);
			builder.pop();
		}

	}

	public static class Client {

		public final ForgeConfigSpec.BooleanValue enableConfirmationScreen;

		public Client(ForgeConfigSpec.Builder builder) {
			builder.push("teleport");
			enableConfirmationScreen = builder
				.comment(
					"Defines whether the confirmation screen pops when using a sign to teleport.",
					"CAUTION 1: The necessary items will be removed without notice if costs are involved.",
					"CAUTION 2: The only way to edit a sign with destination is through this screen."
				).define("enable_confirmation_screen", true);
			builder.pop();
		}

	}

}
