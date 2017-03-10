package gollorum.signpost.network.handlers;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.DoubleBaseInfo;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class SendPostBasesHandler implements IMessageHandler<SendPostBasesMessage, IMessage>{

	@Override
	public IMessage onMessage(SendPostBasesMessage message, MessageContext ctx) {
		DoubleBaseInfo bases = PostHandler.posts.get(message.pos);
		bases.rotation1 = message.base1rot;
		bases.rotation2 = message.base2rot;
		bases.flip1 = message.flip1;
		bases.flip2 = message.flip2;
		bases.base1 = PostHandler.getWSbyName(message.base1);
		bases.base2 = PostHandler.getWSbyName(message.base2);
		if(ctx.side.equals(Side.SERVER)){
			NetworkHandler.netWrap.sendToAll(message);
		}
		return null;
	}

}
