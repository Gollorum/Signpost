package gollorum.signpost.network.handlers;

import java.util.Map.Entry;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.DoubleBaseInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

public class SendPostBasesHandler implements IMessageHandler<SendPostBasesMessage, IMessage>{

	@Override
	public IMessage onMessage(SendPostBasesMessage message, MessageContext ctx) {
		for(Entry<BlockPos, DoubleBaseInfo> now: PostHandler.posts.entrySet()){
			if(now.getKey().equals(message.pos)){
				now.getValue().rotation1 = message.base1rot;
				now.getValue().rotation2 = message.base2rot;
				now.getValue().flip1 = message.flip1;
				now.getValue().flip2 = message.flip2;
				
				PostPostTile tile = PostPost.getWaystonePostTile(Signpost.proxy.getWorld(ctx), message.pos.x, message.pos.y, message.pos.z);
				now.getValue().base1 = PostHandler.getWSbyName(message.base1);
				now.getValue().base2 = PostHandler.getWSbyName(message.base2);
				if(tile.bases!=now.getValue()){
					tile.bases = now.getValue();
				}
				if(ctx.side.equals(Side.SERVER)){
					NetworkHandler.netWrap.sendToAll(message);
				}
				return null;
			}
		}
		return null;
	}

}
