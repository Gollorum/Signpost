package gollorum.signpost.minecraft.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.gui.LangKeys;
import net.minecraft.util.text.TranslationTextComponent;

public class WaystoneArgument implements ArgumentType<String> {

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		String name = reader.readString();
		if(WaystoneLibrary.hasInstance() && WaystoneLibrary.getInstance().getAllWaystoneNames().map(n -> n.contains(name)).orElse(true))
			return name;
		else throw new SimpleCommandExceptionType(new TranslationTextComponent(LangKeys.waystoneNotFound, name)).create();
	}

}
