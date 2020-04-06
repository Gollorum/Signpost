package gollorum.signpost.commands;

import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.ChatMessage;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class GetWaystoneCount extends CommandBase {

	@Override
	public String getName() {
		return "getwaystonesleft";
	}

	@Override
	public String getUsage(ICommandSender p_71518_1_) {
		return "/getwaystonesleft";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
		if(sender instanceof EntityPlayerMP){
			EntityPlayerMP player = (EntityPlayerMP) sender;
			if(args.length==0 || !ConfigHandler.isOp(player)){
				String amount = ""+PostHandler.getPlayerKnownWaystonePositions(player.getUniqueID()).remainingWaystones;
				if(amount.equals("-1")){
					amount = "unlimited";
				}
				NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.getwaystonesleft", "<amount>", amount), player);
			}else{
				EntityPlayerMP p = (EntityPlayerMP) PostHandler.getPlayerByName(args[0]);
				if(p==null){
					NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.playerNotFound", "<player>", args[0]), player);
					return;
				}
				String amount = ""+PostHandler.getPlayerKnownWaystonePositions(p.getUniqueID()).remainingWaystones;
				if(amount.equals("-1")){
					amount = "unlimited";
				}
				String[] keys = {"<amount>", "<player>"};
				String[] replacement = {amount, args[0]};
				NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.getwaystonesleftop", keys, replacement), player);
			}
		}
	}

	@Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender){
    	return sender instanceof EntityPlayer;
    }

}