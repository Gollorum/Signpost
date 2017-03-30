package gollorum.signpost.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class BlockItemRenderer extends ItemRenderer {

	private TileEntity tile;
	private TileEntitySpecialRenderer renderer;

	public BlockItemRenderer(TileEntity tile, TileEntitySpecialRenderer renderer) {
		super(Minecraft.getMinecraft());
		this.tile = tile;
		this.renderer = renderer;
	}

	@Override
    public void renderItem(EntityLivingBase entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform){
		GL11.glPushMatrix();
		TileEntityRendererDispatcher.instance.renderTileEntityAt(tile, 0, 0, 0, 0);
		GL11.glPopMatrix();
	}

}
