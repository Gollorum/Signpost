package gollorum.signpost.minecraft;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

	public static final Server Server;
	private static final ForgeConfigSpec ServerConfig;

	public static final Client Client;
	private static final ForgeConfigSpec ClientConfig;

	static {
		Pair<Server, ForgeConfigSpec> serverPair = new ForgeConfigSpec.Builder().configure(Server::new);
		Server = serverPair.getKey();
		ServerConfig = serverPair.getValue();
		Pair<Client, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(Client::new);
		Client = clientPair.getKey();
		ClientConfig = clientPair.getValue();
	}

	public static void register() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig);
	}

	public static class Server {

		public final ForgeConfigSpec.BooleanValue enableTeleport;
		public final ForgeConfigSpec.IntValue maximumDistance;
		public final ForgeConfigSpec.BooleanValue enforceDiscovery;

		public Server(ForgeConfigSpec.Builder builder) {
			builder.push("teleport");
			enableTeleport = builder.define("enable", true);
			maximumDistance = builder.defineInRange("max_distance", -1, -1, Integer.MAX_VALUE);
			enforceDiscovery = builder.define("enforce_discovery", true);
			builder.pop();
		}

	}

	public static class Client {

		public final ForgeConfigSpec.BooleanValue enableConfirmationScreen;

		public Client(ForgeConfigSpec.Builder builder) {
			builder.push("teleport");
			enableConfirmationScreen = builder.define("enable_confirmation_sreen", false);
			builder.pop();
		}

	}

}
