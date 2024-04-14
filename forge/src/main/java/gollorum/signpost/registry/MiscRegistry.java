package gollorum.signpost.registry;

import gollorum.signpost.minecraft.commands.WaystoneArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import static gollorum.signpost.Signpost.MOD_ID;

public class MiscRegistry {

    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> WAYSTONE_ARGUMENT = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, MOD_ID);

    static {
        WAYSTONE_ARGUMENT.register("waystone", () -> ArgumentTypeInfos.registerByClass(WaystoneArgument.class, new WaystoneArgument.Info()));
    }

    public static void register(IEventBus bus){ WAYSTONE_ARGUMENT.register(bus);	}

}
