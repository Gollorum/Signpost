package gollorum.signpost.gui;

import cpw.mods.fml.common.network.IGuiHandler;
import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.WaystoneContainer;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.util.MyBlockPos;
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
			return new SignGuiBase((WaystoneContainer) world.getTileEntity(x, y, z));
		case Signpost.GuiPostID:
			return new SignGuiPost((PostPostTile) world.getTileEntity(x, y, z));
		case Signpost.GuiBigPostID:
			return new SignGuiBigPost((BigPostPostTile) world.getTileEntity(x, y, z));
		case Signpost.GuiPostBrushID:
			SuperPostPostTile tile = (SuperPostPostTile) world.getTileEntity(x, y, z);
			return new SignGuiPaint(tile.getPaintable(player), tile);
		case Signpost.GuiPostRotationID:
			tile = (SuperPostPostTile) world.getTileEntity(x, y, z);
			return new SignGuiRotation(tile.getSign(player), tile);
		}
		return null;
	}

}