package gollorum.signpost.registry;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.commands.DiscoverWaystone;
import gollorum.signpost.minecraft.commands.ListWaystones;
import gollorum.signpost.minecraft.commands.Teleport;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;

public class CommandRegistry {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal(Signpost.MOD_ID)
                    .then(ListWaystones.register())
                    .then(DiscoverWaystone.register())
                    .then(Teleport.register())
            ));
	}

}
