package gollorum.signpost.minecraft.config;

import gollorum.signpost.Signpost;

import java.util.List;

public interface IConfig {

    static IConfig getInstance() { return Signpost.getConfig(); }

	IServer getServer();
	ICommon getCommon();
	IClient getClient();


	interface IServer {

        static IServer getInstance() { return Signpost.getConfig().getServer(); }

        boolean isLoaded();

        ITeleportConfig teleport();
		IWorldGenConfig worldGen();
		IPermissionConfig permissions();

		List<? extends String> allowedWaystones();
	}

    interface ICommon {

        static ICommon getInstance() { return Signpost.getConfig().getCommon(); }

    }

    interface IClient {

        static IClient getInstance() { return Signpost.getConfig().getClient(); }

        boolean enableConfirmationScreen();
	}

}
