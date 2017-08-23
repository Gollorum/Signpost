package gollorum.signpost.gui;

import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.WaystoneContainer;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
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
			return new SignGuiBase((WaystoneContainer)world.getTileEntity(new BlockPos(x, y, z)));
		case Signpost.GuiPostID:
			return new SignGuiPost((PostPostTile) world.getTileEntity(new BlockPos(x, y, z)));
		case Signpost.GuiBigPostID:
			return new SignGuiBigPost((BigPostPostTile) world.getTileEntity(new BlockPos(x, y, z)));
		case Signpost.GuiPostBrushID:
			SuperPostPostTile tile = (SuperPostPostTile) world.getTileEntity(new BlockPos(x, y, z));
			return new SignGuiPaint(tile.getSign(player), tile);
		case Signpost.GuiPostRotationID:
			tile = (SuperPostPostTile) world.getTileEntity(new BlockPos(x, y, z));
			return new SignGuiRotation(tile.getSign(player), tile);
		}
		return null;
	}

}