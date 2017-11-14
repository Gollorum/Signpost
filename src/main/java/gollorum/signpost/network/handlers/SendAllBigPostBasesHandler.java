package gollorum.signpost.network.handlers;

import java.util.Map.Entry;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.SendAllBigPostBasesMessage;
import gollorum.signpost.network.messages.SendAllBigPostBasesMessage.BigStringInt;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.tileentity.TileEntity;

public class SendAllBigPostBasesHandler implements IMessageHandler<SendAllBigPostBasesMessage, IMessage> {

	@Override
	public IMessage onMessage(SendAllBigPostBasesMessage message, MessageContext ctx) {
		PostHandler.setBigPosts(message.toPostMap());
		for(Entry<MyBlockPos, BigStringInt> now: message.bigPosts.entrySet()){
			TileEntity tileEntity = now.getKey().getTile();
			if(tileEntity instanceof BigPostPostTile){
				BigPostPostTile postTile = (BigPostPostTile) tileEntity;
				BigBaseInfo bases = postTile.getBases();
				switch(now.getValue().paintObjectIndex){
				case 1:
					bases.paintObject = bases;
					bases.awaitingPaint = true;
					break;
				case 2:
					bases.paintObject = bases.sign;
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