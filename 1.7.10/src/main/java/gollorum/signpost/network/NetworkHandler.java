package gollorum.signpost.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import gollorum.signpost.Signpost;
import gollorum.signpost.network.handlers.BaseUpdateClientHandler;
import gollorum.signpost.network.handlers.BaseUpdateServerHandler;
import gollorum.signpost.network.handlers.ChatHandler;
import gollorum.signpost.network.handlers.InitPlayerResponseHandler;
import gollorum.signpost.network.handlers.OpenGuiHandler;
import gollorum.signpost.network.handlers.SendAllBigPostBasesHandler;
import gollorum.signpost.network.handlers.SendAllPostBasesHandler;
import gollorum.signpost.network.handlers.SendBigPostBasesHandler;
import gollorum.signpost.network.handlers.SendDiscoveredToServerHandler;
import gollorum.signpost.network.handlers.SendPostBasesHandler;
import gollorum.signpost.network.handlers.TeleportMeHandler;
import gollorum.signpost.network.handlers.TeleportRequestHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.BaseUpdateServerMessage;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.InitPlayerResponseMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.network.messages.SendAllBigPostBasesMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.network.messages.SendBigPostBasesMessage;
import gollorum.signpost.network.messages.SendDiscoveredToServerMessage;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.network.messages.TeleportMeMessage;
import gollorum.signpost.network.messages.TeleportRequestMessage;

public class NetworkHandler {

	public static final SimpleNetworkWrapper netWrap = NetworkRegistry.INSTANCE.newSimpleChannel(Signpost.MODID);
	
	public static void register(){
		netWrap.registerMessage(BaseUpdateServerHandler.class, BaseUpdateServerMessage.class, 0, Side.SERVER);
		netWrap.registerMessage(BaseUpdateClientHandler.class, BaseUpdateClientMessage.class, 1, Side.CLIENT);
		netWrap.registerMessage(SendDiscoveredToServerHandler.class, SendDiscoveredToServerMessage.class, 2, Side.SERVER);
		netWrap.registerMessage(InitPlayerResponseHandler.class, InitPlayerResponseMessage.class, 3, Side.CLIENT);
		netWrap.registerMessage(SendPostBasesHandler.class, SendPostBasesMessage.class, 4, Side.CLIENT);
		netWrap.registerMessage(SendPostBasesHandler.class, SendPostBasesMessage.class, 5, Side.SERVER);
		netWrap.registerMessage(SendAllPostBasesHandler.class, SendAllPostBasesMessage.class, 6, Side.CLIENT);
		netWrap.registerMessage(SendBigPostBasesHandler.class, SendBigPostBasesMessage.class, 7, Side.CLIENT);
		netWrap.registerMessage(SendBigPostBasesHandler.class, SendBigPostBasesMessage.class, 8, Side.SERVER);
		netWrap.registerMessage(SendAllBigPostBasesHandler.class, SendAllBigPostBasesMessage.class, 9, Side.CLIENT);
		netWrap.registerMessage(TeleportMeHandler.class, TeleportMeMessage.class, 10, Side.SERVER);
		netWrap.registerMessage(ChatHandler.class, ChatMessage.class, 11, Side.CLIENT);
		netWrap.registerMessage(OpenGuiHandler.class, OpenGuiMessage.class, 12, Side.CLIENT);
		netWrap.registerMessage(TeleportRequestHandler.class, TeleportRequestMessage.class, 13, Side.CLIENT);
		netWrap.registerMessage(TeleportRequestHandler.class, TeleportRequestMessage.class, 14, Side.SERVER);
	}
	
}
