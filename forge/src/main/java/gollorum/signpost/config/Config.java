package gollorum.signpost.config;

import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.config.IPermissionConfig;
import gollorum.signpost.minecraft.config.ITeleportConfig;
import gollorum.signpost.minecraft.config.IWorldGenConfig;
import gollorum.signpost.utils.Tuple;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;
import java.util.stream.Collectors;

public class Config implements IConfig {

    public static final Config INSTANCE = new Config();

    public final Server Server;
    public final ForgeConfigSpec ServerConfig;
    @Override
    public IServer getServer() { return Server; }

    public final Common Common;
    private final ForgeConfigSpec CommonConfig;
    @Override
    public ICommon getCommon() { return Common; }

    public final Client Client;
    private final ForgeConfigSpec ClientConfig;
    @Override
    public IClient getClient() { return Client; }

    public Config() {
        var serverTuple = Tuple.from(new ForgeConfigSpec.Builder().configure(Server::new));
        Server = serverTuple._1();
        ServerConfig = serverTuple._2();
        var commonTuple = Tuple.from(new ForgeConfigSpec.Builder().configure(Common::new));
        Common = commonTuple._1();
        CommonConfig = commonTuple._2();
        var clientTuple = Tuple.from(new ForgeConfigSpec.Builder().configure(Client::new));
        Client = clientTuple._1();
        ClientConfig = clientTuple._2();
    }

    public void register(ModLoadingContext modLoadingContext) {
        modLoadingContext.registerConfig(ModConfig.Type.SERVER, ServerConfig);
        modLoadingContext.registerConfig(ModConfig.Type.COMMON, CommonConfig);
        modLoadingContext.registerConfig(ModConfig.Type.CLIENT, ClientConfig);
    }

    public static class Server implements IServer {

        private final ITeleportConfig teleport;
        private final IWorldGenConfig worldGen;
        private final PermissionConfig permissions;

        private final ForgeConfigSpec.ConfigValue<List<? extends String>> allowedWaystones;

        @Override
        public List<? extends String> allowedWaystones() { return allowedWaystones.get(); }

        @Override
        public IPermissionConfig permissions() { return permissions; }

        @Override
        public IWorldGenConfig worldGen() { return worldGen; }

        @Override
        public boolean isLoaded() {
            return INSTANCE.ServerConfig.isLoaded();
        }

        @Override
        public ITeleportConfig teleport() { return teleport; }

        public Server(ForgeConfigSpec.Builder builder) {
            builder.push("teleport");
            teleport = new TeleportConfig(builder);
            builder.pop();
            allowedWaystones = builder.comment("You can define which waystone models are enabled.",
                    "Disabled types are still in the game but disappear from the creative menu. Use data packs to remove the stonecutter recipes as well.",
                    "The available variants are: " +
                        ModelWaystone.variants.stream()
                            .map(v -> "\"" + v.name + "\"")
                            .collect(Collectors.joining(", ")),
                    "Check out the curseforge page to see what they look like: https://www.curseforge.com/minecraft/mc-mods/signpost/pages/waystone-models"
                ).worldRestart()
                .defineList(
                    "allowed_waystone_models",
                    ModelWaystone.variants.stream().map(v -> v.name).collect(Collectors.toList()),
                    n -> n instanceof String &&
                        ModelWaystone.variants.contains(new ModelWaystone.Variant((String) n, null, null, 0
                        )));

            builder.push("permissions");
            permissions = new PermissionConfig(builder);
            builder.pop();

            builder.push("world_gen");
            worldGen = new WorldGenConfig(builder, true);
            builder.pop();
        }

    }

    public static class Common implements ICommon {

        public final WorldGenConfig worldGenDefaults;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("world_gen_defaults");
            worldGenDefaults = new WorldGenConfig(builder, false);
            builder.pop();

        }

    }

    public static class Client implements IClient {

        private final ForgeConfigSpec.BooleanValue enableConfirmationScreen;

        @Override
        public boolean enableConfirmationScreen() { return enableConfirmationScreen.get(); }

        public Client(ForgeConfigSpec.Builder builder) {
            builder.push("teleport");

            enableConfirmationScreen = builder
                .comment(
                    "Defines whether the confirmation screen pops when using a sign to teleport.",
                    "CAUTION 1: The necessary items will be removed without notice if costs are involved.",
                    "CAUTION 2: The only way to edit a sign with destination is through this screen.",
                    "This should probably never be turned off. Why did I make it an option? No idea."
                ).define("enable_confirmation_screen", true);
            builder.pop();
        }

    }

}
