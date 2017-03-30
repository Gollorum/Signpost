package gollorum.signpost.network.handlers;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import gollorum.signpost.blocks.CustomPostPostTile;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.DoubleBaseInfo.OverlayType;

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
		bases.overlay1 = OverlayType.get(message.overlay1);
		bases.overlay2 = OverlayType.get(message.overlay2);
		bases.point1 = message.point1;
		bases.point1 = message.point2;
		if(ctx.side.equals(Side.SERVER)){
			PostPostTile tile = (PostPostTile) ctx.getServerHandler().playerEntity.worldObj.getTileEntity(message.pos.x, message.pos.y, message.pos.z);
			if(tile instanceof CustomPostPostTile){
				CustomPostPostTile t = (CustomPostPostTile)tile;
				t.uMin1 = message.uMin1;
				t.uMax1 = message.uMax1;
				t.vMin1 = message.vMin1;
				t.vMax1 = message.vMax1;

				t.uMin2 = message.uMin2;
				t.uMax2 = message.uMax2;
				t.vMin2 = message.vMin2;
				t.vMax2 = message.vMax2;
			}
			tile.markDirty();
			NetworkHandler.netWrap.sendToAll(message);
		}else{
			PostPostTile tile = (PostPostTile) FMLClientHandler.instance().getWorldClient().getTileEntity(message.pos.x, message.pos.y, message.pos.z);
			if(tile instanceof CustomPostPostTile){
				CustomPostPostTile t = (CustomPostPostTile)tile;
				t.uMin1 = message.uMin1;
				t.uMax1 = message.uMax1;
				t.vMin1 = message.vMin1;
				t.vMax1 = message.vMax1;

				t.uMin2 = message.uMin2;
				t.uMax2 = message.uMax2;
				t.vMin2 = message.vMin2;
				t.vMax2 = message.vMax2;
			}
		}
		return null;
	}

}
