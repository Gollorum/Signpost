package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.FormattedCharSequence;
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
        return TextureSetup.singleTextureWithLightmap(this.renderable.textureView());
    }

    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }

    @Override
    public ScreenRectangle scissorArea() {
        return null;
    }

    public static record ButDifferent(Matrix4f pose, float x, float y, FormattedCharSequence text, boolean dropShadow, Font.DisplayMode displayMode, int packedLight, int color, int backgroundColor, int outlineColor, ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            var font = Minecraft.getInstance().font;
            font.drawInBatch(
                text,
                x, y,
                color,
                dropShadow,
                pose,
                Minecraft.getInstance().renderBuffers().bufferSource(),
                displayMode,
                backgroundColor,
                packedLight
            );
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXT;
        }

        @Override
        public TextureSetup textureSetup() {
            Minecraft minecraft = Minecraft.getInstance();
            TextureManager texturemanager = minecraft.getTextureManager();
            GpuTextureView gputextureview = texturemanager.getTexture(Sheets.GUI_SHEET).getTextureView();
            return TextureSetup.singleTextureWithLightmap(gputextureview);
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
}