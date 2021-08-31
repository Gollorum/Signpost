package gollorum.signpost.minecraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Optional;

public class ListWaystones {

	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("list")
			.requires(source -> source.hasPermission(3))
			.executes(context -> {
				WaystoneLibrary.getInstance().requestAllWaystoneNames(names ->
					context.getSource().sendSuccess(
						names.isEmpty()
							? new TranslatableComponent(LangKeys.noWaystones)
							: new TextComponent(String.join(
								"\n",
								names.values()
							)),
						false
					), Optional.empty()
				);
				return Command.SINGLE_SUCCESS;
			});
	}

}
