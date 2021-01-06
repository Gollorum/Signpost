package gollorum.signpost.minecraft.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.commands.*;
import net.minecraft.command.CommandSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CommandRegistry {

	@SubscribeEvent
	static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
		dispatcher.register(
			LiteralArgumentBuilder.<CommandSource>literal(Signpost.MOD_ID)
				.then(ListWaystones.register(dispatcher))
				.then(DiscoverWaystone.register(dispatcher))
				.then(Teleport.register(dispatcher))
		);
	}

	public static void register(IEventBus bus) { bus.register(CommandRegistry.class); }

}
