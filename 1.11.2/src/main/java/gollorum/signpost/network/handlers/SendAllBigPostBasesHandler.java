package gollorum.signpost.network.handlers;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.SendAllBigPostBasesMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SendAllBigPostBasesHandler implements IMessageHandler<SendAllBigPostBasesMessage, IMessage> {

	@Override
	public IMessage onMessage(SendAllBigPostBasesMessage message, MessageContext ctx) {
		PostHandler.bigPosts = message.toPostMap();
		PostHandler.refreshBigPostWaystones();
		return null;
	}
	
}