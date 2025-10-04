package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.models.modelGeneration.QuadModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.minecraft.rendering.TexturedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.*;

import java.lang.Math;

public abstract class SignRenderer<T extends SignBlockPart<T>> extends BlockPartRenderer<T> {

    protected abstract QuadModel makeMainModel(T sign);
    protected abstract QuadModel makeSecondaryModel(T sign);
    protected abstract QuadModel makeBakedOverlayModel(T sign, Overlay overlay);

    @Override
    public void render(
        T sign,
        Level level,
        BlockPos pos,
        PoseStack blockToView,
        MultiBufferSource buffer,
        int combinedLights,
        int combinedOverlay,
        boolean dropNormalsForGui
    ) {
        if(sign.isMarkedForGeneration() && !IConfig.IServer.getInstance().worldGen().debugMode()) return;
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
                    sign.getMainTexture().tint().map(t -> t.getColorAt(level, pos)).orElse(Colors.white)
                ),
                buffer,
                combinedLights,
                combinedOverlay,
                RenderType::entitySolid,
                dropNormalsForGui
            );
            RenderingUtil.render(
                blockToView,
                new TexturedModel(
                    makeSecondaryModel(sign),
                    sign.getSecondaryTexture().toMaterial(),
                    sign.getSecondaryTexture().tint().map(t -> t.getColorAt(level, pos)).orElse(Colors.white)
                ),
                buffer,
                combinedLights,
                combinedOverlay,
                RenderType::entitySolid,
                dropNormalsForGui
            );
            sign.getOverlay().ifPresent(o -> {
                RenderingUtil.render(
                    blockToView,
                    new TexturedModel(
                        makeBakedOverlayModel(sign, o),
                        o.materialFor(sign.getClass()),
                        o.tint.map(t -> t.getColorAt(level, pos)).orElse(Colors.white)
                    ),
                    buffer,
                    combinedLights,
                    combinedOverlay,
                    RenderType::entityCutout,
                    dropNormalsForGui
                );
            });
        });
    }

    protected abstract void renderText(T sign, PoseStack matrix, Font fontRenderer, MultiBufferSource buffer, int combinedLights);

}