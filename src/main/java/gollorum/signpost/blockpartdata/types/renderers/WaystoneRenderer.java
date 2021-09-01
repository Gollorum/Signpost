package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.blockpartdata.types.WaystoneBlockPart;
import gollorum.signpost.minecraft.data.WaystoneModel;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraftforge.common.util.Lazy;

import java.util.Random;

public class WaystoneRenderer extends BlockPartRenderer<WaystoneBlockPart> {

	private static final Lazy<IBakedModel> model = Lazy.of(() -> RenderingUtil.loadModel(WaystoneModel.inPostLocation));

	@Override
	public void render(
		WaystoneBlockPart part,
		TileEntity tileEntity,
		TileEntityRendererDispatcher renderDispatcher,
		MatrixStack matrix,
		IRenderTypeBuffer buffer,
		int combinedLights,
		int combinedOverlay,
		Random random,
		long randomSeed
	) {
		RenderingUtil.render(matrix, renderModel -> renderModel.render(
			model.get(),
			tileEntity,
			buffer.getBuffer(RenderType.solid()),
			false,
			random,
			randomSeed,
			combinedOverlay,
			new Matrix4f(Quaternion.ONE)
		));
	}

	@Override
	public void renderGui(WaystoneBlockPart part, Point center, Angle yaw, Angle pitch, float scale, Vector3 offset) {
		RenderingUtil.renderGui(model.get(), center, yaw, pitch, scale, offset, false, RenderType.solid());
	}
}
