package gollorum.signpost.minecraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TextComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.awt.*;
import java.util.Optional;

public class ListWaystones {

	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("list")
			.requires(source -> source.hasPermission(Config.Server.permissions.listPermissionLevel.get()))
			.executes(context -> {
				WaystoneLibrary.getInstance().requestAllWaystoneNames(names ->
					context.getSource().sendSuccess(() ->
						names.values().stream().map(n -> TextComponents.waystone(
							context.getSource().getEntity() instanceof ServerPlayer
								? (ServerPlayer) context.getSource().getEntity()
								: null,
							n,
							false
						))
							.reduce((l, r) -> {
								l.append(Component.literal("\n").append(r));
								return l;
							}).orElseGet(() -> Component.translatable(LangKeys.noWaystones)),
						false
					), Optional.empty(),
					false
				);
				return Command.SINGLE_SUCCESS;
			});
	}

}
