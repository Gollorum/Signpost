package gollorum.signpost;

import gollorum.signpost.compat.Compat;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.utils.ServerType;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class Signpost {

    public static final String MOD_ID = "signpost";
    public static final String MOD_NAME = "signpost";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static MinecraftServer serverInstance;
    public static MinecraftServer getServerInstance() { return serverInstance; }

    public static ServerType getServerType() {
        return serverInstance == null
            ? ServerType.ConnectedClient
            : serverInstance.isDedicatedServer()
            ? ServerType.Dedicated
            : ServerType.HostingClient;
    }

    public static Consumer<MinecraftServer> init() {
        Compat.register();

        return server -> serverInstance = server;
    }

}