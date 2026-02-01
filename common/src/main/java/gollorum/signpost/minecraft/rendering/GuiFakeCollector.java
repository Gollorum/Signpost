package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;

public class GuiFakeCollector implements SubmitNodeCollector {

    private final ScreenRectangle rect;
    private final GuiRenderState renderState;
    private final Font font;

    public GuiFakeCollector(ScreenRectangle rect, GuiRenderState renderState, Font font) {
        this.rect = rect;
        this.renderState = renderState;
        this.font = font;
    }

    @Override
    public OrderedSubmitNodeCollector order(int i) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void submitShadow(PoseStack poseStack, float v, List<EntityRenderState.ShadowPiece> list) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 vec3, int i, Component component, boolean b, int i1, double v, CameraRenderState cameraRenderState) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean dropShadow, Font.DisplayMode displayMode, int packedLight, int color, int backgroundColor, int outlineColor) {
        var pose = poseStack.last().copy().pose();
        Font.PreparedText preparedText = font.prepareText(text, x, y, color, dropShadow, true, backgroundColor);
        preparedText.visit(new Font.GlyphVisitor() {
            @Override
            public void acceptGlyph(TextRenderable.Styled renderable) {
                this.accept(renderable);
            }

            @Override
            public void acceptEffect(TextRenderable renderable) {
                this.accept(renderable);
            }

            private void accept(TextRenderable renderable) {
                renderState.submitGuiElement(new Text3dElementRenderState(pose, renderable, packedLight, rect));
            }
        });
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S s, PoseStack poseStack, RenderType renderType, int i, int i1, int i2, @Nullable TextureAtlasSprite textureAtlasSprite, int i3, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int i, int i1, @Nullable TextureAtlasSprite textureAtlasSprite, boolean b, boolean b1, int i2, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int i3) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void submitBlock(PoseStack poseStack, BlockState blockState, int i, int i1, int i2) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, BlockStateModel blockStateModel, float v, float v1, float v2, int i, int i1, int i2) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext itemDisplayContext, int i, int i1, int i2, int[] ints, List<BakedQuad> list, RenderType renderType, ItemStackRenderState.FoilType foilType) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
        submitCustomGeometry(poseStack, renderType, customGeometryRenderer, TextureResource.blockAtlas);
    }

    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer, Identifier atlasLocation) {
        var pose = poseStack.last().copy();
        renderState.submitGuiElement(new ModelElementRenderState(rect, atlasLocation, renderType.pipeline(), vertexConsumer ->
            customGeometryRenderer.render(
                pose,
                vertexConsumer
            )));
    }

    @Override
    public void submitParticleGroup(ParticleGroupRenderer particleGroupRenderer) {
        throw new RuntimeException("Not supported");
    }
}
