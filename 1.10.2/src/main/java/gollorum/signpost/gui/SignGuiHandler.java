package gollorum.signpost.gui;

import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.BasePostTile;
import gollorum.signpost.blocks.PostPostTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class SignGuiHandler implements IGuiHandler {

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		switch (ID) {
		case Signpost.GuiBaseID:
			return new SignGuiBase((BasePostTile) world.getTileEntity(new BlockPos(x, y, z)));
		case Signpost.GuiPostID:
			return new SignGuiPost((PostPostTile) world.getTileEntity(new BlockPos(x, y, z)));
		}
		System.out.println("return null");
		return null;
	}

}
