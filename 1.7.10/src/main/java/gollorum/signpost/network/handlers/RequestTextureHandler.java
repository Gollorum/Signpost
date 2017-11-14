package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.network.messages.RequestTextureMessage;
import gollorum.signpost.util.TextureHelper;

public class RequestTextureHandler implements IMessageHandler<RequestTextureMessage, IMessage>{

	@Override
	public IMessage onMessage(RequestTextureMessage message, MessageContext ctx) {
		TextureHelper.instance().setTexture(message.x, message.y, message.z);
		return null;
	}

}
