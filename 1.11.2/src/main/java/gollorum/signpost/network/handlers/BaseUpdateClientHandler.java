package gollorum.signpost.network.handlers;

import java.util.Map.Entry;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class BaseUpdateClientHandler implements IMessageHandler<BaseUpdateClientMessage, IMessage> {

	@Override
	public IMessage onMessage(BaseUpdateClientMessage message, MessageContext ctx) {
		PostHandler.allWaystones = message.waystones;
		for(Entry<MyBlockPos, DoubleBaseInfo> now: PostHandler.posts.entrySet()){
			BaseInfo base = now.getValue().sign1.base;
			if(base!=null){
				now.getValue().sign1.base = PostHandler.allWaystones.getByPos(base.pos);
			}
			base = now.getValue().sign2.base;
			if(base!=null){
				now.getValue().sign2.base = PostHandler.allWaystones.getByPos(base.pos);
			}
		}
		return null;
	}

}
