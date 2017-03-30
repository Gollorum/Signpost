package gollorum.signpost.render;

import java.util.ArrayList;

import net.minecraft.client.model.ModelRenderer;

public class Board {
	
	private ModelRenderer baseNotFlipped;
	private ModelRenderer baseFlipped;

	public Board(ModelPost modelPost, int heightOffset) {
		
		//Not flipped
		ModelRenderer ltnf = new ModelRenderer(modelPost, 15, -1);
		ltnf.addBox(-9, heightOffset+2f, 2, 1, 4, 1, 0f);
//		ltnf.setTextureSize(24, 22);
//		ltnf.setTextureSize(32, 32);
		baseNotFlipped = new ModelRenderer(modelPost, -1, 15);
		baseNotFlipped.addBox(-8, heightOffset+1, 2, 18, 6, 1, 0.0F);
//		baseNotFlipped.setTextureSize(24, 22);
//		ltnf.setTextureSize(32, 32);
		baseNotFlipped.addChild(ltnf);
		
    	int i = 1;
    	int hv = 3;
    	while(i<4){
    		ModelRenderer now = new ModelRenderer(modelPost, 15 , hv);
        	hv += 8-2*i;
        	now.addBox(8+i*2, heightOffset+i, 2, 2, 8-2*i, 1, 0f);
//    		ltnf.setTextureSize(24, 22);
//    		ltnf.setTextureSize(32, 32);
    		baseNotFlipped.addChild(now);
        	i++;
    	}
		
		//flipped
		ModelRenderer ltf = new ModelRenderer(modelPost, 15, -1);
		ltf.addBox(8, heightOffset+2f, 2, 1, 4, 1, 0f);
//		ltnf.setTextureSize(24, 22);
//		ltnf.setTextureSize(32, 32);
		baseFlipped = new ModelRenderer(modelPost, -1, 15);
		baseFlipped.addBox(-10, heightOffset+1, 2, 18, 6, 1, 0.0F);
//		baseFlipped.setTextureSize(24, 22);
//		ltnf.setTextureSize(32, 32);
		baseFlipped.addChild(ltf);
		
    	i = 1;
    	hv = 3;
    	while(i<4){
    		ModelRenderer now = new ModelRenderer(modelPost, 15 , hv);
        	hv += 8-2*i;
        	now.addBox(-10-i*2, heightOffset+i, 2, 2, 8-2*i, 1, 0f);
//    		ltnf.setTextureSize(24, 22);
//    		ltnf.setTextureSize(32, 32);
    		baseFlipped.addChild(now);
        	i++;
    	}
	}

	public void render(float f5, boolean flipped) {
		if(!flipped){
			baseFlipped.render(f5);
		}else{
			baseNotFlipped.render(f5);
		}
	}

    public void setRotation(float rot){
    	baseNotFlipped.rotateAngleY = baseFlipped.rotateAngleY = (float) Math.toRadians(rot);
    }
    
    public void setTextureOffset(int u, int v){
    	baseNotFlipped.setTextureOffset(u, v);
    	baseFlipped.setTextureOffset(u, v);
    }
    
    public void setTextureSize(int u, int v){
    	baseNotFlipped.setTextureSize(u, v);
    	baseFlipped.setTextureSize(u, v);
    }
}
