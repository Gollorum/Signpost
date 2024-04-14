package gollorum.signpost;

import net.fabricmc.api.ModInitializer;

public class SignpostFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        
        // This method is invoked by the Fabric mod loader when it is ready
        // to load your mod. You can access Fabric and Common code in this
        // project.

        // Use Fabric to bootstrap the Common mod.
        Signpost.LOG.info("Hello Fabric world!");
        Signpost.init();
    }
}
