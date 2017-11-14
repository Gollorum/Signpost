package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.SendDiscoveredToServerMessage;

public class SendDiscoveredToServerHandler implements IMessageHandler<SendDiscoveredToServerMessage, IMessage> {

	@Override
	public IMessage onMessage(SendDiscoveredToServerMessage message, MessageContext ctx) {
		PostHandler.addDiscovered(ctx.getServerHandler().playerEntity.getUniqueID(), PostHandler.getWSbyName(message.waystone));
		return null;
	}

}
