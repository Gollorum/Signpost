package gollorum.signpost.minecraft.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ListWaystones {

	public static ArgumentBuilder<CommandSource, ?> register() {
		return Commands.literal("list")
			.requires(source -> source.hasPermissionLevel(3))
			.executes(context -> {
				WaystoneLibrary.getInstance().requestAllWaystoneNames(names ->
					context.getSource().sendFeedback(
						names.isEmpty()
							? new TranslationTextComponent(LangKeys.noWaystones)
							: new StringTextComponent(String.join(
								"\n",
								names.values()
							)),
						false
					)
				);
				return Command.SINGLE_SUCCESS;
			});
	}

}
