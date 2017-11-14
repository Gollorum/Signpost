package gollorum.signpost.network.messages;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

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
