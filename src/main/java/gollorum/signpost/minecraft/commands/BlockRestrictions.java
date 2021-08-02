package gollorum.signpost.minecraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gollorum.signpost.PlayerHandle;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;

public class BlockRestrictions {

	public static final String commandName = "blockRestrictions";

	public static ArgumentBuilder<CommandSource, ?> register() {
		return Commands.literal(commandName)
			.requires(CommandUtils::isPlayer)
			.then(signposts())
			.then(waystone());
	}

	private static ArgumentBuilder<CommandSource, ?> signposts() {
		return LiteralArgumentBuilder.<CommandSource>literal("signposts")
			.then(getter(gollorum.signpost.BlockRestrictions.Type.Signpost))
			.then(setter(gollorum.signpost.BlockRestrictions.Type.Signpost));
	}

	private static ArgumentBuilder<CommandSource, ?> waystone() {
		return LiteralArgumentBuilder.<CommandSource>literal("waystone")
			.then(getter(gollorum.signpost.BlockRestrictions.Type.Waystone))
			.then(setter(gollorum.signpost.BlockRestrictions.Type.Waystone));
	}

	private static ArgumentBuilder<CommandSource, ?> getter(gollorum.signpost.BlockRestrictions.Type type) {
		return LiteralArgumentBuilder.<CommandSource>literal("get")
			.executes(context -> {
				PlayerEntity player = context.getSource().asPlayer();
				return get(type, player, PlayerHandle.from(player));
			})
			.then(Commands.argument("player", EntityArgument.player())
				.requires(source -> source.hasPermissionLevel(3))
				.executes(context -> get(
					type,
					context.getSource().asPlayer(),
					PlayerHandle.from(EntityArgument.getPlayer(context, "player"))
				)));
	}

	private static int get(
		gollorum.signpost.BlockRestrictions.Type type, PlayerEntity callingPlayer, PlayerHandle targetedPlayer
	) {
		int left = gollorum.signpost.BlockRestrictions.getInstance().getRemaining(type, targetedPlayer);
		boolean isCallerSubject = PlayerHandle.from(callingPlayer).equals(targetedPlayer);
		callingPlayer.sendMessage(left < 0 ?
			new TranslationTextComponent(type.getUnlimitedRemainingLangKey(isCallerSubject)) :
			new TranslationTextComponent(type.getRemainingLangKey(isCallerSubject), left), Util.DUMMY_UUID);
		return Command.SINGLE_SUCCESS;
	}

	private static ArgumentBuilder<CommandSource, ?> setter(gollorum.signpost.BlockRestrictions.Type type) {
		return LiteralArgumentBuilder.<CommandSource>literal("set")
			.requires(source -> source.hasPermissionLevel(3))
			.then(Commands.argument("count", IntegerArgumentType.integer(-1))
				.executes(context -> {
					PlayerEntity player = context.getSource().asPlayer();
					return set(type, player, PlayerHandle.from(player), IntegerArgumentType.getInteger(context, "count"));
				})
				.then(Commands.argument("player", EntityArgument.player())
					.executes(context -> set(
						type,
						context.getSource().asPlayer(),
						PlayerHandle.from(EntityArgument.getPlayer(context, "player")),
						IntegerArgumentType.getInteger(context, "count")
					))));
	}

	private static int set(gollorum.signpost.BlockRestrictions.Type type, PlayerEntity callingPlayer, PlayerHandle targetedPlayer, int count) {
		gollorum.signpost.BlockRestrictions.getInstance().setRemaining(type, targetedPlayer, c -> count);
		boolean isCallerSubject = PlayerHandle.from(callingPlayer).equals(targetedPlayer);
		callingPlayer.sendMessage(count < 0 ?
			new TranslationTextComponent(type.getUnlimitedRemainingLangKey(isCallerSubject)) :
			new TranslationTextComponent(type.getRemainingLangKey(isCallerSubject), count), Util.DUMMY_UUID);
		return Command.SINGLE_SUCCESS;
	}

}
