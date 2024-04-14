package gollorum.signpost.platform;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.platform.services.IPlatformHelper;
import gollorum.signpost.utils.IDelay;

import java.util.ServiceLoader;

public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static final PacketHandler PACKET_HANDLER = load(PacketHandler.class);

    public static final IConfig CONFIG = load(IConfig.class);

    public static final IDelay DELAY = load(IDelay.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Signpost.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}