package gollorum.signpost.render;

import org.lwjgl.opengl.GL11;

import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.util.BigBaseInfo;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

public class ModelBigPost extends ModelBase {

	public ModelRenderer post;
	
	public ModelRenderer waystone;
	public static final ResourceLocation BASETEXTURE = new ResourceLocation("minecraft:textures/blocks/stone.png");
//	public static final ResourceLocation BASETEXTURE = new ResourceLocation("signpost:textures/blocks/base.png");
	
	public ModelBigPost(){

		textureWidth = 16;
		textureHeight = 16;

		post = new ModelRenderer(this, 0, -4);
		post.addBox(-2F, 0F, -2F, 4, 16, 4, 0F);
		
		waystone = new ModelRenderer(this, 4, 4);
		waystone.addBox(-4f, 0f, -4f, 8, 8, 8);
	}

	public void render(BigPostRenderer postRenderer, float f1, float f5, BigBaseInfo tilebases, BigPostPostTile tile, double rotation) {
		if(tile.isAwaitingPaint() && tile.getPaintObject() instanceof BigBaseInfo){
			postRenderer.setTexture(tilebases.POST_PAINT);
		}else{
			postRenderer.setTexture(tilebases.postPaint);
		}
		GL11.glPushMatrix();
		GL11.glRotated(180, 1, 0, 0);
		GL11.glTranslated(0, -1, 0);
		post.render(f5);
		GL11.glPopMatrix();
		if(tile.isWaystone){
			postRenderer.setTexture(BASETEXTURE);
			waystone.render(f5);
		}
	}
	
}
