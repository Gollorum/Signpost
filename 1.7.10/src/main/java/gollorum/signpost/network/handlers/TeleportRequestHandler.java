package gollorum.signpost.network.handlers;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.messages.TeleportRequestMessage;
import net.minecraft.util.ChatComponentText;

public class TeleportRequestHandler implements IMessageHandler<TeleportRequestMessage, IMessage> {

	@Override
	public IMessage onMessage(TeleportRequestMessage message, MessageContext ctx) {
		if(ctx.side.equals(Side.SERVER)){
			PostHandler.confirm(ctx.getServerHandler().playerEntity);
		}else{
			if(ConfigHandler.skipTeleportConfirm){
				return message;
			}else{
				String out;
				if(ConfigHandler.cost!=null){
					out = LanguageRegistry.instance().getStringLocalization("signpost.confirmTeleport");
					if(out.equals("")){
						out = LanguageRegistry.instance().getStringLocalization("signpost.confirmTeleport", "en_US");
					}
					out = out.replaceAll("<Waystone>", message.waystoneName);
					out = out.replaceAll("<amount>", Integer.toString(message.stackSize));
					out = out.replaceAll("<itemName>", ConfigHandler.costName());
				}else{
					out = LanguageRegistry.instance().getStringLocalization("signpost.confirmTeleportNoCost");
					if(out.equals("")){
						out = LanguageRegistry.instance().getStringLocalization("signpost.confirmTeleportNoCost", "en_US");
					}
					out = out.replaceAll("<Waystone>", message.waystoneName);
				}
				FMLClientHandler.instance().getClient().thePlayer.addChatMessage(new ChatComponentText(out));
			}
		}
		return null;
	}

	public String getReplacement(String replace){
		String ret = LanguageRegistry.instance().getStringLocalization(replace);
		if(!ret.equals("")){
			return ret;
		}
		if(!(ret = LanguageRegistry.instance().getStringLocalization(replace, "en_US")).equals("")){
			return ret;
		}
		return replace;
	}
}
