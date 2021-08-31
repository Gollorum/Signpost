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
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WaystoneArgument implements ArgumentType<String> {

	static {
		ArgumentTypes.register(Signpost.MOD_ID + ":waystone", WaystoneArgument.class, new ArgumentSerializer<>() {
			@Override
			public void serializeToNetwork(WaystoneArgument arg, FriendlyByteBuf buffer) {}

			@Override
			public WaystoneArgument deserializeFromNetwork(FriendlyByteBuf buffer) {
				return new WaystoneArgument();
			}

			@Override
			public void serializeToJson(WaystoneArgument arg, JsonObject buffer) {}
		});
	}

	private static final Pattern nonLiteralPattern = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		String name = reader.readString();
		if(WaystoneLibrary.hasInstance() && WaystoneLibrary.getInstance().getAllWaystoneNames().map(n -> n.contains(name)).orElse(true))
			return name;
		else throw new SimpleCommandExceptionType(new TranslatableComponent(LangKeys.waystoneNotFound, Colors.wrap(name, Colors.highlight))).create();
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
			Optional.empty()
		);
		return ret;
	}

}
