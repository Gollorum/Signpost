package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.event.UpdateWaystoneEvent;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.BaseUpdateServerMessage;
import gollorum.signpost.util.BaseInfo;
import net.minecraftforge.common.MinecraftForge;

public class BaseUpdateServerHandler implements IMessageHandler<BaseUpdateServerMessage, IMessage> {

	@Override
	public IMessage onMessage(BaseUpdateServerMessage message, MessageContext ctx) {
		if (message.destroyed) {
		} else {
			PostHandler.addDiscovered(ctx.getServerHandler().playerEntity.getUniqueID(), message.wayStone);
		}
		BaseInfo waystone = PostHandler.allWaystones.getByPos(message.wayStone.pos);
		waystone.setAll(message.wayStone);
		NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage());
		MinecraftForge.EVENT_BUS.post(new UpdateWaystoneEvent(UpdateWaystoneEvent.WaystoneEventType.NAMECHANGED, ctx.getServerHandler().playerEntity.worldObj, waystone.pos.x, waystone.pos.y, waystone.pos.z, waystone.name));
		return null;
	}

}
