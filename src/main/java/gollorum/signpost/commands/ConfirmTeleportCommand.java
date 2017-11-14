package gollorum.signpost.commands;

import gollorum.signpost.SPEventHandler;
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
	public void execute(MinecraftServer server, final ICommandSender sender, String[] args) {
		if(sender instanceof EntityPlayerMP){
			SPEventHandler.scheduleTask(new Runnable(){
				@Override
				public void run() {
					PostHandler.confirm((EntityPlayerMP) sender);
				}
			}, 0);
		}
	}

	@Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender){
    	return sender instanceof EntityPlayer;
    }

}