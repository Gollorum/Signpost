package gollorum.signpost.gui;

import cpw.mods.fml.common.network.IGuiHandler;
import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.WaystoneContainer;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class SignGuiHandler implements IGuiHandler {

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		TileEntity ti =  world.getTileEntity(x, y, z);
		if(ti == null) return null;
		switch (ID) {
		case Signpost.GuiBaseID:
			return new SignGuiBase((WaystoneContainer) ti);
		case Signpost.GuiPostID:
			return new SignGuiPost((PostPostTile) ti);
		case Signpost.GuiBigPostID:
			return new SignGuiBigPost((BigPostPostTile) ti);
		case Signpost.GuiPostBrushID:
			SuperPostPostTile tile = (SuperPostPostTile) ti;
			return new SignGuiPaint(tile.getPaintable(player), tile);
		case Signpost.GuiPostRotationID:
			tile = (SuperPostPostTile) ti;
			return new SignGuiRotation(tile.getSign(player), tile);
		}
		return null;
	}

}