package gollorum.signpost.network.messages;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import gollorum.signpost.blocks.BigPostPostTile;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.BlockPos;
import io.netty.buffer.ByteBuf;

public class SendBigPostBasesMessage implements IMessage {

	public BlockPos pos;
	public String base;
	public int baserot;
	public boolean flip;
	public String overlay;
	public boolean point;
	public String[] description;

	public SendBigPostBasesMessage(){}
	
	public SendBigPostBasesMessage(BigPostPostTile tile, BigBaseInfo base) {
		tile.markDirty();
		this.pos = tile.toPos();
		this.base = ""+base.base;
		baserot = base.rotation;
		flip = base.flip;
		overlay = ""+base.overlay;
		point = base.point;
		description = base.description;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		pos.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, base);
		buf.writeInt(baserot);
		buf.writeBoolean(flip);
		ByteBufUtils.writeUTF8String(buf, overlay);
		buf.writeBoolean(point);
		buf.writeInt(description.length);
		for(String now: description){
			ByteBufUtils.writeUTF8String(buf, now);
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromBytes(buf);
		base = ByteBufUtils.readUTF8String(buf);
		baserot = buf.readInt();
		flip = buf.readBoolean();
		overlay = ByteBufUtils.readUTF8String(buf);
		point = buf.readBoolean();
		description = new String[buf.readInt()];
		for(int i=0; i<description.length; i++){
			description[i] = ByteBufUtils.readUTF8String(buf);
		}
	}

}
