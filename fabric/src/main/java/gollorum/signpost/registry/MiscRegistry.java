package gollorum.signpost.registry;

import gollorum.signpost.minecraft.commands.WaystoneArgument;
import gollorum.signpost.mixin.ArgumentTypeInfosAccessor;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import static gollorum.signpost.Signpost.MOD_ID;

public class MiscRegistry {

    private static final ArgumentTypeInfo<?, ?> WAYSTONE_ARGUMENT = new WaystoneArgument.Info();

    public static void register(){
        ArgumentTypeInfosAccessor.getByClassMap().put(WaystoneArgument.class, WAYSTONE_ARGUMENT);
        Registry.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, new ResourceLocation(MOD_ID, "waystone"), WAYSTONE_ARGUMENT);
    }

}
