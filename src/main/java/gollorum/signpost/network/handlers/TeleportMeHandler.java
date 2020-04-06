package gollorum.signpost.network.handlers;

import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.TeleportMeMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TeleportMeHandler implements IMessageHandler<TeleportMeMessage, IMessage> {

	@Override
	public IMessage onMessage(TeleportMeMessage message, MessageContext ctx) {
		if(ClientConfigStorage.INSTANCE.deactivateTeleportation()){
			return null;
		}
		if(PostHandler.canTeleport(ctx.getServerHandler().player, message.base)){
			World world = message.base.teleportPosition.getWorld();
			if(world == null){
				return new ChatMessage("signpost.errorWorld", "<world>", ""+message.base.teleportPosition.dim);
			}else{
				EntityPlayerMP player = ctx.getServerHandler().player;
				if(!player.world.equals(world)){
					player.setWorld(world);
				}
				if(!(player.dimension==message.base.teleportPosition.dim)){
					player.changeDimension(message.base.teleportPosition.dim);
				}
				ctx.getServerHandler().player.setPositionAndUpdate(message.base.teleportPosition.x, message.base.teleportPosition.y, message.base.teleportPosition.z);
			}
		}
		return null;
	}

}
