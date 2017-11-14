package gollorum.signpost.network.messages;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class SendDiscoveredToServerMessage implements IMessage {

	public String waystone;
	
	public SendDiscoveredToServerMessage(){}
	
	public SendDiscoveredToServerMessage(String waystone){
		this.waystone = waystone;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		waystone = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, waystone);
	}

}
