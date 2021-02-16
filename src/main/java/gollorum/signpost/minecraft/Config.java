package gollorum.signpost.minecraft;

import com.google.common.collect.Lists;
import gollorum.signpost.minecraft.block.ModelWaystone;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
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

		public final ForgeConfigSpec.BooleanValue enableTeleport;
		public final ForgeConfigSpec.IntValue maximumDistance;
		public final ForgeConfigSpec.BooleanValue enforceDiscovery;

		public final ForgeConfigSpec.ConfigValue<String> costItem;
		public final ForgeConfigSpec.ConfigValue<Integer> constantPayment;
		public final ForgeConfigSpec.ConfigValue<Integer> distancePerPayment;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> allowedWaystones;

		public final ForgeConfigSpec.ConfigValue<Integer> editLockedWaystoneCommandPermissionLevel;

		public final ForgeConfigSpec.ConfigValue<List<? extends String>> allowedVillageWaystones;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> villageNamePrefixes;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> villageNameInfixes;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> villageNamePostfixes;

		private static final List<String> generalNamePrefixes = Lists.newArrayList("ak", "dev", "dol ", "ed", "il", "og", "por", "rov", "tek ", "tar", "tol ", "ves", "");
		private static final List<String> generalNameInfixes = Lists.newArrayList("do ", "en", "go", "na", "nah ", "ker", "ol", "ora", "ra", "rem", "ro");
		private static final List<String> generalNamePostfixes = Lists.newArrayList("ar", "blo", "bo", "bro", "do", "dra", "er", "ker", "lia", "tek");

		private static final List<String> germanNamePrefixes = Lists.newArrayList("", "", "", "", "Klein", "Gro\u00df", "Nieder", "Ober", "Bad ", "Gau-");
		private static final List<String> germanNameInfixes = Lists.newArrayList("", "sege", "m\u00fchl", "s\u00e4ngers", "bach", "stein", "holz", "w\u00fcrz", "h\u00f6ch", "wolfs", "katz", "lauter", "hildes", "heides", "ochsen", "k\u00f6nigs", "neu", "schaf", "rotten");
		private static final List<String> germanNamePostfixes = Lists.newArrayList("heim", "stadt", "stedt", "berg", "tal", "hausen", "dorf", "ingen", "burg", "furt", "haven", "felde", "br\u00fcck", "br\u00fccken", "kirch", "horn");

		private static final List<String> englishNamePrefixes = Lists.newArrayList("", "", "", "", "", "", "", "", "", "", "", "little ", "grand ", "St ", "new ");
		private static final List<String> englishNameInfixes = Lists.newArrayList("black", "bow", "long", "cal", "glen", "elk", "taylors", "man", "spring", "cats", "brad", "leakes", "singers", "thorn", "lake", "burn", "chip", "brace", "raven", "middle");
		private static final List<String> englishNamePostfixes = Lists.newArrayList("ville", "bridge", "ham", " island", "cester", "water", "town", " creek", " valley", "view", "bury", "burgh", "ington", "field", "dale", " port", "worth", "sey", "don", "pool", "wood", "ley", "ford", " hill", "gate");

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
			builder.comment("You can define which waystone models are enabled.",
				"Disabled types are still in the game but cannot be crafted and disappear from the creative menu.",
				"The available variants are: " +
					ModelWaystone.variants.stream()
						.map(v -> "\"" + v.name + "\"")
						.collect(Collectors.joining(", ")),
				"Check out the curseforge page to see what they look like: https://www.curseforge.com/minecraft/mc-mods/signpost/pages/waystone-models"
			);
			allowedWaystones = builder.defineList(
				"allowed_waystone_models",
				ModelWaystone.variants.stream().map(v -> v.name).collect(Collectors.toList()),
				n -> n instanceof String &&
					ModelWaystone.variants.contains(new ModelWaystone.Variant((String) n, null, 0
			)));
			editLockedWaystoneCommandPermissionLevel = builder.define("edit_locked_waystones_command_permission_level", 3);

			builder.push("world_gen");
				allowedVillageWaystones = builder.defineList(
					"allowed_waystone_models",
					Lists.newArrayList("simple0", "simple1", "simple2", "detailed0", "detailed1"), n -> n instanceof String &&
							ModelWaystone.variants.contains(new ModelWaystone.Variant((String) n, null, 0
					)));

				builder.comment("", "The names of waystones generated in villages will consist of a prefix, an infix and a postfix, each randomly selected from these lists.",
					"e.g.: If \"tol \", \"ker\" and \"dra\" are selected, the name will be \"Tol Kerdra\""
				);
				builder.comment(
					"Here are some language-specific examples:",
					"german:",
					"\tvillage_name_prefixes = [\"" + String.join("\", \"", germanNamePrefixes) + "\"]",
					"\tvillage_name_infixes = [\"" + String.join("\", \"", germanNameInfixes) + "\"]",
					"\tvillage_name_postfixes = [\"" + String.join("\", \"", germanNamePostfixes) + "\"]",
					"english:",
					"\tvillage_name_prefixes = [\"" + String.join("\", \"", englishNamePrefixes) + "\"]",
					"\tvillage_name_infixes = [\"" + String.join("\", \"", englishNameInfixes) + "\"]",
					"\tvillage_name_postfixes = [\"" + String.join("\", \"", englishNamePostfixes) + "\"]"
				);
				villageNamePrefixes = builder.defineList(
					"village_name_prefixes",
					englishNamePrefixes,
					x -> x instanceof String
				);
				villageNameInfixes = builder.defineList(
					"village_name_infixes",
					englishNameInfixes,
					x -> x instanceof String
				);
				villageNamePostfixes = builder.defineList(
					"village_name_postfixes",
					englishNamePostfixes,
					x -> x instanceof String
				);
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
