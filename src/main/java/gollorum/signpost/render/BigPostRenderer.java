package gollorum.signpost.render;

import org.lwjgl.opengl.GL11;

import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.Sign;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;

public class BigPostRenderer extends TileEntitySpecialRenderer<BigPostPostTile>{

	private static final ModelBigSign model16 = new ModelBigSign(true);
	private static final ModelBigSign model32 = new ModelBigSign(false);
	private static final ModelBigPost post = new ModelBigPost();
	
	public BigPostRenderer(){}
	
	void setTexture(ResourceLocation loc){
		try{
		bindTexture(loc);
		}catch(Exception e){}
	}
	
	@Override
    public void render(BigPostPostTile tile, double x, double y, double z, float partialTicks, int destroyStage, float alfa){
		BigBaseInfo tilebases = tile.bases;
		double rotation = 0;
		if(tilebases==null && !tile.isItem){
			tilebases = tile.getBases();
		}
		if(!tile.isItem){
			rotation = tilebases.sign.calcRot(tile.getPos().getX(), tile.getPos().getZ());
		}
		
		GL11.glPushMatrix();
		GL11.glTranslated(x+0.5, y, z+0.5);
		post.render(this, 0.1f, 0.0625f, tilebases, tile, rotation);
		ResourceLocation resLoc;
		if(!tile.isItem && tile.isAwaitingPaint() && tile.getPaintObject() instanceof Sign){
			resLoc = tilebases.sign.BIGSIGN_PAINT;
		}else{
			resLoc = tile.isItem ? tile.type.texture : tilebases.sign.paint;
		}
		try{
			this.bindTexture(resLoc);
		}catch(Exception e){
			this.setTexture(resLoc = tile.type.texture);
			tilebases.sign.paint = resLoc;
		}
		if(resLoc.getNamespace().equals("signpost")){
			model32.render(this, 0.1f, 0.0625f, tilebases, tile, rotation);
		}else{
			model16.render(this, 0.1f, 0.0625f, tilebases, tile, rotation);
		}

		//Overlays
		if(!tile.isItem){
			if(tilebases.sign.base!=null && tilebases.sign.overlay!=null){
				setTexture(new ResourceLocation(Signpost.MODID + ":textures/blocks/bigsign_overlay_"+tilebases.sign.overlay.texture+".png"));
				model32.renderOverlay(tilebases, 0.0625f, rotation);
			}
		}
        
        FontRenderer fontrenderer = this.getFontRenderer();
        GL11.glPushMatrix();
		GL11.glTranslated(0, 1.12, 0);
		GL11.glRotated(180, 0, 0, 1);
		GL11.glRotated(180, 0, 1, 0);
		double sc = 0.013d;
        
		int color = 0;
		
        if(!tile.isItem){
        	if(tilebases.sign.base!=null&&!tilebases.sign.base.getName().equals("null")&&!tilebases.sign.base.getName().equals("")){
        		GL11.glTranslated(0, 0.1, 0);
        		for(String s: tilebases.description){
	        		GL11.glTranslated(0, 0.165, 0);
	        		if(s==null){
	        			continue;
	        		}
	        		double sc2 = 90d/fontrenderer.getStringWidth(s);
	        		if(sc2>=1){
	        			sc2 = 1;
	        		}
	        		double lurch = (tilebases.sign.flip?-0.1:0.1)-fontrenderer.getStringWidth(s)*sc*sc2/2;
	            	double alpha = Math.atan(lurch*16/3.001);
	            	double d = Math.sqrt(Math.pow(3.001/16, 2)+Math.pow(lurch, 2));
	            	double beta = alpha + rotation;
	            	double dx = Math.sin(beta)*d;
	            	double dz = -Math.cos(beta)*d;
	    			GL11.glPushMatrix();
	        		GL11.glTranslated(dx, 0, dz);
	        		GL11.glScaled(sc, sc, sc);
	        		GL11.glRotated(-Math.toDegrees(rotation), 0, 1, 0);
	        		GL11.glScaled(sc2,  sc2,  sc2);
        			fontrenderer.drawString(s, 0, 0, color);
        			GL11.glPopMatrix();
        		}
        	}
        }

        GL11.glPopMatrix();
        GL11.glPopMatrix();

	}

}
