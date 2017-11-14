package gollorum.signpost.network.messages;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class TeleportRequestMessage implements IMessage {

	public int stackSize;
	public String waystoneName;
	
	public TeleportRequestMessage(){}
	
	public TeleportRequestMessage(int stackSize, String waystoneName) {
		this.stackSize = stackSize;
		this.waystoneName = waystoneName;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(stackSize);
		ByteBufUtils.writeUTF8String(buf, waystoneName);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		stackSize = buf.readInt();
		waystoneName = ByteBufUtils.readUTF8String(buf);
	}

}
