package gollorum.signpost.network.messages;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

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
