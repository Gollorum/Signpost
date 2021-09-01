package gollorum.signpost.minecraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TextComponents;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;

public class DiscoverWaystone {

	public static ArgumentBuilder<CommandSource, ?> register() {
		return Commands.literal("discover")
			.requires(source -> source.hasPermission(Config.Server.permissions.discoverPermissionLevel.get()))
			.then(Commands.argument("waystone", new WaystoneArgument())
				.requires(source -> {
					try {
						source.getPlayerOrException();
						return true;
					} catch (CommandSyntaxException e) {
						return false;
					}
				})
				.executes(context -> execute(
					context.getArgument("waystone", String.class),
					context.getSource().getPlayerOrException()
			)))
			.then(Commands.argument("waystone", new WaystoneArgument())
				.then(Commands.argument("player", EntityArgument.player())
					.executes(context -> execute(
						context.getArgument("waystone", String.class),
						EntityArgument.getPlayer(context, "player")
					))));
	}

	private static int execute(String name, ServerPlayerEntity player) throws CommandSyntaxException {
		WaystoneHandle.Vanilla handle = WaystoneLibrary.getInstance().getHandleByName(name)
			.orElseThrow(() -> new SimpleCommandExceptionType(new TranslationTextComponent(LangKeys.waystoneNotFound, Colors.wrap(name, Colors.highlight))).create());
		if(WaystoneLibrary.getInstance().addDiscovered(new PlayerHandle(player), handle)) {
			player.sendMessage(new TranslationTextComponent(LangKeys.discovered, TextComponents.waystone(player, name)), Util.NIL_UUID);
		}
		return Command.SINGLE_SUCCESS;
	}

}
