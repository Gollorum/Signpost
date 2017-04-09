package gollorum.signpost.render;

import org.lwjgl.opengl.GL11;

import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.BigPostPostTile;
import gollorum.signpost.util.BigBaseInfo;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class BigPostRenderer extends TileEntitySpecialRenderer{

	private static final ModelBigPost model = new ModelBigPost();
	
	public BigPostRenderer(){}
	
	void setTexture(ResourceLocation loc){
		bindTexture(loc);
	}
	
	@Override
	public void renderTileEntityAt(TileEntity ti, double x, double y, double z, float scale) {
		BigPostPostTile tile = (BigPostPostTile)ti;
		BigBaseInfo tilebases = tile.bases;
		if(tilebases==null){
			tilebases = tile.getBases();
		}
		double rotation = BigPostPostTile.calcRot(tilebases, tile.xCoord, tile.zCoord);
		GL11.glPushMatrix();
		GL11.glTranslated(x+0.5, y, z+0.5);
		this.bindTexture(tile.type.texture);
		model.render(this, 0.1f, 0.0625f, tilebases, tile, rotation);

		//Overlays
		if(!tile.isItem){
			if(tilebases.base!=null && tilebases.overlay!=null){
				bindTexture(new ResourceLocation(Signpost.MODID + ":textures/blocks/bigsign_overlay_"+tilebases.overlay.texture+".png"));
				model.renderOverlay(tilebases, 0.0625f, rotation);
			}
		}
        
        FontRenderer fontrenderer = this.func_147498_b();
        GL11.glPushMatrix();
		GL11.glTranslated(0, 1.12, 0);
		GL11.glRotated(180, 0, 0, 1);
		GL11.glRotated(180, 0, 1, 0);
		double sc = 0.013d;
//		double ys = sc;
        
		int color = (1<<16) + (1<<8);
		
        if(!tile.isItem){
        	if(tilebases.base!=null&&!tilebases.base.name.equals("null")&&!tilebases.base.name.equals("")){
        		for(String s: tilebases.description){
	        		GL11.glTranslated(0, 0.2, 0);
	        		if(s==null){
	        			continue;
	        		}
	        		double sc2 = 90d/fontrenderer.getStringWidth(s);
	        		if(sc2>=1){
	        			sc2 = 1;
	        		}
	        		double lurch = (tilebases.flip?-0.1:0.1)-fontrenderer.getStringWidth(s)*sc*sc2/2;
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
