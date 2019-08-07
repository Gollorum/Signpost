package gollorum.signpost.network.handlers;

import java.util.Collection;
import java.util.HashSet;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.network.messages.SendAllWaystoneNamesMessage;

public class SendAllWaystoneNamesHandler implements IMessageHandler<SendAllWaystoneNamesMessage, IMessage>{
	
	public static Collection<String> cachedWaystoneNames = new HashSet<String>();

	@Override
	public IMessage onMessage(SendAllWaystoneNamesMessage message, MessageContext ctx) {
		cachedWaystoneNames = message.waystones;
		return null;
	}

}