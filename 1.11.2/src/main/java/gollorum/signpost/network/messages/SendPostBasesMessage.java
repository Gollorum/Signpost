package gollorum.signpost.network.messages;

import gollorum.signpost.blocks.PostPostTile;
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

	public SendPostBasesMessage(){}
	
	public SendPostBasesMessage(PostPostTile tile, DoubleBaseInfo base) {
		tile.markDirty();
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
	}

}
