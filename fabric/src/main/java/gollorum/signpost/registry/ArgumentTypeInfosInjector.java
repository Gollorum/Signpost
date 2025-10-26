package gollorum.signpost.registry;

import gollorum.signpost.minecraft.commands.WaystoneArgument;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.resources.ResourceLocation;

import static gollorum.signpost.Signpost.MOD_ID;

public class ArgumentTypeInfosInjector {

    public static void register() {
        ArgumentTypeRegistry.registerArgumentType(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "waystone"),
            WaystoneArgument.class,
            new WaystoneArgument.Info()
        );

    }

}