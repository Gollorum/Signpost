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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;

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
			if(!tileEntity.hasLevel()) throw new RuntimeException("TileEntity without world cannot be rendered.");
			RenderingUtil.wrapInMatrixEntry(matrix, () -> {
				Quaternion rotation = new Quaternion(Vector3f.YP, sign.getAngle().radians(), false);
				matrix.mulPose(rotation);
				RenderingUtil.wrapInMatrixEntry(matrix, () -> {
					if(!sign.isFlipped()) matrix.mulPose(new Quaternion(Vector3f.YP, 180, true));
					renderText(sign, matrix, renderDispatcher.getFont(), buffer, combinedLights);
				});
				Matrix4f rotationMatrix = new Matrix4f(rotation);
				if(shouldRenderBaked)
					renderModel.render(
						makeBakedModel(sign),
						tileEntity.getLevel(),
						tileEntity.getBlockState(),
						tileEntity.getBlockPos(),
						buffer.getBuffer(RenderType.solid()), false, random, randomSeed, combinedOverlay, rotationMatrix
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
							tileEntity.getLevel(),
							tileEntity.getBlockState(),
							tileEntity.getBlockPos(),
							buffer.getBuffer(RenderType.cutoutMipped()), false, random, randomSeed, combinedOverlay, rotationMatrix
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
			});
		});
	}

	protected abstract void renderText(T sign, MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights);

	@Override
	public void renderGui(T sign, MatrixStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset) {
		RenderingUtil.renderGui(makeBakedModel(sign), matrixStack, 0xffffff, center, yaw.add(sign.getAngle()), pitch, isFlipped, scale, offset, RenderType.solid(),
			ms -> RenderingUtil.wrapInMatrixEntry(ms, () -> {
				if(!sign.isFlipped())
					ms.mulPose(new Quaternion(Vector3f.YP, 180, true));
				renderText(sign, ms, Minecraft.getInstance().font, Minecraft.getInstance().renderBuffers().bufferSource(), 0xf000f0);
			})
		);
		sign.getOverlay().ifPresent(o ->
			RenderingUtil.renderGui(makeBakedOverlayModel(sign, o), matrixStack, o.getDefaultTint(), center, yaw.add(sign.getAngle()), pitch, isFlipped, scale, offset, RenderType.cutout(), m -> {}));
	}

	@Override
	public void renderGui(T sign, MatrixStack matrixStack, Vector3 offset, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay) {
		RenderingUtil.renderGui(makeBakedModel(sign), matrixStack, 0xffffff, offset, sign.getAngle(), buffer.getBuffer(RenderType.solid()), combinedLight, combinedOverlay,
			ms -> RenderingUtil.wrapInMatrixEntry(matrixStack, () -> {
				if(!sign.isFlipped())
					matrixStack.mulPose(new Quaternion(Vector3f.YP, 180, true));
				renderText(sign, ms, Minecraft.getInstance().font, buffer, combinedLight);
			})
		);
		sign.getOverlay().ifPresent(o -> {
			RenderingUtil.renderGui(makeBakedOverlayModel(sign, o), matrixStack, o.getDefaultTint(), offset, sign.getAngle(), buffer.getBuffer(RenderType.cutout()), combinedLight, combinedOverlay, m -> {});
		});
	}

}
