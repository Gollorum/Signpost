package gollorum.signpost.minecraft.rendering;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    // The pipeline comes from a world RenderType, whose shader may also sample the overlay (Sampler1) and the
    // lightmap (Sampler2). Since 26.3 the render pass rejects a draw that leaves a declared sampler unbound,
    // so bind exactly what the pipeline declares, the same textures RenderSetup binds in the world.
    @Override
    public TextureSetup textureSetup() {
        Minecraft minecraft = Minecraft.getInstance();
        TextureManager texturemanager = minecraft.getTextureManager();
        GpuTextureView atlas = texturemanager.getTexture(atlasLocation).getTextureView();
        Set<String> samplers = BindGroupLayout.flattenUniforms(renderPipeline.getBindGroupLayouts()).stream()
            .map(BindGroupLayout.UniformDescription::name)
            .collect(Collectors.toSet());
        var samplerCache = RenderSystem.getSamplerCache();
        boolean overlay = samplers.contains("Sampler1");
        boolean lightmap = samplers.contains("Sampler2");
        return new TextureSetup(
            atlas,
            overlay ? minecraft.gameRenderer.overlayTexture().getTextureView() : null,
            lightmap ? minecraft.gameRenderer.lightmap() : null,
            samplerCache.getRepeat(FilterMode.NEAREST),
            overlay ? samplerCache.getClampToEdge(FilterMode.LINEAR) : null,
            lightmap ? samplerCache.getClampToEdge(FilterMode.LINEAR) : null
        );
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
