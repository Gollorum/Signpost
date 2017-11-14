package gollorum.signpost.network.handlers;

import gollorum.signpost.Signpost;
import gollorum.signpost.network.messages.OpenGuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class OpenGuiHandler implements IMessageHandler<OpenGuiMessage, IMessage> {

	@Override
	public IMessage onMessage(OpenGuiMessage message, MessageContext ctx) {
		Minecraft.getMinecraft().player.openGui(Signpost.instance, message.guiID, Signpost.proxy.getWorld(ctx), message.x, message.y, message.z);
		return null;
	}

}