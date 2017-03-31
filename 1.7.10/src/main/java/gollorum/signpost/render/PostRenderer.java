package gollorum.signpost.render;

import org.lwjgl.opengl.GL11;

import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.math.tracking.DDDVector;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class PostRenderer extends TileEntitySpecialRenderer{

	private static final ModelPost model = new ModelPost();
	
	public PostRenderer(){}
	
	void setTexture(ResourceLocation loc){
		bindTexture(loc);
	}
	
	@Override
	public void renderTileEntityAt(TileEntity ti, double x, double y, double z, float scale) {
		PostPostTile tile = (PostPostTile)ti;
		DoubleBaseInfo tilebases = tile.bases;
		if(tilebases==null){
			tilebases = tile.getBases();
		}
		double rotation1 = PostPostTile.calcRot1(tilebases, tile.xCoord, tile.zCoord);
		double rotation2 = PostPostTile.calcRot2(tilebases, tile.xCoord, tile.zCoord);
		GL11.glPushMatrix();
		GL11.glTranslated(x+0.5, y, z+0.5);
		this.bindTexture(tile.type.texture);
		model.render(this, 0.1f, 0.0625f, tilebases, tile, rotation1, rotation2);

		//Overlays
		if(!tile.isItem){
			if(tilebases.base1!=null && tilebases.overlay1!=null){
				bindTexture(new ResourceLocation(Signpost.MODID + ":textures/blocks/sign_overlay_"+tilebases.overlay1.texture+".png"));
				model.renderOverlay1(tilebases, 0.0625f, rotation1);
			}
			if(tilebases.base2!=null && tilebases.overlay2!=null){
				bindTexture(new ResourceLocation(Signpost.MODID + ":textures/blocks/sign_overlay_"+tilebases.overlay2.texture+".png"));
				model.renderOverlay2(tilebases, 0.0625f, rotation2);
			}
		}
        
        FontRenderer fontrenderer = this.func_147498_b();
        GL11.glPushMatrix();
		GL11.glTranslated(0, 0.8d, 0);
		GL11.glRotated(180, 0, 0, 1);
		GL11.glRotated(180, 0, 1, 0);
		double sc = 0.013d;
		double ys = 1.3d*sc;
        
		int color = (1<<16) + (1<<8);
		
        if(!tile.isItem){
        	if(tilebases.base1!=null&&!tilebases.base1.name.equals("null")&&!tilebases.base1.name.equals("")){
        		String s = tilebases.base1.name;
        		double sc2 = 100d/fontrenderer.getStringWidth(s);
        		if(sc2>=1){
        			sc2 = 1;
        		}
        		double lurch = (tilebases.flip1?-0.2:0.2)-fontrenderer.getStringWidth(s)*sc*sc2/2;
            	double alpha = Math.atan(lurch*16/3.001);
            	double d = Math.sqrt(Math.pow(3.001/16, 2)+Math.pow(lurch, 2));
            	double beta = alpha + rotation1;
            	double dx = Math.sin(beta)*d;
            	double dz = -Math.cos(beta)*d;
            	GL11.glPushMatrix();
        		GL11.glTranslated(dx, 0, dz);
        		GL11.glScaled(sc, ys, sc);
        		GL11.glRotated(-Math.toDegrees(rotation1), 0, 1, 0);
        		GL11.glScaled(sc2,  sc2,  sc2);
                fontrenderer.drawString(s, 0, 0, color);
                GL11.glPopMatrix();
        	}

        	if(tilebases.base2!=null&&!tilebases.base2.name.equals("null")&&!tilebases.base2.name.equals("")){
        		GL11.glTranslated(0, 0.5d, 0);
        		String s = tilebases.base2.name;
        		double sc2 = 100d/fontrenderer.getStringWidth(s);
        		if(sc2>=1){
        			sc2 = 1;
        		}
        		double lurch = (tilebases.flip2?-0.2:0.2)-fontrenderer.getStringWidth(s)*sc*sc2/2;
            	double alpha = Math.atan(lurch*16/3.001);
            	double d = Math.sqrt(Math.pow(3.001/16, 2)+Math.pow(lurch, 2));
            	double beta = alpha + rotation2;
            	double dx = Math.sin(beta)*d;
            	double dz = -Math.cos(beta)*d;
    			GL11.glPushMatrix();
        		GL11.glTranslated(dx, 0, dz);
        		GL11.glScaled(sc, ys, sc);
        		GL11.glRotated(-Math.toDegrees(rotation2), 0, 1, 0);
        		GL11.glScaled(sc2,  sc2,  sc2);
                fontrenderer.drawString(s, 0, 0, color);
                GL11.glPopMatrix();
        	}
        }

        GL11.glPopMatrix();
        GL11.glPopMatrix();

	}

}
