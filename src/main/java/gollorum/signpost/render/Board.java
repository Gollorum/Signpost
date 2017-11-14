package gollorum.signpost.render;

import net.minecraft.client.model.ModelRenderer;

public class Board {
	
	private ModelRenderer baseNotFlipped;
	private ModelRenderer baseFlipped;

	public Board(ModelPost modelPost, int heightOffset) {
		
		//Not flipped
		ModelRenderer ltnf = new ModelRenderer(modelPost, 12, 9);
		ltnf.addBox(-9, heightOffset+2f, 2, 1, 4, 1, 0f);
		baseNotFlipped = new ModelRenderer(modelPost, -1, 0);
		baseNotFlipped.addBox(-8, heightOffset+1, 2, 20, 6, 1, 0.0F);
		baseNotFlipped.addChild(ltnf);
		
    	int i = 2;
    	while(i<4){
    		ModelRenderer now = new ModelRenderer(modelPost, 6*i-12 , 7+i);
        	now.addBox(8+i*2, heightOffset+i, 2, 2, 8-2*i, 1, 0f);
        	now.rotationPointX += 18+i*4;
        	now.rotationPointZ += 5f;
        	now.rotateAngleY = (float) Math.PI;
    		baseNotFlipped.addChild(now);
        	i++;
    	}
		
		//flipped
    	ModelRenderer ltf = new ModelRenderer(modelPost, 12, 9);
		ltf.addBox(8, heightOffset+2f, 2, 1, 4, 1, 0f);
		baseFlipped = new ModelRenderer(modelPost, -1, 0);
		baseFlipped.addBox(-12, heightOffset+1, 2, 20, 6, 1, 0.0F);
		baseFlipped.addChild(ltf);
		
    	i = 2;
    	while(i<4){
    		ModelRenderer now = new ModelRenderer(modelPost, 6*i-12 , 7+i);
        	now.addBox(-10-i*2, heightOffset+i, 2, 2, 8-2*i, 1, 0f);
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
