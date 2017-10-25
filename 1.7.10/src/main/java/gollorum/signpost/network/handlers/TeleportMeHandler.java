package gollorum.signpost.network.handlers;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.TeleportMeMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class TeleportMeHandler implements IMessageHandler<TeleportMeMessage, IMessage> {

	@Override
	public IMessage onMessage(TeleportMeMessage message, MessageContext ctx) {
		if(ClientConfigStorage.INSTANCE.deactivateTeleportation()){
			return null;
		}
		if(PostHandler.canTeleport(ctx.getServerHandler().playerEntity, message.base)){
			World world = message.base.pos.getWorld();
			if(world == null){
				return new ChatMessage("signpost.errorWorld", "<world>", message.base.pos.world);
			}else{
				ServerConfigurationManager manager = FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager();
				EntityPlayerMP player = ctx.getServerHandler().playerEntity;
				if(!player.worldObj.equals(world)){
					manager.transferEntityToWorld(player, 1, (WorldServer)player.worldObj, (WorldServer)world);
				}
				if(!(player.dimension==message.base.pos.dim)){
		            WorldServer minecraftserver = MinecraftServer.getServer().worldServerForDimension(message.base.pos.dim);
					manager.transferEntityToWorld(player, 1, (WorldServer)player.worldObj, minecraftserver);
				}
				ctx.getServerHandler().playerEntity.setPositionAndUpdate(message.base.pos.x, message.base.pos.y, message.base.pos.z);
			}
		}else{
			return new ChatMessage("signpost.notDiscovered", "<Waystone>", message.base.name);
		}
		return null;
	}

}
