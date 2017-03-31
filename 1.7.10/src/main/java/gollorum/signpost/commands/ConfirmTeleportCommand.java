package gollorum.signpost.commands;

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
	public void processCommand(ICommandSender sender, String[] args) {
		if(sender instanceof EntityPlayerMP){
			PostHandler.confirm((EntityPlayerMP) sender);
		}
	}

	@Override
    public boolean canCommandSenderUseCommand(ICommandSender sender){
    	return sender instanceof EntityPlayer;
    }

}