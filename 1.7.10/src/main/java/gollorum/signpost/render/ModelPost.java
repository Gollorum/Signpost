package gollorum.signpost.render;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gollorum.signpost.util.DoubleBaseInfo;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

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

	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5, DoubleBaseInfo tilebases, boolean isItem) {
		super.render(entity, f, f1, f2, f3, f4, f5);
		post.render(f5);
		if ((tilebases.base1 != null&&!tilebases.base1.name.equals("null")&&!tilebases.base1.name.equals("")) || isItem) {
			if (tilebases.flip1) {
				GL11.glRotated(180, 0, 0, 1);
				GL11.glTranslated(0, -1.5, 0);
				board1.setRotation(-tilebases.rotation1);
				board1.render(f5);
				GL11.glTranslated(0, 1.5, 0);
				GL11.glRotated(180, 0, 0, 1);
			} else {
				board1.setRotation(tilebases.rotation1);
				board1.render(f5);
			}
		}
		if ((tilebases.base2 != null&&!tilebases.base2.name.equals("null")&&!tilebases.base2.name.equals("")) || isItem) {
			if (tilebases.flip2) {
				GL11.glRotated(180, 0, 0, 1);
				GL11.glTranslated(0, -0.5, 0);
				board2.setRotation(-tilebases.rotation2);
				board2.render(f5);
				GL11.glTranslated(0, 0.5, 0);
				GL11.glRotated(180, 0, 0, 1);
			} else {
				board2.setRotation(tilebases.rotation2);
				board2.render(f5);
			}
		}
	}

}