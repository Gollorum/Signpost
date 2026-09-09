package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ModelElementRenderState implements GuiElementRenderState {

    private final ScreenRectangle rect;
    private final Consumer<VertexConsumer> render;
    private final Identifier atlasLocation;
    private final RenderPipeline renderPipeline;

    public ModelElementRenderState(Rect rect, Identifier atlasLocation, RenderPipeline renderPipeline, Consumer<VertexConsumer> render) {
        this.rect = new ScreenRectangle(rect.min().x, rect.min().y, rect.width, rect.height);
        this.render = render;
        this.atlasLocation = atlasLocation;
        this.renderPipeline = renderPipeline;
    }

    public ModelElementRenderState(ScreenRectangle rect, Identifier atlasLocation, RenderPipeline renderPipeline, Consumer<VertexConsumer> render) {
        this.rect = rect;
        this.render = render;
        this.atlasLocation = atlasLocation;
        this.renderPipeline = renderPipeline;
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        render.accept(vertexConsumer);
    }

    @Override
    public RenderPipeline pipeline() {
        return renderPipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        Minecraft minecraft = Minecraft.getInstance();
        TextureManager texturemanager = minecraft.getTextureManager();
        GpuTextureView gputextureview = texturemanager.getTexture(atlasLocation).getTextureView();
        return TextureSetup.singleTexture(gputextureview, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST));
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return null;
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return rect;
    }
}
