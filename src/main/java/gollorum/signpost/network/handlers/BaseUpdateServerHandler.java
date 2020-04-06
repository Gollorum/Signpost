package gollorum.signpost.network.handlers;

import gollorum.signpost.event.UpdateWaystoneEvent;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.BaseUpdateServerMessage;
import gollorum.signpost.util.BaseInfo;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class BaseUpdateServerHandler implements IMessageHandler<BaseUpdateServerMessage, IMessage> {

	@Override
	public IMessage onMessage(BaseUpdateServerMessage message, MessageContext ctx) {
		if (message.destroyed) {
		} else {
			PostHandler.addDiscovered(ctx.getServerHandler().player.getUniqueID(), message.wayStone);
		}
		BaseInfo waystone = PostHandler.getAllWaystones().getByPos(message.wayStone.blockPosition);
		waystone.setAll(message.wayStone);
		NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage());
		MinecraftForge.EVENT_BUS.post(new UpdateWaystoneEvent(UpdateWaystoneEvent.WaystoneEventType.NAMECHANGED, ctx.getServerHandler().player.world, waystone.teleportPosition.x, waystone.teleportPosition.y, waystone.teleportPosition.z, waystone.getName()));
		return null;
	}

}
