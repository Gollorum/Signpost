package gollorum.signpost.render;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gollorum.signpost.blocks.BigPostPostTile;
import gollorum.signpost.util.BigBaseInfo;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

@SideOnly(Side.CLIENT)
public class ModelBigPost extends ModelBase {
	
	public BigBoard board;
	public ModelRenderer post;
	private static final String __OBFID = "CL_00000854";

	public ModelBigPost() {

		textureWidth = 16;
		textureHeight = 16;

		board = new BigBoard(this);

		post = new ModelRenderer(this, 0, 0);
		post.addBox(-2F, 0F, -2F, 4, 16, 4, 0.0F);
	}

	public void render(BigPostRenderer postRenderer, float f1, float f5, BigBaseInfo tilebases, BigPostPostTile tile, double rotation) {
		super.render(null, 0, f1, 0, 0, 0, f5);
		post.render(f5);
		if ((tilebases.sign.base != null&&!tilebases.sign.base.name.equals("null")&&!tilebases.sign.base.name.equals("")) || tile.isItem) {
			GL11.glPushMatrix();
			GL11.glRotated(180, 0, 0, 1);
			GL11.glTranslated(0, -1, 0);
			GL11.glRotated(-Math.toDegrees(rotation), 0, 1, 0);
			if(tilebases.sign.paint!=null){
				postRenderer.setTexture(tilebases.sign.paint);
			}
			board.render(f5, tilebases.sign.flip);
			GL11.glPopMatrix();
		}
	}

	public void renderOverlay(BigBaseInfo tilebases, float f5, double rotation) {
		GL11.glPushMatrix();
		GL11.glRotated(Math.toDegrees(rotation), 0, 1, 0);
		GL11.glTranslated(0, 0.75, 2.5/16.0);
		GL11.glScaled(1.01, 1.01, 1.1);
		GL11.glTranslated(0, -0.75, -2.5/16.0);
		GL11.glRotated(180, 0, 0, 1);
		GL11.glTranslated(0, -1.5, 0);
		board.render(f5, tilebases.sign.flip);
		GL11.glPopMatrix();
	}
	
}