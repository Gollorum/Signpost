package gollorum.signpost.minecraft.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;

public class CommandUtils {

	public static boolean isPlayer(CommandSource source){
		try {
			source.asPlayer();
			return true;
		} catch (CommandSyntaxException e) {
			return false;
		}
	}

}
