package gollorum.signpost.minecraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TextComponents;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Optional;

public class ListWaystones {

	public static ArgumentBuilder<CommandSource, ?> register() {
		return Commands.literal("list")
			.requires(source -> source.hasPermission(Config.Server.permissions.listPermissionLevel.get()))
			.executes(context -> {
				WaystoneLibrary.getInstance().requestAllWaystoneNames(names ->
					context.getSource().sendSuccess(
						names.values().stream().map(n -> TextComponents.waystone(
							context.getSource().getEntity() instanceof ServerPlayerEntity
								? (ServerPlayerEntity) context.getSource().getEntity()
								: null,
							n,
							false
						))
							.reduce((l, r) -> {
								l.append(new StringTextComponent("\n").append(r));
								return l;
							}).orElseGet(() -> new TranslationTextComponent(LangKeys.noWaystones)),
						false
					), Optional.empty()
				);
				return Command.SINGLE_SUCCESS;
			});
	}

}
