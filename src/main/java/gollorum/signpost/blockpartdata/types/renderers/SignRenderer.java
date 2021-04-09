package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.gui.Colors;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.Sign;
import gollorum.signpost.utils.modelGeneration.SignModel;
import net.minecraft.client.Minecraft;
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

import static gollorum.signpost.minecraft.rendering.RenderingUtil.withTintIndex;

public abstract class SignRenderer<T extends Sign<T>> extends BlockPartRenderer<T> {

	private final boolean shouldRenderBaked = false;

	protected abstract IBakedModel makeBakedModel(T sign);
	protected abstract IBakedModel makeBakedOverlayModel(T sign, Overlay overlay);
	protected abstract SignModel makeModel(T sign);
	protected abstract SignModel makeOverlayModel(T sign, Overlay overlay);

	@Override
	public void render(T sign, TileEntity tileEntity, TileEntityRendererDispatcher renderDispatcher, MatrixStack matrix, IRenderTypeBuffer buffer, int combinedLights, int combinedOverlay, Random random, long randomSeed) {
		RenderingUtil.render(matrix, renderModel -> {
			matrix.push();
			Quaternion rotation = new Quaternion(Vector3f.YP, sign.getAngle().radians(), false);
			matrix.rotate(rotation);
			Matrix4f rotationMatrix = new Matrix4f(rotation);
			if(shouldRenderBaked)
				renderModel.render(
					makeBakedModel(sign),
					tileEntity, buffer.getBuffer(RenderType.getSolid()), false, random, randomSeed, combinedOverlay, rotationMatrix
				);
			else makeModel(sign).render(
				matrix.getLast(),
				buffer,
				RenderType.getSolid(),
				combinedLights,
				combinedOverlay,
				1, 1, 1
			);
			sign.getOverlay().ifPresent(o -> {
				if(shouldRenderBaked)
					renderModel.render(
						makeBakedOverlayModel(sign, o),
						tileEntity, buffer.getBuffer(RenderType.getCutoutMipped()), false, random, randomSeed, combinedOverlay, rotationMatrix
					);
				else {
					int tint = o.getTintAt(tileEntity.getWorld(), tileEntity.getPos());
					makeOverlayModel(sign, o).render(
						matrix.getLast(),
						buffer,
						RenderType.getCutoutMipped(),
						combinedLights,
						combinedOverlay,
						Colors.getRed(tint) / 255f, Colors.getGreen(tint) / 255f, Colors.getBlue(tint) / 255f
					);
				}
			});
			matrix.pop();
			renderText(sign, matrix, renderDispatcher.getFontRenderer(), buffer, combinedLights);
		});
	}

	protected abstract void renderText(T sign, MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights);

}
