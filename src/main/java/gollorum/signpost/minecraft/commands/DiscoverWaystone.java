package gollorum.signpost.minecraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;

public class DiscoverWaystone {

	public static ArgumentBuilder<CommandSource, ?> register() {
		return Commands.literal("discover")
			.requires(source -> source.hasPermissionLevel(3))
			.then(Commands.argument("waystone", new WaystoneArgument())
				.requires(source -> {
					try {
						source.asPlayer();
						return true;
					} catch (CommandSyntaxException e) {
						return false;
					}
				})
				.executes(context -> execute(
					context.getArgument("waystone", String.class),
					context.getSource().asPlayer()
			)))
			.then(Commands.argument("waystone", new WaystoneArgument())
				.then(Commands.argument("player", EntityArgument.player())
					.executes(context -> execute(
						context.getArgument("waystone", String.class),
						EntityArgument.getPlayer(context, "player")
					))));
	}

	private static int execute(String name, PlayerEntity player) throws CommandSyntaxException {
		WaystoneHandle handle = WaystoneLibrary.getInstance().getHandleByName(name)
			.orElseThrow(() -> new SimpleCommandExceptionType(new TranslationTextComponent(LangKeys.waystoneNotFound, Colors.wrap(name, Colors.highlight))).create());
		if(WaystoneLibrary.getInstance().addDiscovered(new PlayerHandle(player), handle)) {
			player.sendMessage(new TranslationTextComponent(LangKeys.discovered, Colors.wrap(name, Colors.highlight)), Util.DUMMY_UUID);
		}
		return Command.SINGLE_SUCCESS;
	}

}
