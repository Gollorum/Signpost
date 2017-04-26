package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendBigPostBasesMessage;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.BigBaseInfo.OverlayType;

public class SendBigPostBasesHandler implements IMessageHandler<SendBigPostBasesMessage, IMessage>{

	@Override
	public IMessage onMessage(SendBigPostBasesMessage message, MessageContext ctx) {
		BigBaseInfo bases = PostHandler.bigPosts.get(message.pos);
		bases.rotation = message.baserot;
		bases.flip = message.flip;
		bases.base = PostHandler.getWSbyName(message.base);
		bases.overlay = OverlayType.get(message.overlay);
		bases.point = message.point;
		bases.description = message.description;
		bases.signPaint = message.paint;
		if(ctx.side.equals(Side.SERVER)){
			ctx.getServerHandler().playerEntity.worldObj.getTileEntity(message.pos.x, message.pos.y, message.pos.z).markDirty();
			NetworkHandler.netWrap.sendToAll(message);
		}
		return null;
	}

}
