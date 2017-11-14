package gollorum.signpost.network.handlers;

import java.util.Map.Entry;

import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage.DoubleStringInt;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SendAllPostBasesHandler implements IMessageHandler<SendAllPostBasesMessage, IMessage> {

	@Override
	public IMessage onMessage(SendAllPostBasesMessage message, MessageContext ctx) {
		PostHandler.setPosts(message.toPostMap());
		for(Entry<MyBlockPos, DoubleStringInt> now: message.posts.entrySet()){
			TileEntity tileEntity = now.getKey().getTile();
			if(tileEntity instanceof PostPostTile){
				PostPostTile postTile = (PostPostTile) tileEntity;
				DoubleBaseInfo bases = postTile.getBases();
				switch(now.getValue().paintObjectIndex){
				case 1:
					bases.paintObject = bases;
					bases.awaitingPaint = true;
					break;
				case 2:
					bases.paintObject = bases.sign1;
					bases.awaitingPaint = true;
					break;
				case 3:
					bases.paintObject = bases.sign2;
					bases.awaitingPaint = true;
					break;
				default:
					bases.paintObject = null;
					bases.awaitingPaint = false;
					break;
				}
			}
		}
		return null;
	}
}
