package gollorum.signpost.render;

import org.lwjgl.opengl.GL11;

import gollorum.signpost.blocks.PostPostTile;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelPost extends ModelBase {
	public Board board1;
	public Board board2;
	public ModelRenderer post;
	private static final String __OBFID = "CL_00000854";

	public ModelPost() {

		textureWidth = 24;
		textureHeight = 22;

		board1 = new Board(this, 8);
		board2 = new Board(this, 0);

		post = new ModelRenderer(this, 0, 0);
		post.addBox(-2F, 0F, -2F, 4, 16, 4, 0.0F);
	}

	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
		super.render(entity, f, f1, f2, f3, f4, f5);
		post.render(f5);
		board1.render(f5);
		board2.render(f5);
	}

	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5, PostPostTile tile) {
		super.render(entity, f, f1, f2, f3, f4, f5);
		post.render(f5);
		if ((tile.bases.base1 != null&&!tile.bases.base1.name.equals("null")&&!tile.bases.base1.name.equals("")) || tile.isItem) {
			if (tile.bases.flip1) {
				GL11.glRotated(180, 0, 0, 1);
				GL11.glTranslated(0, -1.5, 0);
				board1.setRotation(-tile.bases.rotation1);
				board1.render(f5);
				GL11.glTranslated(0, 1.5, 0);
				GL11.glRotated(180, 0, 0, 1);
			} else {
				board1.setRotation(tile.bases.rotation1);
				board1.render(f5);
			}
		}
		if ((tile.bases.base2 != null&&!tile.bases.base2.name.equals("null")&&!tile.bases.base2.name.equals("")) || tile.isItem) {
			if (tile.bases.flip2) {
				GL11.glRotated(180, 0, 0, 1);
				GL11.glTranslated(0, -0.5, 0);
				board2.setRotation(-tile.bases.rotation2);
				board2.render(f5);
				GL11.glTranslated(0, 0.5, 0);
				GL11.glRotated(180, 0, 0, 1);
			} else {
				board2.setRotation(tile.bases.rotation2);
				board2.render(f5);
			}
		}
	}

}