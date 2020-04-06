package gollorum.signpost.network.handlers;

import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.InitPlayerResponseMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class InitPlayerResponseHandler implements IMessageHandler<InitPlayerResponseMessage, IMessage>{

	@Override
	public IMessage onMessage(InitPlayerResponseMessage message, MessageContext ctx) {
		if(!message.deactivateTeleportation){
			PostHandler.setNativeWaystones(message.allWaystones);
		}
		ClientConfigStorage.INSTANCE.setDeactivateTeleportation(message.deactivateTeleportation);
		ClientConfigStorage.INSTANCE.setInterdimensional(message.interdimensional);
		ClientConfigStorage.INSTANCE.setMaxDist(message.maxDist);
		ClientConfigStorage.INSTANCE.setPaymentItem(message.paymentItem);
		ClientConfigStorage.INSTANCE.setCostMult(message.costMult);
		ClientConfigStorage.INSTANCE.setCostBase(message.costBase);
		ClientConfigStorage.INSTANCE.setSignRec(message.signRec);
		ClientConfigStorage.INSTANCE.setWaysRec(message.waysRec);
		ClientConfigStorage.INSTANCE.setSecurityLevelWaystone(message.securityLevelWaystone);
		ClientConfigStorage.INSTANCE.setSecurityLevelSignpost(message.securityLevelSignpost);
		ClientConfigStorage.INSTANCE.setDisableVillageGeneration(message.disableVillageGeneration); 
	    ClientConfigStorage.INSTANCE.setVillageMaxSignposts(message.villageMaxSignposts); 
	    ClientConfigStorage.INSTANCE.setVillageSignpostsWeight(message.villageSignpostsWeight); 
	    ClientConfigStorage.INSTANCE.setVillageWaystonesWeight(message.villageWaystonesWeight);
	    ClientConfigStorage.INSTANCE.setOnlyVillageTargets(message.onlyVillageTargets);
		ClientConfigStorage.INSTANCE.setAllowedCraftingModels(message.allowedCraftingModels);
		ClientConfigStorage.INSTANCE.setAllowedVillageModels(message.allowedVillageModels);
		ClientConfigStorage.INSTANCE.postInit();
		return null;
	}

}
