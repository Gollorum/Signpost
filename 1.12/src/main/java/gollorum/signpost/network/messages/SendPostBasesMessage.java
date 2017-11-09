package gollorum.signpost.network.messages;

import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class SendPostBasesMessage implements IMessage {

	public MyBlockPos pos;
	
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

	public String paint1;
	public String paint2;

	public String postPaint;
	
	public byte paintObjectIndex;

	public SendPostBasesMessage(){}
	
	public SendPostBasesMessage(PostPostTile tile, DoubleBaseInfo base) {
		tile.markDirty();
		this.pos = tile.toPos();
		this.base1 = ""+base.sign1.base;
		this.base2 = ""+base.sign2.base;
		base1rot = base.sign1.rotation;
		base2rot = base.sign2.rotation;
		flip1 = base.sign1.flip;
		flip2 = base.sign2.flip;
		overlay1 = ""+base.sign1.overlay;
		overlay2 = ""+base.sign2.overlay;
		point1 = base.sign1.point;
		point2 = base.sign2.point;
		paint1 = SuperPostPostTile.locToString(base.sign1.paint);
		paint2 = SuperPostPostTile.locToString(base.sign2.paint);
		postPaint = SuperPostPostTile.locToString(base.postPaint);
		
		if(base.equals(tile.getPaintObject())){
			paintObjectIndex = 1;
		}else if(base.sign1.equals(tile.getPaintObject())){
			paintObjectIndex = 2;
		}else if(base.sign2.equals(tile.getPaintObject())){
			paintObjectIndex = 3;
		}else{
			paintObjectIndex = 0;
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
		ByteBufUtils.writeUTF8String(buf, ""+paint1);
		ByteBufUtils.writeUTF8String(buf, ""+paint2);
		ByteBufUtils.writeUTF8String(buf, ""+postPaint);
		buf.writeByte(paintObjectIndex);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = MyBlockPos.fromBytes(buf);
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
		paint1 = ByteBufUtils.readUTF8String(buf);
		paint2 = ByteBufUtils.readUTF8String(buf);
		postPaint = ByteBufUtils.readUTF8String(buf);
		paintObjectIndex = buf.readByte();
	}

}
