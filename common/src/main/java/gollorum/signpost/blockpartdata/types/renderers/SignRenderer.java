package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.models.modelGeneration.QuadModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.minecraft.rendering.TexturedModel;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.*;

import java.lang.Math;

public abstract class SignRenderer<T extends SignBlockPart<T>> extends BlockPartRenderer<T> {

    protected abstract QuadModel makeMainModel(T sign);
    protected abstract QuadModel makeSecondaryModel(T sign);
    protected abstract QuadModel makeBakedOverlayModel(T sign, Overlay overlay);

    @Override
    public void render(
        T sign,
        BlockEntity tileEntity,
        PoseStack blockToView,
        MultiBufferSource buffer,
        int combinedLights,
        int combinedOverlay
    ) {
        if(sign.isMarkedForGeneration() && !IConfig.IServer.getInstance().worldGen().debugMode()) return;
        if(!tileEntity.hasLevel()) throw new RuntimeException("TileEntity without world cannot be rendered.");
        RenderingUtil.wrapInMatrixEntry(blockToView, () -> {
            Quaternionf rotation = new Quaternionf(new AxisAngle4f(sign.getAngle().get().radians(), new Vector3f(0,1,0)));
            blockToView.mulPose(rotation);
            RenderingUtil.wrapInMatrixEntry(blockToView, () -> {
                blockToView.mulPose(new Quaternionf(new AxisAngle4f((float) Math.PI, sign.isFlipped() ? new Vector3f(0, 0, 1) : new Vector3f(1, 0, 0))));
                renderText(sign, blockToView, Minecraft.getInstance().font, buffer, combinedLights);
            });
            RenderingUtil.render(
                blockToView,
                new TexturedModel(
                    makeMainModel(sign),
                    sign.getMainTexture().toMaterial(),
                    sign.getMainTexture().tint().map(t -> t.getColorAt(tileEntity.getLevel(), tileEntity.getBlockPos())).orElse(Colors.white)
                ),
                buffer,
                combinedLights,
                combinedOverlay,
                RenderType::entitySolid
            );
            RenderingUtil.render(
                blockToView,
                new TexturedModel(
                    makeSecondaryModel(sign),
                    sign.getSecondaryTexture().toMaterial(),
                    sign.getSecondaryTexture().tint().map(t -> t.getColorAt(tileEntity.getLevel(), tileEntity.getBlockPos())).orElse(Colors.white)
                ),
                buffer,
                combinedLights,
                combinedOverlay,
                RenderType::entitySolid
            );
            sign.getOverlay().ifPresent(o -> {
                RenderingUtil.render(
                    blockToView,
                    new TexturedModel(
                        makeBakedOverlayModel(sign, o),
                        o.materialFor(sign.getClass()),
                        o.tint.map(t -> t.getColorAt(tileEntity.getLevel(), tileEntity.getBlockPos())).orElse(Colors.white)
                    ),
                    buffer,
                    combinedLights,
                    combinedOverlay,
                    RenderType::entityCutout
                );
            });
        });
    }

    protected abstract void renderText(T sign, PoseStack matrix, Font fontRenderer, MultiBufferSource buffer, int combinedLights);

    @Override
    public void renderGui(T sign, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset, MultiBufferSource buffer) {
        if(sign.isMarkedForGeneration() && !IConfig.IServer.getInstance().worldGen().debugMode()) return;
        var level = Minecraft.getInstance().level;
        var blockPos = Minecraft.getInstance().player.blockPosition();
        RenderingUtil.renderGui(
            new TexturedModel(
                makeMainModel(sign),
                sign.getMainTexture().toMaterial(),
                sign.getMainTexture().tint().map(t -> t.getColorAt(level, blockPos)).orElse(Colors.white)
            ),
            matrixStack,
            center,
            yaw.add(sign.getAngle().get()),
            pitch,
            isFlipped,
            scale,
            offset,
            buffer,
            ms -> RenderingUtil.wrapInMatrixEntry(ms, () -> {
                if(!sign.isFlipped())
                    ms.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
                renderText(sign, ms, Minecraft.getInstance().font, Minecraft.getInstance().renderBuffers().bufferSource(), 0xf000f0);
            })
        );
        RenderingUtil.renderGui(
            new TexturedModel(
                makeSecondaryModel(sign),
                sign.getSecondaryTexture().toMaterial(),
                sign.getSecondaryTexture().tint().map(t -> t.getColorAt(level, blockPos)).orElse(Colors.white)
            ),
            matrixStack,
            center,
            yaw.add(sign.getAngle().get()),
            pitch,
            isFlipped,
            scale,
            offset,
            buffer,
            ms -> RenderingUtil.wrapInMatrixEntry(ms, () -> {
                if(!sign.isFlipped())
                    ms.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
                renderText(sign, ms, Minecraft.getInstance().font, Minecraft.getInstance().renderBuffers().bufferSource(), 0xf000f0);
            })
        );
        sign.getOverlay().ifPresent(o ->
            RenderingUtil.renderGui(
                new TexturedModel(
                    makeBakedOverlayModel(sign, o),
                    o.materialFor(sign.getClass()),
                    o.tint.map(t -> t.getColorAt(level, blockPos)).orElse(Colors.white)
                ),
                matrixStack,
                center, yaw.add(sign.getAngle().get()), pitch, isFlipped, scale, offset,
                buffer, m -> {}));
    }

    @Override
    public void renderGui(T sign, PoseStack matrixStack, Vector3 offset, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if(sign.isMarkedForGeneration() && !IConfig.IServer.getInstance().worldGen().debugMode()) return;
        var level = Minecraft.getInstance().level;
        var blockPos = Minecraft.getInstance().player.blockPosition();
        RenderingUtil.renderGui(
            new TexturedModel(
                makeMainModel(sign),
                sign.getMainTexture().toMaterial(),
                sign.getMainTexture().tint().map(t -> t.getColorAt(level, blockPos)).orElse(Colors.white)
            ),
            matrixStack, offset, sign.getAngle().get(),
            buffer,
            combinedLight, combinedOverlay,
            ms -> RenderingUtil.wrapInMatrixEntry(matrixStack, () -> {
                if(!sign.isFlipped())
                    matrixStack.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
                renderText(sign, ms, Minecraft.getInstance().font, buffer, combinedLight);
            })
        );
        RenderingUtil.renderGui(
            new TexturedModel(
                makeSecondaryModel(sign),
                sign.getSecondaryTexture().toMaterial(),
                sign.getSecondaryTexture().tint().map(t -> t.getColorAt(level, blockPos)).orElse(Colors.white)
            ),
            matrixStack, offset, sign.getAngle().get(),
            buffer,
            combinedLight, combinedOverlay,
            ms -> RenderingUtil.wrapInMatrixEntry(matrixStack, () -> {
                if(!sign.isFlipped())
                    matrixStack.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
                renderText(sign, ms, Minecraft.getInstance().font, buffer, combinedLight);
            })
        );
        sign.getOverlay().ifPresent(o -> {
            RenderingUtil.renderGui(
                new TexturedModel(
                    makeBakedOverlayModel(sign, o),
                    o.materialFor(sign.getClass()),
                    o.tint.map(t -> t.getColorAt(level, blockPos)).orElse(Colors.white)
                ),
                matrixStack,
                offset, sign.getAngle().get(),
                buffer,
                combinedLight, combinedOverlay,
                m -> {});
        });
    }

}