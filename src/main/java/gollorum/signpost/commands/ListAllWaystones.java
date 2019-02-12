package gollorum.signpost.commands;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class ListAllWaystones extends CommandBase {

	@Override
	public String getName() {
		return "signpost";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/signpost list";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (sender instanceof EntityPlayerMP) {
			if (args.length == 1 && args[0].toLowerCase().equals("list".toLowerCase())) {
				printAllWaystones(sender);
			} else {
				sender.sendMessage(new TextComponentString("Usage: " + getUsage(sender)));
			}
		}
	}

	private void printAllWaystones(ICommandSender sender) {
		for (BaseInfo base : PostHandler.getAllWaystones()) {
			if (base.hasName()) {
				sender.sendMessage(new TextComponentString(base.getName()));
			}
		}
	}

	@Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender){
    	return sender instanceof EntityPlayer;
    }

}
