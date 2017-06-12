package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendBigPostBasesMessage;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.Sign.OverlayType;

public class SendBigPostBasesHandler implements IMessageHandler<SendBigPostBasesMessage, IMessage>{

	@Override
	public IMessage onMessage(SendBigPostBasesMessage message, MessageContext ctx) {
		BigBaseInfo bases = PostHandler.bigPosts.get(message.pos);
		if(bases==null){
			return null;
		}
		bases.sign.rotation = message.baserot;
		bases.sign.flip = message.flip;
		bases.sign.base = PostHandler.getForceWSbyName(message.base);
		bases.sign.overlay = OverlayType.get(message.overlay);
		bases.sign.point = message.point;
		bases.description = message.description;
		bases.sign.paint = message.paint;
		if(ctx.side.equals(Side.SERVER)){
			ctx.getServerHandler().playerEntity.worldObj.getTileEntity(message.pos.x, message.pos.y, message.pos.z).markDirty();
			NetworkHandler.netWrap.sendToAll(message);
		}
		return null;
	}

}
