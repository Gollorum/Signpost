package gollorum.signpost.minecraft.config;

import gollorum.signpost.platform.Services;

import java.util.List;

public interface IConfig {

    static IConfig getInstance() { return Services.CONFIG; }

	IServer getServer();
	ICommon getCommon();
	IClient getClient();


	interface IServer {

        static IServer getInstance() { return Services.CONFIG.getServer(); }

        ITeleportConfig teleport();
		IWorldGenConfig worldGen();
		IPermissionConfig permissions();

		List<? extends String> allowedWaystones();
	}

    interface ICommon {

        static ICommon getInstance() { return Services.CONFIG.getCommon(); }

        IWorldGenConfig worldGenDefaults();
	}

    interface IClient {

        static IClient getInstance() { return Services.CONFIG.getClient(); }

        boolean enableConfirmationScreen();
		boolean enableWaystoneLimitNotifications();
		boolean enableSignpostLimitNotifications();
	}

}
