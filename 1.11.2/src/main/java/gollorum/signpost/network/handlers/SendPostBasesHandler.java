package gollorum.signpost.network.handlers;

import java.util.Map.Entry;

import gollorum.signpost.Signpost;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SendPostBasesHandler implements IMessageHandler<SendPostBasesMessage, IMessage>{

	@Override
	public IMessage onMessage(SendPostBasesMessage message, MessageContext ctx) {
		for(Entry<MyBlockPos, DoubleBaseInfo> now: PostHandler.posts.entrySet()){
			if(now.getKey().equals(message.pos)){
				now.getValue().rotation1 = message.base1rot;
				now.getValue().rotation2 = message.base2rot;
				now.getValue().flip1 = message.flip1;
				now.getValue().flip2 = message.flip2;
				if(message.base1.equals("null")){
					now.getValue().base1 = null;
				}else{
					now.getValue().base1 = PostHandler.getWSbyName(message.base1);
				}
				if(message.base2.equals("null")){
					now.getValue().base2 = null;
				}else{
					now.getValue().base2 = PostHandler.getWSbyName(message.base2);
				}
				if(Signpost.serverSide){
					NetworkHandler.netWrap.sendToAll(message);
				}
				return null;
			}
		}
		return null;
	}

}
