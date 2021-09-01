package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.modelGeneration.SignModel;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;

import java.util.Random;

public abstract class SignRenderer<T extends SignBlockPart<T>> extends BlockPartRenderer<T> {

	private final boolean shouldRenderBaked = false;

	protected abstract IBakedModel makeBakedModel(T sign);
	protected abstract IBakedModel makeBakedOverlayModel(T sign, Overlay overlay);
	protected abstract SignModel makeModel(T sign);
	protected abstract SignModel makeOverlayModel(T sign, Overlay overlay);

	@Override
	public void render(T sign, TileEntity tileEntity, TileEntityRendererDispatcher renderDispatcher, MatrixStack matrix, IRenderTypeBuffer buffer, int combinedLights, int combinedOverlay, Random random, long randomSeed) {
		RenderingUtil.render(matrix, renderModel -> {
			matrix.pushPose();
			Quaternion rotation = new Quaternion(Vector3f.YP, sign.getAngle().radians(), false);
			matrix.mulPose(rotation);
			Matrix4f rotationMatrix = new Matrix4f(rotation);
			if(shouldRenderBaked)
				renderModel.render(
					makeBakedModel(sign),
					tileEntity, buffer.getBuffer(RenderType.solid()), false, random, randomSeed, combinedOverlay, rotationMatrix
				);
			else makeModel(sign).render(
				matrix.last(),
				buffer,
				RenderType.solid(),
				combinedLights,
				combinedOverlay,
				1, 1, 1
			);
			sign.getOverlay().ifPresent(o -> {
				if(shouldRenderBaked)
					renderModel.render(
						makeBakedOverlayModel(sign, o),
						tileEntity, buffer.getBuffer(RenderType.cutoutMipped()), false, random, randomSeed, combinedOverlay, rotationMatrix
					);
				else {
					int tint = o.getTintAt(tileEntity.getLevel(), tileEntity.getBlockPos());
					makeOverlayModel(sign, o).render(
						matrix.last(),
						buffer,
						RenderType.cutoutMipped(),
						combinedLights,
						combinedOverlay,
						Colors.getRed(tint) / 255f, Colors.getGreen(tint) / 255f, Colors.getBlue(tint) / 255f
					);
				}
			});
			matrix.popPose();
			renderText(sign, matrix, renderDispatcher.getFont(), buffer, combinedLights);
		});
	}

	protected abstract void renderText(T sign, MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights);

	@Override
	public void renderGui(T part, Point center, Angle yaw, Angle pitch, float scale, Vector3 offset) {
		RenderingUtil.renderGui(makeBakedModel(part), center, yaw.add(part.getAngle()), pitch, scale, offset, false, RenderType.solid());
	}

}
