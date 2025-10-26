package gollorum.signpost;

import gollorum.signpost.networking.FabricPacketHandler;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.SERVER)
public class SignpostFabricDedicatedServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        FabricPacketHandler.initialize(false);
    }
}
