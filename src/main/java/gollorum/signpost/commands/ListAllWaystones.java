package gollorum.signpost.commands;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

public class ListAllWaystones extends CommandBase {

	@Override
	public String getCommandName() {
		return "signpost";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/signpost list";
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (sender instanceof EntityPlayerMP) {
			if (args.length == 1 && args[0].toLowerCase().equals("list".toLowerCase())) {
				printAllWaystones(sender);
			} else {
				sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
			}
		}
	}

	private void printAllWaystones(ICommandSender sender) {
		for (BaseInfo base : PostHandler.getAllWaystones()) {
			if (base.hasName()) {
				sender.addChatMessage(new ChatComponentText(base.getName()));
			}
		}
	}

	@Override
    public boolean canCommandSenderUseCommand(ICommandSender sender){
    	return sender instanceof EntityPlayer;
    }

}
