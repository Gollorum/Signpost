package gollorum.signpost.signdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.signdata.Overlay;
import gollorum.signpost.signdata.types.Sign;
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

	protected abstract IBakedModel makeModel(T sign);
	protected abstract IBakedModel makeOverlayModel(T sign, Overlay texture);

	@Override
	public void render(T sign, TileEntity tileEntity, TileEntityRendererDispatcher renderDispatcher, MatrixStack matrix, IRenderTypeBuffer buffer, int combinedLights, int combinedOverlay, Random random, long randomSeed) {
		RenderingUtil.render(matrix, renderModel -> {
			matrix.push();
			Quaternion rotation = new Quaternion(Vector3f.YP, sign.getAngle().radians(), false);
			matrix.rotate(rotation);
			Matrix4f rotationMatrix = new Matrix4f(rotation);
			renderModel.render(
				makeModel(sign),
				tileEntity,
				buffer.getBuffer(RenderType.getSolid()),
				false,
				random,
				randomSeed,
				combinedOverlay,
				rotationMatrix
			);
			sign.getOverlay().ifPresent(o -> renderModel.render(
				withTintIndex(makeOverlayModel(sign, o), o.tintIndex),
				tileEntity,
				buffer.getBuffer(RenderType.getCutoutMipped()),
				true,
				random,
				randomSeed,
				combinedOverlay,
				rotationMatrix
			));
			matrix.pop();
			renderText(sign, matrix, renderDispatcher.getFontRenderer(), buffer, combinedLights);
		});
	}

	protected abstract void renderText(T sign, MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights);

}
