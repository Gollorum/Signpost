package gollorum.signpost.commands;

import gollorum.signpost.management.PostHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class ConfirmTeleportCommand extends CommandBase {

	@Override
	public String getName() {
		return "signpostconfirm";
	}

	@Override
	public String getUsage(ICommandSender p_71518_1_) {
		return "/signpostconfirm";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
		if(sender instanceof EntityPlayerMP){
			PostHandler.confirm((EntityPlayerMP) sender);
		}
	}

	@Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender){
    	return sender instanceof EntityPlayer;
    }

}