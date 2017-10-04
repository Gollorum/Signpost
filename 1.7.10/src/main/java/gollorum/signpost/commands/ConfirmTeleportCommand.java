package gollorum.signpost.commands;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.management.PostHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class ConfirmTeleportCommand extends CommandBase {

	@Override
	public String getCommandName() {
		return "signpostconfirm";
	}

	@Override
	public String getCommandUsage(ICommandSender p_71518_1_) {
		return "/signpostconfirm";
	}

	@Override
	public void processCommand(final ICommandSender sender, String[] args) {
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
    public boolean canCommandSenderUseCommand(ICommandSender sender){
    	return sender instanceof EntityPlayer;
    }

}