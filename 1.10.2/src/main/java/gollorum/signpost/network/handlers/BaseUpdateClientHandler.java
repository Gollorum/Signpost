package gollorum.signpost.network.handlers;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.StonedHashSet;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class BaseUpdateClientHandler implements IMessageHandler<BaseUpdateClientMessage, IMessage> {

	@Override
	public IMessage onMessage(BaseUpdateClientMessage message, MessageContext ctx) {
		StonedHashSet toDelete = new StonedHashSet();
		toDelete.addAll(PostHandler.allWaystones);
		for (BaseInfo now : message.waystones) {
			boolean hasChanged = false;
			for (BaseInfo now2 : PostHandler.allWaystones) {
				if (now2.update(now)) {
					hasChanged = true;
					toDelete.remove(now2);
					break;
				}
			}
			if (!hasChanged) {
				PostHandler.allWaystones.add(now);
			}
		}
		PostHandler.allWaystones.removeAll(toDelete);
		return null;
	}

}
