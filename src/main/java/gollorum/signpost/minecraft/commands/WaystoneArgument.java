package gollorum.signpost.minecraft.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static gollorum.signpost.Signpost.MOD_ID;

public class WaystoneArgument implements ArgumentType<String> {

	private static final DeferredRegister<ArgumentTypeInfo<?, ?>> Register = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, MOD_ID);

	static {
		Register.register("waystone", () -> ArgumentTypeInfos.registerByClass(WaystoneArgument.class, new Info()));
	}

	public static void register(IEventBus bus){ Register.register(bus);	}

	private static class Info implements ArgumentTypeInfo<WaystoneArgument, Info.Template> {

		@Override
		public void serializeToNetwork(Template arg, FriendlyByteBuf buffer) {}

		@Override
		public Template deserializeFromNetwork(FriendlyByteBuf buffer) {
			return new Template();
		}

		@Override
		public void serializeToJson(Template arg, JsonObject buffer) {}

		@Override
		public Template unpack(WaystoneArgument argument) { return new Template(); }

		public final class Template implements ArgumentTypeInfo.Template<WaystoneArgument> {

			@Override
			public WaystoneArgument instantiate(CommandBuildContext context) {
				return new WaystoneArgument();
			}

			@Override
			public ArgumentTypeInfo<WaystoneArgument, ?> type() { return Info.this; }
		}

	}

	private static final Pattern nonLiteralPattern = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		String name = reader.readString();
		if(WaystoneLibrary.hasInstance() && WaystoneLibrary.getInstance().getAllWaystoneNames(false).map(n -> n.contains(name)).orElse(true))
			return name;
		else throw new SimpleCommandExceptionType(Component.translatable(LangKeys.waystoneNotFound, Colors.wrap(name, Colors.highlight))).create();
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(
		CommandContext<S> context, SuggestionsBuilder builder
	) {
		if(!WaystoneLibrary.hasInstance()) return SharedSuggestionProvider.suggest(new HashSet<>(), builder);
		CompletableFuture<Suggestions> ret = new CompletableFuture<>();
		WaystoneLibrary.getInstance().requestAllWaystoneNames(
			names -> SharedSuggestionProvider.suggest(
				names.values().stream().map(s -> nonLiteralPattern.matcher(s).find() ? "\"" + s + "\"" : s).collect(Collectors.toSet()),
				builder
			).thenAccept(ret::complete),
			Optional.empty(),
			Signpost.getServerType().isClient
		);
		return ret;
	}

}
