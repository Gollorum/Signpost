package gollorum.signpost.network.handlers;

import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.TeleportRequestMessage;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class TeleportRequestHandler implements IMessageHandler<TeleportRequestMessage, IMessage> {

	@Override
	public IMessage onMessage(TeleportRequestMessage message, MessageContext ctx) {
		if(ctx.side.equals(Side.SERVER)){
			PostHandler.confirm(ctx.getServerHandler().player);
		}else{
			if(ClientConfigStorage.INSTANCE.skipTeleportConfirm()){
				return message;
			}else{
				String out;
				if(ClientConfigStorage.INSTANCE.getCost()!=null){
					out = I18n.format("signpost.confirmTeleport")
					 .replaceAll("<Waystone>", message.waystoneName)
					 .replaceAll("<amount>", Integer.toString(message.stackSize))
					 .replaceAll("<itemName>", ConfigHandler.costName());
				}else{
					out = I18n.format("signpost.confirmTeleportNoCost")
					 .replaceAll("<Waystone>", message.waystoneName);
				}
				FMLClientHandler.instance().getClient().player.sendMessage(new TextComponentString(out));
			}
		}
		return null;
	}

	public String getReplacement(String replace){
		String ret = I18n.format(replace);
		if(!ret.equals("")){
			return ret;
		}
		return replace;
	}
}
