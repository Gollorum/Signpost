package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.ConfigHandler.SecurityLevel;
import gollorum.signpost.network.messages.InitPlayerResponseMessage;
import net.minecraft.item.ItemStack;

public class InitPlayerResponseHandler implements IMessageHandler<InitPlayerResponseMessage, IMessage>{

	@Override
	public IMessage onMessage(InitPlayerResponseMessage message, MessageContext ctx) {
		if(!message.deactivateTeleportation){
			PostHandler.allWaystones.addAll(message.allWaystones);
		}
		ConfigHandler.deactivateTeleportation = message.deactivateTeleportation;
		ConfigHandler.interdimensional = message.interdimensional;
		ConfigHandler.maxDist = message.maxDist;
		ConfigHandler.paymentItem = message.paymentItem;
		ConfigHandler.costMult = message.costMult;
		ConfigHandler.securityLevelWaystone = message.securityLevelWaystone;
		ConfigHandler.securityLevelSignpost = message.securityLevelSignpost;
		ConfigHandler.postInit();
		return null;
	}

}
