package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.util.BaseInfo;

public class BaseUpdateClientHandler implements IMessageHandler<BaseUpdateClientMessage, IMessage> {

	@Override
	public IMessage onMessage(BaseUpdateClientMessage message, MessageContext ctx) {
		for (BaseInfo now : message.waystones) {
			boolean hasChanged = false;
			for (BaseInfo now2 : PostHandler.allWaystones) {
				if (now2.update(now)) {
					hasChanged = true;
					break;
				}
			}
			if (!hasChanged) {
				PostHandler.allWaystones.add(now);
			}
		}
		return null;
	}

}
