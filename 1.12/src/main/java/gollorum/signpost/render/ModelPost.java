package gollorum.signpost.render;

import org.lwjgl.opengl.GL11;

import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.util.DoubleBaseInfo;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelPost extends ModelBase {
	public Board board1;
	public Board board2;
	public ModelRenderer post;
	public ModelRenderer waystone;
	public static final ResourceLocation BASETEXTURE = new ResourceLocation("minecraft:textures/blocks/stone.png");
//	public static final ResourceLocation BASETEXTURE = new ResourceLocation("signpost:textures/blocks/base.png");
	private static final String __OBFID = "CL_00000854";

	public ModelPost() {

		textureWidth = 16;
		textureHeight = 16;

		board1 = new Board(this, 8);
		board2 = new Board(this, 0);

		post = new ModelRenderer(this, 0, -4);
		post.addBox(-2F, 0F, -2F, 4, 16, 4, 0F);
		
		waystone = new ModelRenderer(this, 4, 4);
		waystone.addBox(-4f, 0f, -4f, 8, 8, 8);
	}

	public void render(PostRenderer postRenderer, float f1, float f5, DoubleBaseInfo tilebases, PostPostTile tile, double rotation1, double rotation2) {
		super.render(null, 0, f1, 0, 0, 0, f5);
		if(tile.isAwaitingPaint() && tile.getPaintObject() instanceof DoubleBaseInfo){
			postRenderer.setTexture(tilebases.POST_PAINT);
		}else{
			postRenderer.setTexture(tilebases.postPaint);
		}

		GL11.glPushMatrix();
		GL11.glTranslated(0, 1, 0);
		GL11.glRotated(180, 1, 0, 0);
		post.render(f5);
		GL11.glPopMatrix();
		
		if(tile.isWaystone){
			postRenderer.setTexture(BASETEXTURE);
			waystone.render(f5);
		}
		ResourceLocation mainLoc = tile.type.texture;
		if (tile.isItem || tilebases.sign1.isValid()) {
			GL11.glPushMatrix();
			GL11.glRotated(180, 0, 0, 1);
			GL11.glTranslated(0, -1.5, 0);
			GL11.glRotated(-Math.toDegrees(rotation1), 0, 1, 0);
			if(tile.isItem){
				postRenderer.setTexture(tile.type.texture);
				board1.render(f5, false);
			}else if(tile.isAwaitingPaint() && tilebases.sign1.equals(tile.getPaintObject())){
				postRenderer.setTexture(tilebases.sign1.SIGN_PAINT);
				board1.render(f5, tilebases.sign1.flip);
			}else if(tilebases.sign1.paint!=null){
				postRenderer.setTexture(tilebases.sign1.paint);
				board1.render(f5, tilebases.sign1.flip);
			}else{
				postRenderer.setTexture(mainLoc);
				board1.render(f5, tilebases.sign1.flip);
			}
			GL11.glPopMatrix();
		}
		if (tile.isItem || tilebases.sign2.isValid()) {
			GL11.glPushMatrix();
			GL11.glRotated(180, 0, 0, 1);
			GL11.glTranslated(0, -0.5, 0);
			GL11.glRotated(-Math.toDegrees(rotation2), 0, 1, 0);
			if(tile.isItem){
				postRenderer.setTexture(tile.type.texture);
				board2.render(f5, false);
			}else if(tile.isAwaitingPaint() && tilebases.sign2.equals(tile.getPaintObject())){
				postRenderer.setTexture(tilebases.sign2.SIGN_PAINT);
				board2.render(f5, tilebases.sign2.flip);
			}else if(tilebases.sign2.paint!=null){
				postRenderer.setTexture(tilebases.sign2.paint);
				board2.render(f5, tilebases.sign2.flip);
			}else{
				postRenderer.setTexture(mainLoc);
				board2.render(f5, tilebases.sign2.flip);
			}
			GL11.glPopMatrix();
		}
	}

	public void renderOverlay1(DoubleBaseInfo tilebases, float f5, double rotation) {
		GL11.glPushMatrix();
		GL11.glRotated(Math.toDegrees(rotation), 0, 1, 0);
		GL11.glTranslated(0, 0.75, 2.5/16.0);
		GL11.glScaled(1.01, 1.01, 1.1);
		GL11.glTranslated(0, -0.75, -2.5/16.0);
		GL11.glRotated(180, 0, 0, 1);
		GL11.glTranslated(0, -1.5, 0);
		board1.render(f5, tilebases.sign1.flip);
		GL11.glPopMatrix();
	}
	
	public void renderOverlay2(DoubleBaseInfo tilebases, float f5, double rotation) {
		GL11.glPushMatrix();
		GL11.glRotated(Math.toDegrees(rotation), 0, 1, 0);
		GL11.glTranslated(0, 0.25, 2.5/16.0);
		GL11.glScaled(1.01, 1.01, 1.1);
		GL11.glTranslated(0, -0.25, -2.5/16.0);
		GL11.glRotated(180, 0, 0, 1);
		GL11.glTranslated(0, -0.5, 0);
		board2.render(f5, tilebases.sign2.flip);
		GL11.glPopMatrix();
	}

}