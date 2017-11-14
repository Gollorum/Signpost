package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.Signpost;
import gollorum.signpost.network.messages.OpenGuiMessage;
import net.minecraft.client.Minecraft;

public class OpenGuiHandler implements IMessageHandler<OpenGuiMessage, IMessage> {

	@Override
	public IMessage onMessage(OpenGuiMessage message, MessageContext ctx) {
		Minecraft.getMinecraft().thePlayer.openGui(Signpost.instance, message.guiID, Signpost.proxy.getWorld(ctx), message.x, message.y, message.z);
		return null;
	}

}
