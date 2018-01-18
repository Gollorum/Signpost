package gollorum.signpost.blocks.tiles;

import gollorum.signpost.blocks.BaseModelPost.ModelType;
import gollorum.signpost.util.render.ModelObject;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

public class BaseModelPostTile extends BasePostTile implements ModelObject{

	private ModelType typ;
	
	private static final byte[] facingMatrix = new byte[]{1,0,3,2};
	
	public BaseModelPostTile(){
		this(ModelType.MODEL1);
	}
	
	public BaseModelPostTile(ModelType typ){
		super();
		this.typ = typ;
	}
	
	@Override
	public IModelCustom getModel() {
		return typ.MODEL;
	}

	@Override
	public ResourceLocation getTexture() {
		return typ.TEXTURE;
	}

	@Override
	public double rotX() {
		return 0;
	}

	@Override
	public double rotY() {
		return 1;
	}

	@Override
	public double rotZ() {
		return 0;
	}

	@Override
	public double getAngle() {
		if(worldObj==null){
			return 0;
		}else{
			return facingMatrix[worldObj.getBlockMetadata(xCoord, yCoord, zCoord)]*90;
		}
	}

	@Override
	public double transX() {
		return 0;
	}

	@Override
	public double transY() {
		return 0;
	}

	@Override
	public double transZ() {
		return 0;
	}
	
	@Override
	 public int getBlockMetadata(){
		try{
			if(worldObj==null){
				return 0;
			}else{
				return super.getBlockMetadata();
			}
		}catch(NullPointerException e){return 0;}
	}
	
}
