package gollorum.signpost.minecraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.minecraft.config.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class BlockRestrictions {

	public static final String commandName = "blockRestrictions";

	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal(commandName)
			.then(signposts())
			.then(waystone());
	}

	private static ArgumentBuilder<CommandSourceStack, ?> signposts() {
		return LiteralArgumentBuilder.<CommandSourceStack>literal("signposts")
			.then(getter(gollorum.signpost.BlockRestrictions.Type.Signpost))
			.then(setter(gollorum.signpost.BlockRestrictions.Type.Signpost));
	}

	private static ArgumentBuilder<CommandSourceStack, ?> waystone() {
		return LiteralArgumentBuilder.<CommandSourceStack>literal("waystone")
			.then(getter(gollorum.signpost.BlockRestrictions.Type.Waystone))
			.then(setter(gollorum.signpost.BlockRestrictions.Type.Waystone));
	}

	private static ArgumentBuilder<CommandSourceStack, ?> getter(gollorum.signpost.BlockRestrictions.Type type) {
		return LiteralArgumentBuilder.<CommandSourceStack>literal("get")
			.executes(context -> {
				Player player = context.getSource().getPlayerOrException();
				return get(type, context.getSource(), player);
			})
			.then(Commands.argument("player", EntityArgument.player())
				.requires(source -> source.hasPermission(3))
				.executes(context -> get(
					type,
					context.getSource(),
					EntityArgument.getPlayer(context, "player")
				)));
	}

	private static int get(
		gollorum.signpost.BlockRestrictions.Type type, CommandSourceStack commandSource, Player targetedPlayer
	) {
		int left = gollorum.signpost.BlockRestrictions.getInstance().getRemaining(type, PlayerHandle.from(targetedPlayer));
		Optional<Component> subject = PlayerHandle.from(commandSource.getEntity()).equals(PlayerHandle.from(targetedPlayer)) || targetedPlayer == null
			? Optional.empty()
			: Optional.of(targetedPlayer.getDisplayName());
		commandSource.sendSuccess(left < 0 ?
			type.getUnlimitedRemainingTextComponent(subject) :
			type.getRemainingTextComponent(left, subject), false);
		return Command.SINGLE_SUCCESS;
	}

	private static ArgumentBuilder<CommandSourceStack, ?> setter(gollorum.signpost.BlockRestrictions.Type type) {
		return LiteralArgumentBuilder.<CommandSourceStack>literal("set")
			.requires(source -> source.hasPermission(Config.Server.permissions.setBlockResPermissionLevel.get()))
			.then(Commands.argument("count", IntegerArgumentType.integer(-1))
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
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

	private static int set(gollorum.signpost.BlockRestrictions.Type type, CommandSourceStack commandSource, ServerPlayer targetedPlayer, int count) {
		PlayerHandle tHandle = PlayerHandle.from(targetedPlayer);
		gollorum.signpost.BlockRestrictions.getInstance().setRemaining(type, tHandle, c -> count);
		Optional<Component> subject = PlayerHandle.from(commandSource.getEntity()).equals(tHandle) || targetedPlayer == null
			? Optional.empty()
			: Optional.of(targetedPlayer.getDisplayName());
		commandSource.sendSuccess(count < 0 ?
			type.getUnlimitedRemainingTextComponent(subject) :
			type.getRemainingTextComponent(count, subject), true);
		return Command.SINGLE_SUCCESS;
	}

}
