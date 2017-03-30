package gollorum.signpost.network.messages;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import gollorum.signpost.blocks.CustomPostPostTile;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.DoubleBaseInfo;
import io.netty.buffer.ByteBuf;

public class SendPostBasesMessage implements IMessage {

	public BlockPos pos;
	
	public String base1;
	public String base2;
	
	public int base1rot;
	public int base2rot;

	public boolean flip1;
	public boolean flip2;

	public String overlay1;
	public String overlay2;

	public boolean point1;
	public boolean point2;
	
	public boolean isCustom;

	public float uMin1;
	public float uMax1;
	public float vMin1;
	public float vMax1;

	public float uMin2;
	public float uMax2;
	public float vMin2;
	public float vMax2;
	
	public SendPostBasesMessage(){}
	
	public SendPostBasesMessage(PostPostTile tile, DoubleBaseInfo base) {
		this.pos = tile.toPos();
		this.base1 = ""+base.base1;
		this.base2 = ""+base.base2;
		base1rot = base.rotation1;
		base2rot = base.rotation2;
		flip1 = base.flip1;
		flip2 = base.flip2;
		overlay1 = ""+base.overlay1;
		overlay2 = ""+base.overlay2;
		point1 = base.point1;
		point2 = base.point2;
		
		if(isCustom = tile instanceof CustomPostPostTile){
			CustomPostPostTile t = (CustomPostPostTile)tile;
			uMin1 = t.uMin1;
			uMax1 = t.uMax1;
			vMin1 = t.vMin1;
			vMax1 = t.vMax1;

			uMin2 = t.uMin2;
			uMax2 = t.uMax2;
			vMin2 = t.vMin2;
			vMax2 = t.vMax2;
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		pos.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, base1);
		ByteBufUtils.writeUTF8String(buf, base2);
		buf.writeInt(base1rot);
		buf.writeInt(base2rot);
		buf.writeBoolean(flip1);
		buf.writeBoolean(flip2);
		ByteBufUtils.writeUTF8String(buf, overlay1);
		ByteBufUtils.writeUTF8String(buf, overlay2);
		buf.writeBoolean(point1);
		buf.writeBoolean(point2);
		buf.writeBoolean(isCustom);
		if(isCustom){
			buf.writeFloat(uMin1);
			buf.writeFloat(uMax1);
			buf.writeFloat(vMin1);
			buf.writeFloat(vMax1);
			
			buf.writeFloat(uMin2);
			buf.writeFloat(uMax2);
			buf.writeFloat(vMin2);
			buf.writeFloat(vMax2);
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromBytes(buf);
		base1 = ByteBufUtils.readUTF8String(buf);
		base2 = ByteBufUtils.readUTF8String(buf);
		base1rot = buf.readInt();
		base2rot = buf.readInt();
		flip1 = buf.readBoolean();
		flip2 = buf.readBoolean();
		overlay1 = ByteBufUtils.readUTF8String(buf);
		overlay2 = ByteBufUtils.readUTF8String(buf);
		point1 = buf.readBoolean();
		point2 = buf.readBoolean();
		if(isCustom = buf.readBoolean()){
			uMin1 = buf.readFloat();
			uMax1 = buf.readFloat();
			vMin1 = buf.readFloat();
			vMax1 = buf.readFloat();

			uMin2 = buf.readFloat();
			uMax2 = buf.readFloat();
			vMin2 = buf.readFloat();
			vMax2 = buf.readFloat();
		}
	}

}
