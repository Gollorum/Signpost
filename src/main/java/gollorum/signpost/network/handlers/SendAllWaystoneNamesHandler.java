package gollorum.signpost.network.handlers;

import java.util.Collection;
import java.util.HashSet;

import gollorum.signpost.network.messages.SendAllWaystoneNamesMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SendAllWaystoneNamesHandler implements IMessageHandler<SendAllWaystoneNamesMessage, IMessage>{
	
	public static Collection<String> cachedWaystoneNames = new HashSet<String>();

	@Override
	public IMessage onMessage(SendAllWaystoneNamesMessage message, MessageContext ctx) {
		cachedWaystoneNames = message.waystones;
		return null;
	}

}
