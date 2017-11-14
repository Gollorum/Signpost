package gollorum.signpost.network.handlers;

import gollorum.signpost.network.messages.ChatMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ChatHandler implements IMessageHandler<ChatMessage, IMessage> {

	@Override
	public IMessage onMessage(ChatMessage message, MessageContext ctx) {
		String out = I18n.translateToLocal(message.message);
		for(int i=0; i<message.keyword.length; i++){
			out = out.replaceAll(message.keyword[i], getReplacement(message.replacement[i]));
		}
		Minecraft.getMinecraft().thePlayer.addChatMessage(new TextComponentString(out));
		return null;
	}

	public String getReplacement(String replace){
		String ret = I18n.translateToLocal(replace);
		if(!ret.equals("")){
			return ret;
		}
		return replace;
	}

}
