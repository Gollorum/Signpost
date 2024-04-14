package gollorum.signpost;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Signpost.MOD_ID)
public class SignpostNeoforge {

    public SignpostNeoforge(IEventBus eventBus) {

        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        // Use NeoForge to bootstrap the Common mod.
        Signpost.LOG.info("Hello NeoForge world!");
        Signpost.init();

    }
}