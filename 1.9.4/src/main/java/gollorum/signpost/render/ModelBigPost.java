package gollorum.signpost.render;

import gollorum.signpost.blocks.BigPostPostTile;
import gollorum.signpost.util.BigBaseInfo;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelBigPost extends ModelBase {

	public ModelRenderer post;
	
	public ModelBigPost(){

		textureWidth = 16;
		textureHeight = 16;

		post = new ModelRenderer(this, 0, -4);
		post.addBox(-2F, 0F, -2F, 4, 16, 4, 0.0F);
		
	}

	public void render(BigPostRenderer postRenderer, float f1, float f5, BigBaseInfo tilebases, BigPostPostTile tile, double rotation) {
		post.render(f5);
	}
	
}
