package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.minecraft.rendering.TexturedModel;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.*;

import java.lang.Math;

public abstract class SignRenderer<T extends SignBlockPart<T>> extends BlockPartRenderer<T> {

	protected abstract TexturedModel makeMainModel(T sign);
	protected abstract TexturedModel makeSecondaryModel(T sign);
	protected abstract TexturedModel makeBakedOverlayModel(T sign, Overlay overlay);

	@Override
	public void render(
		T sign,
		BlockEntity tileEntity,
		BlockEntityRenderDispatcher renderDispatcher,
		PoseStack blockToView,
		PoseStack localToBlock,
		MultiBufferSource buffer,
		int combinedLights,
		int combinedOverlay,
		RandomSource random,
		long randomSeed
	) {
		if(sign.isMarkedForGeneration() && !IConfig.IServer.getInstance().worldGen().debugMode()) return;
		if(!tileEntity.hasLevel()) throw new RuntimeException("TileEntity without world cannot be rendered.");
		RenderingUtil.wrapInMatrixEntry(localToBlock, () -> {
			Quaternionf rotation = new Quaternionf(new AxisAngle4f(sign.getAngle().get().radians(), new Vector3f(0,1,0)));
			localToBlock.mulPose(rotation);
			RenderingUtil.wrapInMatrixEntry(blockToView, () -> {
				blockToView.mulPose(localToBlock.last().pose());
				if(!sign.isFlipped()) blockToView.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0,1,0))));
				renderText(sign, blockToView, Minecraft.getInstance().font, buffer, combinedLights);
			});
			var colorMain = sign.getMainTexture().tint().map(t -> t.getColorAt(tileEntity.getLevel(), tileEntity.getBlockPos())).orElse(Colors.white);
			var colorSecondary = sign.getSecondaryTexture().tint().map(t -> t.getColorAt(tileEntity.getLevel(), tileEntity.getBlockPos())).orElse(Colors.white);
            RenderingUtil.render(
                blockToView,
                makeMainModel(sign),
				buffer.getBuffer(RenderType.entitySolid(sign.getMainTexture().location())),
				combinedLights,
				combinedOverlay,
				colorMain
            );
            RenderingUtil.render(
                blockToView,
                makeSecondaryModel(sign),
                buffer.getBuffer(RenderType.entitySolid(sign.getSecondaryTexture().location())),
				combinedLights,
				combinedOverlay,
				colorSecondary
            );
			sign.getOverlay().ifPresent(o -> {
                RenderingUtil.render(
                    blockToView,
                    makeBakedOverlayModel(sign, o),
                    buffer.getBuffer(RenderType.entityCutout(o.textureFor(sign.getClass()))),
					combinedLights,
					combinedOverlay,
                    o.tint.map(t -> t.getColorAt(tileEntity.getLevel(), tileEntity.getBlockPos())).orElse(Colors.white)
                );
			});
		});
	}

	protected abstract void renderText(T sign, PoseStack matrix, Font fontRenderer, MultiBufferSource buffer, int combinedLights);

	@Override
	public void renderGui(T sign, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset, MultiBufferSource buffer) {
		if(sign.isMarkedForGeneration() && !IConfig.IServer.getInstance().worldGen().debugMode()) return;
		RenderingUtil.renderGui(
			makeMainModel(sign),
			matrixStack,
			sign.getMainTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white),
			center,
			yaw.add(sign.getAngle().get()),
			pitch,
			isFlipped,
			scale,
			offset,
			buffer.getBuffer(RenderType.guiTextured(sign.getMainTexture().location())),
			ms -> RenderingUtil.wrapInMatrixEntry(ms, () -> {
				if(!sign.isFlipped())
					ms.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
				renderText(sign, ms, Minecraft.getInstance().font, Minecraft.getInstance().renderBuffers().bufferSource(), 0xf000f0);
			})
		);
		RenderingUtil.renderGui(
			makeSecondaryModel(sign),
			matrixStack,
			sign.getSecondaryTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white),
			center,
			yaw.add(sign.getAngle().get()),
			pitch,
			isFlipped,
			scale,
			offset,
			buffer.getBuffer(RenderType.guiTextured(sign.getSecondaryTexture().location())),
			ms -> RenderingUtil.wrapInMatrixEntry(ms, () -> {
				if(!sign.isFlipped())
					ms.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
				renderText(sign, ms, Minecraft.getInstance().font, Minecraft.getInstance().renderBuffers().bufferSource(), 0xf000f0);
			})
		);
		sign.getOverlay().ifPresent(o ->
			RenderingUtil.renderGui(
				makeBakedOverlayModel(sign, o),
				matrixStack,
				o.tint.map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white),
				center, yaw.add(sign.getAngle().get()), pitch, isFlipped, scale, offset,
				buffer.getBuffer(RenderType.guiTextured(o.textureFor(sign.getClass()))), m -> {}));
	}

	@Override
	public void renderGui(T sign, PoseStack matrixStack, Vector3 offset, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
		if(sign.isMarkedForGeneration() && !IConfig.IServer.getInstance().worldGen().debugMode()) return;
		RenderingUtil.renderGui(makeMainModel(sign), matrixStack, offset, sign.getAngle().get(),
			buffer.getBuffer(RenderType.guiTextured(sign.getMainTexture().location())),
			combinedLight, combinedOverlay,
			sign.getMainTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white),
			ms -> RenderingUtil.wrapInMatrixEntry(matrixStack, () -> {
				if(!sign.isFlipped())
					matrixStack.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
				renderText(sign, ms, Minecraft.getInstance().font, buffer, combinedLight);
			})
		);
		RenderingUtil.renderGui(makeSecondaryModel(sign), matrixStack, offset, sign.getAngle().get(),
			buffer.getBuffer(RenderType.guiTextured(sign.getSecondaryTexture().location())),
			combinedLight, combinedOverlay,
			sign.getSecondaryTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white),
			ms -> RenderingUtil.wrapInMatrixEntry(matrixStack, () -> {
				if(!sign.isFlipped())
					matrixStack.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
				renderText(sign, ms, Minecraft.getInstance().font, buffer, combinedLight);
			})
		);
		sign.getOverlay().ifPresent(o -> {
			RenderingUtil.renderGui(makeBakedOverlayModel(sign, o), matrixStack,
				offset, sign.getAngle().get(),
				buffer.getBuffer(RenderType.guiTextured(o.textureFor(sign.getClass()))),
				combinedLight, combinedOverlay,
				o.tint.map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white),
				m -> {});
		});
	}

}
