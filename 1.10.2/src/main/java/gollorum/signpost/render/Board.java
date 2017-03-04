package gollorum.signpost.render;

import java.util.ArrayList;

import net.minecraft.client.model.ModelRenderer;

public class Board {
	
	private ModelRenderer mt;

	public Board(ModelPost modelPost, int heightOffset) {
		ModelRenderer lt = new ModelRenderer(modelPost, 15, -1);
		lt.addBox(-9, heightOffset+2f, 2, 1, 4, 1, 0f);
		lt.setTextureSize(24, 22);
		mt = new ModelRenderer(modelPost, -1, 15);
		mt.addBox(-8, heightOffset+1, 2, 18, 6, 1, 0.0F);
		mt.setTextureSize(24, 22);
		mt.addChild(lt);
		
    	int i = 1;
    	int hv = 3;
    	while(i<4){
    		ModelRenderer now = new ModelRenderer(modelPost, 15 , hv);
        	hv += 8-2*i;
        	now.addBox(8+i*2, heightOffset+i, 2, 2, 8-2*i, 1, 0f);
    		now.setTextureSize(24, 22);
    		mt.addChild(now);
        	i++;
    	}
	}

	public void render(float f5) {
		mt.render(f5);
	}

    public void setRotation(float rot){
		mt.rotateAngleY = (float) Math.toRadians(rot);
    }
}
