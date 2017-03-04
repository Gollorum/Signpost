package gollorum.signpost.network.messages;

import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
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
	
	public SendPostBasesMessage(){}
	
	public SendPostBasesMessage(MyBlockPos pos, DoubleBaseInfo base) {
		this.pos = pos;
		this.base1 = ""+base.base1;
		this.base2 = ""+base.base2;
		base1rot = base.rotation1;
		base2rot = base.rotation2;
		flip1 = base.flip1;
		flip2 = base.flip2;
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
	}

}
