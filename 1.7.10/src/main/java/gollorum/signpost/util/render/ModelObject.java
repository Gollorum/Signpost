package gollorum.signpost.util.render;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

public interface ModelObject {

	public IModelCustom getModel();
	public ResourceLocation getTexture();

	public double rotX();
	public double rotY();
	public double rotZ();
	public double getAngle();

	public double transX();
	public double transY();
	public double transZ();
}
