package gollorum.signpost.network.messages;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class ChatMessage implements IMessage {

	public String message, keyword, replacement;

	public ChatMessage(){}
	
	public ChatMessage(String message, String keyword, String replacement){
		this.message = message;
		this.keyword = keyword;
		this.replacement = replacement;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		message = ByteBufUtils.readUTF8String(buf);
		keyword = ByteBufUtils.readUTF8String(buf);
		replacement = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, message);
		ByteBufUtils.writeUTF8String(buf, keyword);
		ByteBufUtils.writeUTF8String(buf, replacement);
	}

}
