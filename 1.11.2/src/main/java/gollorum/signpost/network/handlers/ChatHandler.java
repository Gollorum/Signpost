package gollorum.signpost.network.handlers;

import gollorum.signpost.network.messages.ChatMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.util.text.translation.LanguageMap;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ChatHandler implements IMessageHandler<ChatMessage, IMessage> {

	@Override
	public IMessage onMessage(ChatMessage message, MessageContext ctx) {
		String out = I18n.translateToLocal(message.message);
		out = out.replaceAll(message.keyword, message.replacement);
		Minecraft.getMinecraft().player.sendMessage(new TextComponentString(out));
		return null;
	}

}
