package gollorum.signpost.gui;

import cpw.mods.fml.common.network.IGuiHandler;
import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.BasePostTile;
import gollorum.signpost.blocks.PostPostTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class SignGuiHandler implements IGuiHandler {

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		switch (ID) {
		case Signpost.GuiBaseID:
			return new SignGuiBase((BasePostTile) world.getTileEntity(x, y, z));
		case Signpost.GuiPostID:
			return new SignGuiPost((PostPostTile) world.getTileEntity(x, y, z));
		}
		System.out.println("return null");
		return null;
	}

}
