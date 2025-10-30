package gollorum.signpost.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.commands.DiscoverWaystone;
import gollorum.signpost.minecraft.commands.ListWaystones;
import gollorum.signpost.minecraft.commands.Teleport;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Signpost.MOD_ID)
public class CommandRegistry {

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(
			LiteralArgumentBuilder.<CommandSourceStack>literal(Signpost.MOD_ID)
				.then(ListWaystones.register())
				.then(DiscoverWaystone.register())
				.then(Teleport.register())
		);
	}

}
