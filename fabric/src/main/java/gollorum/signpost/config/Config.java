package gollorum.signpost.config;

// Fabric/AutoConfig imports
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.config.IPermissionConfig;
import gollorum.signpost.minecraft.config.ITeleportConfig;
import gollorum.signpost.minecraft.config.IWorldGenConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import java.util.List;
import java.util.stream.Collectors;

@me.shedaniel.autoconfig.annotation.Config(name = Signpost.MOD_ID)
public class Config implements ConfigData, IConfig {
    @ConfigEntry.Gui.CollapsibleObject
    public CommonConfig common = new CommonConfig();
    @ConfigEntry.Gui.CollapsibleObject
    public ClientConfig client = new ClientConfig();
    @ConfigEntry.Gui.CollapsibleObject
    public ServerConfig server = new ServerConfig();

    @Override
    public IServer getServer() {
        return server;
    }

    @Override
    public ICommon getCommon() {
        return common;
    }

    @Override
    public IClient getClient() {
        return client;
    }

    public static void register() {
        AutoConfig.register(Config.class, GsonConfigSerializer::new);
    }

    public static Config get() {
        return AutoConfig.getConfigHolder(Config.class).getConfig();
    }

    public static class ServerConfig implements IConfig.IServer {
        @ConfigEntry.Gui.CollapsibleObject
        public TeleportConfig teleport = new TeleportConfig();
        @ConfigEntry.Gui.Tooltip(count = 4)
        public List<String> allowedWaystones = ModelWaystone.variants.stream().map(v -> v.name).collect(Collectors.toList());
        @ConfigEntry.Gui.CollapsibleObject
        public PermissionConfig permissions = new PermissionConfig();

        @Override
        public boolean isLoaded() {
            return true;
        }

        @Override
        public ITeleportConfig teleport() {
            return teleport;
        }

        @Override
        public IWorldGenConfig worldGen() {
            return Config.get().common.worldGen;
        }

        @Override
        public IPermissionConfig permissions() {
            return permissions;
        }

        @Override
        public List<? extends String> allowedWaystones() {
            return allowedWaystones;
        }
    }

    public static class CommonConfig implements IConfig.ICommon {
        @ConfigEntry.Gui.CollapsibleObject
        public WorldGenConfig worldGen = new WorldGenConfig();

    }

    public static class ClientConfig implements IConfig.IClient {
        @ConfigEntry.Gui.Tooltip(count = 4)
        public boolean enableConfirmationScreen = true;

        @Override
        public boolean enableConfirmationScreen() {
            return enableConfirmationScreen;
        }
    }
}
