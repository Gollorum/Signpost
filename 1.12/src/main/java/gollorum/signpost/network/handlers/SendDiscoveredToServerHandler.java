package gollorum.signpost.network.handlers;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.SendDiscoveredToServerMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SendDiscoveredToServerHandler implements IMessageHandler<SendDiscoveredToServerMessage, IMessage> {

	@Override
	public IMessage onMessage(SendDiscoveredToServerMessage message, MessageContext ctx) {
		PostHandler.addDiscovered(ctx.getServerHandler().player.getUniqueID(), PostHandler.getWSbyName(message.waystone));
		return null;
	}

}
