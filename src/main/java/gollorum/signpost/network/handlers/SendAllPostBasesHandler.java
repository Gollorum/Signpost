package gollorum.signpost.network.handlers;

import java.util.Map.Entry;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage.DoubleStringInt;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.DoubleBaseInfo;

public class SendAllPostBasesHandler implements IMessageHandler<SendAllPostBasesMessage, IMessage> {

	@Override
	public IMessage onMessage(SendAllPostBasesMessage message, MessageContext ctx) {
		for(Entry<BlockPos, DoubleStringInt> now : message.posts.entrySet()){
			boolean found = false;
			for(Entry<BlockPos, DoubleBaseInfo> nowPost: PostHandler.posts.entrySet()){
				if(nowPost.getKey().equals(now.getKey())){
					found = true;
					if(nowPost.getValue().base1==null||PostHandler.getWSbyName(now.getValue().string1)==null){
						nowPost.getValue().base1 = PostHandler.getWSbyName(now.getValue().string1);
					}else{
						nowPost.getValue().base1.update(PostHandler.getWSbyName(now.getValue().string1));
					}

					if(nowPost.getValue().base2==null||PostHandler.getWSbyName(now.getValue().string2)==null){
						nowPost.getValue().base2 = PostHandler.getWSbyName(now.getValue().string2);
					}else{
						nowPost.getValue().base2.update(PostHandler.getWSbyName(now.getValue().string2));
					}

					nowPost.getValue().rotation1 = now.getValue().int1;
					nowPost.getValue().rotation2 = now.getValue().int2;

					nowPost.getValue().flip1 = now.getValue().bool1;
					nowPost.getValue().flip2 = now.getValue().bool2;
					
					break;
				}
			}
			if(!found){
				PostHandler.posts.put(now.getKey(), new DoubleBaseInfo(PostHandler.getWSbyName(now.getValue().string1), PostHandler.getWSbyName(now.getValue().string2), now.getValue().int1, now.getValue().int2, now.getValue().bool1, now.getValue().bool2));
			}
		}
		return null;
	}
	
}
		