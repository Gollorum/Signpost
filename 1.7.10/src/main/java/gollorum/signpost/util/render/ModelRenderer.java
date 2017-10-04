package gollorum.signpost.util.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

public class ModelRenderer extends TileEntitySpecialRenderer {

	@Override
	public void renderTileEntityAt(TileEntity ti, double x, double y, double z, float scale) {
		if(!(ti instanceof ModelObject)){
			System.out.println("NO MODEL OBJECT");
			return;
		}
		ModelObject tile = (ModelObject)ti;
		setTexture(tile.getTexture());
		GL11.glPushMatrix();
		GL11.glTranslated(x + tile.transX()+0.5, y + tile.transY(), z + tile.transZ()+0.5);
		GL11.glRotated(tile.getAngle(), tile.rotX(),  tile.rotY(),  tile.rotZ());
		GL11.glTranslated(-0.5, 0, -0.5);		
		IModelCustom model = tile.getModel();
		if(model != null){
			model.renderAll();
		}else{
//			System.out.println("DAT MODELL IST NULL UND NICHTIG!!!");
		}
		GL11.glPopMatrix();
	}

	void setTexture(ResourceLocation loc){
		try{
			bindTexture(loc);
		}catch(Exception e){
			if(loc.equals(new ResourceLocation("signpost:textures/blocks/sign.png"))){
				bindTexture(new ResourceLocation("signpost:textures/blocks/sign_oak.png"));
			}else if(loc.equals(new ResourceLocation("signpost:textures/blocks/bigsign.png"))){
				bindTexture(new ResourceLocation("signpost:textures/blocks/bigsign_oak.png"));
			}
		}
	}
	
}
