package gollorum.signpost.network.handlers;

import gollorum.signpost.management.ConfigHandler;
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
		if(ConfigHandler.deactivateTeleportation){
			return null;
		}
		if(PostHandler.canTeleport(ctx.getServerHandler().player, message.base)){
			World world = message.base.pos.getWorld();
			if(world == null){
				return new ChatMessage("signpost.errorWorld", "<world>", message.base.pos.world);
			}else{
				EntityPlayerMP player = ctx.getServerHandler().player;
				if(!player.world.equals(world)){
					player.setWorld(world);
				}
				if(!(player.dimension==message.base.pos.dim)){
					player.changeDimension(message.base.pos.dim);
				}
				ctx.getServerHandler().player.setPositionAndUpdate(message.base.pos.x, message.base.pos.y, message.base.pos.z);
			}
		}else{
			return new ChatMessage("signpost.notDiscovered", "<Waystone>", message.base.name);
		}
		return null;
	}

}
