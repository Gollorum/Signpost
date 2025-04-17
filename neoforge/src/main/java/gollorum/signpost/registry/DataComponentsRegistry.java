package gollorum.signpost.registry;

import gollorum.signpost.minecraft.data.PostData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static gollorum.signpost.Signpost.MOD_ID;

public class DataComponentsRegistry {

    private static final DeferredRegister<DataComponentType<?>> Register = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PostData>> POST_DATA =
        Register.register("post_data", () -> PostData.TYPE);


    public static void register(IEventBus bus){
        Register.register(bus);
    }
}
