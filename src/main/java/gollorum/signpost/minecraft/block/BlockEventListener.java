package gollorum.signpost.minecraft.block;

import gollorum.signpost.Signpost;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

// unused
//@Mod.EventBusSubscriber(modid = Signpost.MOD_ID)
public class BlockEventListener {

    public static void register(IEventBus bus) { bus.register(BlockEventListener.class); }

}
