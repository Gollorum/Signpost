package gollorum.signpost;

import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.utils.IDelay;
import gollorum.signpost.utils.ServerType;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Signpost {

    public static final String MOD_ID = "signpost";
    public static final String MOD_NAME = "signpost";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static MinecraftServer serverInstance;
    public static MinecraftServer getServerInstance() { return serverInstance; }

    private static Supplier<IConfig> config;
    public static IConfig getConfig() { return config.get(); }

    private static IDelay delay;
    public static IDelay getDelay() { return delay; }

    public static ServerType getServerType() {
        return serverInstance == null
            ? ServerType.ConnectedClient
            : serverInstance.isDedicatedServer()
            ? ServerType.Dedicated
            : ServerType.HostingClient;
    }

    public static Consumer<MinecraftServer> init(IConfig config, IDelay delay) {
        return init(() -> config, delay);
    }

    public static Consumer<MinecraftServer> init(Supplier<IConfig> config, IDelay delay) {
        Signpost.config = config;
        Signpost.delay = delay;
        return server -> serverInstance = server;
    }

}