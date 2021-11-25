package gollorum.signpost.minecraft.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.commands.*;
import net.minecraft.command.CommandSource;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

@Mod.EventBusSubscriber(modid = Signpost.MOD_ID, bus = FORGE)
public class CommandRegistry {

	@SubscribeEvent
	public static void onRegisterCommands(FMLServerStartingEvent event) {
		CommandDispatcher<CommandSource> dispatcher = event.getCommandDispatcher();
		dispatcher.register(
			LiteralArgumentBuilder.<CommandSource>literal(Signpost.MOD_ID)
				.then(ListWaystones.register())
				.then(DiscoverWaystone.register())
				.then(Teleport.register())
				.then(BlockRestrictions.register())
		);
	}

}
