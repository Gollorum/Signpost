package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix4f;

public record Text3dElementRenderState(Matrix4f pose, TextRenderable renderable, int packedLight, ScreenRectangle bounds) implements GuiElementRenderState {
    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        this.renderable.render(pose, vertexConsumer, packedLight, true);
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.TEXT;
//        return this.renderable.guiPipeline();
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.singleTextureWithLightmap(this.renderable.textureView(), RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST));
    }

    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }

    @Override
    public ScreenRectangle scissorArea() {
        return null;
    }
}
