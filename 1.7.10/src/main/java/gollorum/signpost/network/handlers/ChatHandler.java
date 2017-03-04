package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.LanguageRegistry;
import gollorum.signpost.network.messages.ChatMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public class ChatHandler implements IMessageHandler<ChatMessage, IMessage> {

	@Override
	public IMessage onMessage(ChatMessage message, MessageContext ctx) {
		String out = LanguageRegistry.instance().getStringLocalization(message.message);
		if(out.equals("")){
			out = LanguageRegistry.instance().getStringLocalization(message.message, "en_US");
		}
		System.out.println(out);
		out = out.replaceAll(message.keyword, message.replacement);
		Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(out));
		return null;
	}

}
