package gollorum.signpost.network.handlers;

import java.util.Map.Entry;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage.DoubleStringInt;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SendAllPostBasesHandler implements IMessageHandler<SendAllPostBasesMessage, IMessage> {

	@Override
	public IMessage onMessage(SendAllPostBasesMessage message, MessageContext ctx) {
		PostHandler.posts = message.toPostMap();
		return null;
	}
	
}
		