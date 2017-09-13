package gollorum.signpost.network.handlers;

import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.InitPlayerResponseMessage;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class InitPlayerResponseHandler implements IMessageHandler<InitPlayerResponseMessage, IMessage>{

	@Override
	public IMessage onMessage(InitPlayerResponseMessage message, MessageContext ctx) {
		PostHandler.init();
		if(!message.deactivateTeleportation){
			PostHandler.allWaystones = message.allWaystones;
		}
		ConfigHandler.deactivateTeleportation = message.deactivateTeleportation;
		ConfigHandler.interdimensional = message.interdimensional;
		ConfigHandler.maxDist = message.maxDist;
		ConfigHandler.paymentItem = message.paymentItem;
		ConfigHandler.costMult = message.costMult;
		ConfigHandler.signRec = message.signRec;
		ConfigHandler.waysRec = message.waysRec;
		ConfigHandler.securityLevelWaystone = message.securityLevelWaystone;
		ConfigHandler.securityLevelSignpost = message.securityLevelSignpost;
		ConfigHandler.postInit();
		return null;
	}

}
