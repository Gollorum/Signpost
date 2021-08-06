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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Optional;

public class BlockRestrictions {

	public static final String commandName = "blockRestrictions";

	public static ArgumentBuilder<CommandSource, ?> register() {
		return Commands.literal(commandName)
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
				return get(type, context.getSource(), player);
			})
			.then(Commands.argument("player", EntityArgument.player())
				.requires(source -> source.hasPermissionLevel(3))
				.executes(context -> get(
					type,
					context.getSource(),
					EntityArgument.getPlayer(context, "player")
				)));
	}

	private static int get(
		gollorum.signpost.BlockRestrictions.Type type, CommandSource commandSource, PlayerEntity targetedPlayer
	) {
		int left = gollorum.signpost.BlockRestrictions.getInstance().getRemaining(type, PlayerHandle.from(targetedPlayer));
		Optional<ITextComponent> subject = PlayerHandle.from(commandSource.getEntity()).equals(PlayerHandle.from(targetedPlayer)) || targetedPlayer == null
			? Optional.empty()
			: Optional.of(targetedPlayer.getDisplayName());
		commandSource.sendFeedback(left < 0 ?
			type.getUnlimitedRemainingTextComponent(subject) :
			type.getRemainingTextComponent(left, subject), false);
		return Command.SINGLE_SUCCESS;
	}

	private static ArgumentBuilder<CommandSource, ?> setter(gollorum.signpost.BlockRestrictions.Type type) {
		return LiteralArgumentBuilder.<CommandSource>literal("set")
			.requires(source -> source.hasPermissionLevel(3))
			.then(Commands.argument("count", IntegerArgumentType.integer(-1))
				.executes(context -> {
					PlayerEntity player = context.getSource().asPlayer();
					return set(type, context.getSource(), player, IntegerArgumentType.getInteger(context, "count"));
				})
				.then(Commands.argument("player", EntityArgument.player())
					.executes(context -> set(
						type,
						context.getSource(),
						EntityArgument.getPlayer(context, "player"),
						IntegerArgumentType.getInteger(context, "count")
					))));
	}

	private static int set(gollorum.signpost.BlockRestrictions.Type type, CommandSource commandSource, PlayerEntity targetedPlayer, int count) {
		PlayerHandle tHandle = PlayerHandle.from(targetedPlayer);
		gollorum.signpost.BlockRestrictions.getInstance().setRemaining(type, tHandle, c -> count);
		Optional<ITextComponent> subject = PlayerHandle.from(commandSource.getEntity()).equals(tHandle) || targetedPlayer == null
			? Optional.empty()
			: Optional.of(targetedPlayer.getDisplayName());
		commandSource.sendFeedback(count < 0 ?
			type.getUnlimitedRemainingTextComponent(subject) :
			type.getRemainingTextComponent(count, subject), true);
		return Command.SINGLE_SUCCESS;
	}

}
