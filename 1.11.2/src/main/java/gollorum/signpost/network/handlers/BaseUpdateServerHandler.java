package gollorum.signpost.network.handlers;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.BaseUpdateServerMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class BaseUpdateServerHandler implements IMessageHandler<BaseUpdateServerMessage, IMessage> {

	@Override
	public IMessage onMessage(BaseUpdateServerMessage message, MessageContext ctx) {
		if (message.destroyed) {
		} else {
			PostHandler.addDiscovered(ctx.getServerHandler().playerEntity.getUniqueID(), message.wayStone);
		}
		PostHandler.updateWS(message.wayStone, message.destroyed);
		if (PostHandler.updateWS(message.wayStone, message.destroyed)) {
			NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage());
		}
		return null;
	}

}
