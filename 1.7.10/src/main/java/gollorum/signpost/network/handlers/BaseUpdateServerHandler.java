package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.BaseUpdateServerMessage;

public class BaseUpdateServerHandler implements IMessageHandler<BaseUpdateServerMessage, IMessage> {

	@Override
	public IMessage onMessage(BaseUpdateServerMessage message, MessageContext ctx) {
		if (message.destroyed) {
		} else {
			PostHandler.addDiscovered(ctx.getServerHandler().playerEntity.getUniqueID(), message.wayStone);
		}
		if (PostHandler.updateWS(message.wayStone, message.destroyed)) {
			NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage());
		}
		return null;
	}

}
