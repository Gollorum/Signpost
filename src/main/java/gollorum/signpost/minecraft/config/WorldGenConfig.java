package gollorum.signpost.minecraft.config;

import com.google.common.collect.Lists;
import gollorum.signpost.minecraft.block.ModelWaystone;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldGenConfig {

	public final ForgeConfigSpec.BooleanValue isVillageGenerationEnabled;
	public final ForgeConfigSpec.BooleanValue villagesOnlyTargetVillages;
	public final ForgeConfigSpec.IntValue maxSignpostsPerVillage;
	public final ForgeConfigSpec.ConfigValue<List<? extends String>> allowedVillageWaystones;
	public final ForgeConfigSpec.BooleanValue debugMode;
	public final Naming naming;

	private final boolean isServer;

	private <T> Supplier<T> defaults(T defaultVal, Function<WorldGenConfig, ForgeConfigSpec.ConfigValue<T>> factory) {
		return isServer ? () -> factory.apply(Config.Common.worldGenDefaults).get() : () -> defaultVal;
	}

	WorldGenConfig(ForgeConfigSpec.Builder builder, boolean isServer) {
		this.isServer = isServer;
		isVillageGenerationEnabled = builder.comment("Enables the generation of signposts and waystones in villages")
			.define("enable_generation", defaults(true, d -> d.isVillageGenerationEnabled));

		villagesOnlyTargetVillages = builder.comment("Defines whether village signposts can have any waystones as destination or just the ones generated in other villages")
			.define("only_target_other_villages", defaults(true, d -> d.villagesOnlyTargetVillages));

		maxSignpostsPerVillage = builder.comment("The maximum number of signposts that can spawn in one village")
			.defineInRange("max_signposts_per_village", defaults(2, d -> d.maxSignpostsPerVillage), 0, Integer.MAX_VALUE);

		allowedVillageWaystones = builder.comment(
			"Decide what waystone models are generated in villages",
			"You can look up the model names at https://www.curseforge.com/minecraft/mc-mods/signpost/pages/waystone-models"
		).defineList(
			"allowed_waystone_models",
			defaults(
				Stream.of(ModelWaystone.simple_0, ModelWaystone.simple_1, ModelWaystone.simple_2, ModelWaystone.detailed_0, ModelWaystone.detailed_1)
					.map(v -> v.name).collect(Collectors.toList()),
				d -> d.allowedVillageWaystones),
			n -> n instanceof String &&
				ModelWaystone.variants.contains(new ModelWaystone.Variant((String) n, null, null, 0
			)));

		debugMode = builder.comment(
			"Disables generator blocks/signs being replaced with actual ones and makes them visible. " +
			"Only enable this if you want to design structures with auto generated waystones / sign posts"
		).define("debug_mode", defaults(false, d -> d.debugMode));

		builder.push("naming");
		naming = new Naming(builder);
		builder.pop();
	}

	public class Naming {

		public final ForgeConfigSpec.ConfigValue<List<? extends String>> villageNamePrefixes;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> villageNameInfixes;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> villageNamePostfixes;

		private static final List<String> genericNamePrefixes = Lists.newArrayList("ak", "dev", "dol ", "ed", "il", "og", "por", "rov", "tek ", "tar", "tol ", "ves", "");
		private static final List<String> genericNameInfixes = Lists.newArrayList("do ", "en", "go", "na", "nah ", "ker", "ol", "ora", "ra", "rem", "ro");
		private static final List<String> genericNamePostfixes = Lists.newArrayList("ar", "blo", "bo", "bro", "do", "dra", "er", "ker", "lia", "tek");

		private static final List<String> germanNamePrefixes = Lists.newArrayList("", "", "", "", "klein", "gro\u00df", "nieder", "ober", "bad ", "hinter", "neu ");
		private static final List<String> germanNameInfixes = Lists.newArrayList("", "sege", "m\u00fchl", "s\u00e4ngers", "bach", "stein", "holz", "w\u00fcrz", "h\u00f6ch", "wolfs", "katz", "hunds", "lauter", "hildes", "heides", "ochsen", "ochs", "k\u00f6nigs", "neu", "schafs", "rotten", "ger", "schweins", "frank", "hexen", "münch", "ber", "see", "freuden");
		private static final List<String> germanNamePostfixes = Lists.newArrayList("heim", "stadt", "stedt", "berg", "tal", "hausen", "dorf", "ingen", "burg", "furt", "haven", "feld", "felde", "br\u00fcck", "br\u00fccken", "kirch", "horn", "brunn", "loch", "fluch", "en", "beck", "end", "walde", "wind", "garten", "ach", "au", "hofen");

		private static final List<String> englishNamePrefixes = Lists.newArrayList("", "", "", "", "", "", "", "", "", "", "", "little ", "grand ", "St ", "new ");
		private static final List<String> englishNameInfixes = Lists.newArrayList("black", "bow", "long", "cal", "glen", "elk", "taylors", "man", "spring", "cats", "brad", "leakes", "singers", "thorn", "lake", "burn", "chip", "brace", "raven", "middle");
		private static final List<String> englishNamePostfixes = Lists.newArrayList("ville", "bridge", "ham", " island", "cester", "water", "town", " creek", " valley", "view", "bury", "burgh", "ington", "field", "dale", " port", "worth", "sey", "don", "pool", "wood", "ley", "ford", " hill", "gate");

		Naming(ForgeConfigSpec.Builder builder) {
			villageNamePrefixes = builder.comment("The names of waystones generated in villages will consist of a prefix, an infix and a postfix, each randomly selected from these lists.",
				"e.g.: If \"tol \", \"ker\" and \"dra\" are selected, the name will be \"Tol Kerdra\"",
				"Here are some language-specific examples:",
				"english:",
				"village_name_prefixes = [\"" + String.join("\", \"", englishNamePrefixes) + "\"]",
				"village_name_infixes = [\"" + String.join("\", \"", englishNameInfixes) + "\"]",
				"village_name_postfixes = [\"" + String.join("\", \"", englishNamePostfixes) + "\"]",
				"german:",
				"village_name_prefixes = [\"" + String.join("\", \"", germanNamePrefixes) + "\"]",
				"village_name_infixes = [\"" + String.join("\", \"", germanNameInfixes) + "\"]",
				"village_name_postfixes = [\"" + String.join("\", \"", germanNamePostfixes) + "\"]"
			).defineList(
				"village_name_prefixes",
				defaults(genericNamePrefixes, d -> d.naming.villageNamePrefixes),
				x -> x instanceof String
			);
			villageNameInfixes = builder.defineList(
				"village_name_infixes",
				defaults(genericNameInfixes, d -> d.naming.villageNameInfixes),
				x -> x instanceof String
			);
			villageNamePostfixes = builder.defineList(
				"village_name_postfixes",
				defaults(genericNamePostfixes, d -> d.naming.villageNamePostfixes),
				x -> x instanceof String
			);
		}

	}

}
