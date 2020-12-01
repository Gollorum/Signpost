package gollorum.signpost.minecraft.block;

import net.minecraftforge.eventbus.api.IEventBus;

public class BlockEventListener {

    public static void register(IEventBus bus) { bus.register(BlockEventListener.class); }

}
