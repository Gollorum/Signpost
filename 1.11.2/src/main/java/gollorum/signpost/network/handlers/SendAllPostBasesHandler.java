package gollorum.signpost.network.handlers;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SendAllPostBasesHandler implements IMessageHandler<SendAllPostBasesMessage, IMessage> {

	@Override
	public IMessage onMessage(SendAllPostBasesMessage message, MessageContext ctx) {
		PostHandler.posts = message.toPostMap();
		PostHandler.refreshDoublePostWaystones();
		return null;
	}
	
}
		