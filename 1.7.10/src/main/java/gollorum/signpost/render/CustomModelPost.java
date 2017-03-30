package gollorum.signpost.render;

import org.lwjgl.opengl.GL11;

import gollorum.signpost.blocks.CustomPostPostTile;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.util.DoubleBaseInfo;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.texture.TextureMap;

public class CustomModelPost extends ModelPost {

	public CustomModelPost(){
		board1 = new Board(this, 8);
		board2 = new Board(this, 0);

		post = new ModelRenderer(this, 0, 0);
		post.addBox(-2F, 0F, -2F, 4, 16, 4, 0.0F);
	}

	@Override
	public void render(PostRenderer postRenderer, float f1, float f5, DoubleBaseInfo tilebases, PostPostTile tile, double rotation1, double rotation2) {
		postRenderer.setTexture(TextureMap.locationBlocksTexture);
		CustomPostPostTile t = (CustomPostPostTile)tile;
		super.render(null, 0, f1, 0, 0, 0, f5);
		post.render(f5);
		if ((tilebases.base1 != null&&!tilebases.base1.name.equals("null")&&!tilebases.base1.name.equals("")) || tile.isItem) {
//			this.textureWidth = (int) (t.uMax1-t.uMin1);
//			this.textureHeight = (int) (t.vMax1-t.vMin1);
			board1.setTextureOffset((int)t.uMin1, (int)t.vMin1);
			board1.setTextureSize((int) (t.uMax1-t.uMin1), (int) (t.vMax1-t.vMin1));
			GL11.glPushMatrix();
			GL11.glRotated(180, 0, 0, 1);
			GL11.glTranslated(0, -1.5, 0);
			GL11.glRotated(-Math.toDegrees(rotation1), 0, 1, 0);
			board1.render(f5, tilebases.flip1);
			GL11.glPopMatrix();
		}
		if ((tilebases.base2 != null&&!tilebases.base2.name.equals("null")&&!tilebases.base2.name.equals("")) || tile.isItem) {
			this.textureWidth = (int) (t.uMax2-t.uMin2);
			this.textureHeight = (int) (t.vMax2-t.vMin2);
			board2.setTextureOffset((int)t.uMin2, (int)t.vMin2);
			board1.setTextureSize((int) (t.uMax2-t.uMin2), (int) (t.vMax2-t.vMin2));
			GL11.glPushMatrix();
			GL11.glRotated(180, 0, 0, 1);
			GL11.glTranslated(0, -0.5, 0);
			GL11.glRotated(-Math.toDegrees(rotation2), 0, 1, 0);
			board2.render(f5, tilebases.flip2);
			GL11.glPopMatrix();
		}
	}
}
