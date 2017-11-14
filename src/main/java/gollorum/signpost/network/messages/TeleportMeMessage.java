package gollorum.signpost.network.messages;

import gollorum.signpost.util.BaseInfo;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class TeleportMeMessage implements IMessage{

	public BaseInfo base;
	
	public TeleportMeMessage(){}
	
	public TeleportMeMessage(BaseInfo base) {
		this.base = base;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		base = BaseInfo.fromBytes(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		base.toBytes(buf);
	}
	
}
