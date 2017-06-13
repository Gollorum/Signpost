package gollorum.signpost.network.handlers;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendBigPostBasesMessage;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.Sign.OverlayType;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

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
		bases.postPaint = message.postPaint;
		if(ctx.side.equals(Side.SERVER)){
			ctx.getServerHandler().playerEntity.world.getTileEntity(message.pos.toBlockPos()).markDirty();
			NetworkHandler.netWrap.sendToAll(message);
		}
		return null;
	}

}
