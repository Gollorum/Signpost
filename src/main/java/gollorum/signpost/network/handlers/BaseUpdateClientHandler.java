package gollorum.signpost.network.handlers;

import java.util.Map.Entry;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.tileentity.TileEntity;

public class BaseUpdateClientHandler implements IMessageHandler<BaseUpdateClientMessage, IMessage> {

	@Override
	public IMessage onMessage(BaseUpdateClientMessage message, MessageContext ctx) {
		for(Entry<MyBlockPos, DoubleBaseInfo> now: PostHandler.getPosts().entrySet()){
			TileEntity tile = FMLClientHandler.instance().getWorldClient().getTileEntity(now.getKey().x, now.getKey().y, now.getKey().z);
			if(tile instanceof SuperPostPostTile){
				((SuperPostPostTile)tile).isWaystone=false;
			}
		}
		for(Entry<MyBlockPos, BigBaseInfo> now: PostHandler.getBigPosts().entrySet()){
			TileEntity tile = FMLClientHandler.instance().getWorldClient().getTileEntity(now.getKey().x, now.getKey().y, now.getKey().z);
			if(tile instanceof SuperPostPostTile){
				((SuperPostPostTile)tile).isWaystone=false;
			}
		}
		PostHandler.setNativeWaystones(message.waystones);
		for(BaseInfo now: PostHandler.getNativeWaystones()){
			TileEntity tile = FMLClientHandler.instance().getWorldClient().getTileEntity(now.blockPosition.x, now.blockPosition.y, now.blockPosition.z);
			if(tile instanceof SuperPostPostTile){
				((SuperPostPostTile)tile).isWaystone=true;
			}
		}
		for(Entry<MyBlockPos, DoubleBaseInfo> now: PostHandler.getPosts().entrySet()){
			BaseInfo base = now.getValue().sign1.base;
			if(base!=null &&!(base.teleportPosition ==null && base.owner==null)){
				now.getValue().sign1.base = PostHandler.getAllWaystones().getByPos(base.blockPosition);
			}
			base = now.getValue().sign2.base;
			if(base!=null &&!(base.teleportPosition ==null && base.owner==null)){
				now.getValue().sign2.base = PostHandler.getAllWaystones().getByPos(base.blockPosition);
			}
		}
		for(Entry<MyBlockPos, BigBaseInfo> now: PostHandler.getBigPosts().entrySet()){
			BaseInfo base = now.getValue().sign.base;
			if(base!=null &&!(base.teleportPosition ==null && base.owner==null)){
				now.getValue().sign.base = PostHandler.getAllWaystones().getByPos(base.blockPosition);
			}
			TileEntity tile = now.getKey().getTile();
		}
		return null;
	}

}