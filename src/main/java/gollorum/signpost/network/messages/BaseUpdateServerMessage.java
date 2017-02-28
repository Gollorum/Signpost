package gollorum.signpost.network.messages;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import gollorum.signpost.util.BaseInfo;
import io.netty.buffer.ByteBuf;

public class BaseUpdateServerMessage implements IMessage{

	public BaseInfo wayStone;
	public boolean destroyed;

	public BaseUpdateServerMessage(){}
	
	public BaseUpdateServerMessage(BaseInfo wayStone, boolean destroyed){
		this.wayStone = wayStone;
		this.destroyed = destroyed;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		wayStone = BaseInfo.fromBytes(buf);
		destroyed = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		wayStone.toBytes(buf);
		buf.writeBoolean(destroyed);
	}

}
