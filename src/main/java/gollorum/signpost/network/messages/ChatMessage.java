package gollorum.signpost.network.messages;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class ChatMessage implements IMessage {

	public String message;
	public String[] keyword, replacement;

	public ChatMessage(){}
	
	public ChatMessage(String message, String keyword, String replacement){
		this.message = message;
		this.keyword = new String[1];
		this.keyword[0] = keyword;
		this.replacement = new String[1];
		this.replacement[0] = replacement;
	}
	
	public ChatMessage(String message, String[] keyword, String[] replacement){
		this.message = message;
		this.keyword = keyword;
		this.replacement = replacement;
	}
	
	public ChatMessage(String message) {
		this(message, new String[0], new String[0]);
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, message);
		buf.writeInt(keyword.length);
		for(int i=0; i<keyword.length; i++){
			ByteBufUtils.writeUTF8String(buf, getKeyword(i));
			ByteBufUtils.writeUTF8String(buf, getReplacement(i));
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		message = ByteBufUtils.readUTF8String(buf);
		keyword = new String[buf.readInt()];
		replacement = new String[keyword.length];
		for(int i=0; i<keyword.length; i++){
			keyword[i] = ByteBufUtils.readUTF8String(buf);
			replacement[i] = ByteBufUtils.readUTF8String(buf);
		}
	}
	
	private String getKeyword(int i){
		String ret = keyword[i];
		return ""+ret;
	}
	
	private String getReplacement(int i){
		String ret = replacement[i];
		return ""+ret;
	}
}
