package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.SimpleModelTransform;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.common.util.Lazy;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RenderingUtil {

    public static IBakedModel loadModel(ResourceLocation location) {
        return ModelLoader.instance().getBakedModel(
            location,
            new SimpleModelTransform(TransformationMatrix.identity()),
            Material::sprite
        );
    }

    public static IBakedModel loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation) {
        final ResourceLocation textLoc = trim(textureLocation);
        return ModelLoader.instance().getModel(modelLocation).bake(
            ModelLoader.instance(),
            m -> Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(textLoc),
            new SimpleModelTransform(TransformationMatrix.identity()),
            modelLocation
        );
    }

    public static IBakedModel loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation1, ResourceLocation textureLocation2) {
        final ResourceLocation textLoc1 = trim(textureLocation1);
        final ResourceLocation textLoc2 = trim(textureLocation2);
        return ModelLoader.instance().getModel(modelLocation).bake(
            ModelLoader.instance(),
            m -> Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(
                m.texture().equals(PostModel.mainTextureMarker)
                    ? textLoc1 : textLoc2
            ),
            new SimpleModelTransform(TransformationMatrix.identity()),
            modelLocation
        );
    }

    private static final Lazy<BlockModelRenderer> Renderer = Lazy.of(() -> Minecraft.getInstance().getBlockRenderer().getModelRenderer());
    private static final Lazy<Tessellator> Tesselator = Lazy.of(Tessellator::getInstance);
    private static final Lazy<BufferBuilder> BufferBuilder = Lazy.of(() -> Tesselator.get().getBuilder());

    public static interface RenderModel {
        void render(
            IBakedModel model,
            World world,
            BlockState state,
            BlockPos pos,
            IVertexBuilder buffer,
            boolean checkSides,
            Random random,
            long rand,
            int combinedOverlay,
            Matrix4f rotationMatrix
        );
    }

    public static ResourceLocation trim(ResourceLocation textureLocation){
        if(textureLocation.getPath().startsWith("textures/"))
            textureLocation = new ResourceLocation(textureLocation.getNamespace(), textureLocation.getPath().substring("textures/".length()));
        if(textureLocation.getPath().endsWith(".png"))
            textureLocation = new ResourceLocation(textureLocation.getNamespace(), textureLocation.getPath().substring(0, textureLocation.getPath().length() - ".png".length()));
        return textureLocation;
    }

    public static void render(MatrixStack matrix, Consumer<RenderModel> inner){
        matrix.pushPose();
        inner.accept((model, world, state, pos, buffer, checkSides, random, rand, combinedOverlay, rotationMatrix) -> {
            Renderer.get().tesselateBlock(
                world,
                model,
                state,
                pos,
                matrix,
                buffer,
                checkSides,
                random,
                rand,
                combinedOverlay
            );
        });
        matrix.popPose();
    }

    public static int drawString(FontRenderer fontRenderer, String text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, int color, int maxWidth, boolean dropShadow){
        IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());
        int textWidth = fontRenderer.width(text);
        float scale = Math.min(1f, maxWidth / (float) textWidth);
        Matrix4f matrix = Matrix4f.createTranslateMatrix(
            Rect.xCoordinateFor(point.x, maxWidth, xAlignment) + maxWidth * 0.5f,
            Rect.yCoordinateFor(point.y, fontRenderer.lineHeight, yAlignment) + fontRenderer.lineHeight * 0.5f,
            100
        );
        if(scale < 1) matrix.multiply(Matrix4f.createScaleMatrix(scale, scale, scale));
        int i = fontRenderer.drawInBatch(
            text,
            (maxWidth - Math.min(maxWidth, textWidth)) * 0.5f,
            -fontRenderer.lineHeight * 0.5f,
            color,
            dropShadow,
            matrix,
            buffer,
            false,
            0,
            0xf000f0
        );
        buffer.endBatch();
        return i;
    }

    public static void renderGui(IBakedModel model, MatrixStack matrixStack, int color, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset, RenderType renderType, Consumer<MatrixStack> alsoDo) {
        wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.translate(center.x, center.y, 0);
            matrixStack.scale(scale, -scale, scale);
            matrixStack.mulPose(new Quaternion(Vector3f.XP, pitch.radians(), false));
            if(isFlipped) matrixStack.mulPose(new Quaternion(Vector3f.YP, (float) Math.PI, false));
            IRenderTypeBuffer.Impl renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
            renderGui(model, matrixStack, color, offset, yaw, renderTypeBuffer.getBuffer(renderType), 0xf000f0, OverlayTexture.NO_OVERLAY, alsoDo);
            renderTypeBuffer.endBatch();
        });
    }

    public static void renderGui(IBakedModel model, MatrixStack matrixStack, int color, Vector3 offset, Angle yaw, IVertexBuilder builder, int combinedLight, int combinedOverlay, Consumer<MatrixStack> alsoDo) {
        wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.mulPose(new Quaternion(Vector3f.YP, yaw.radians(), false));
            matrixStack.translate(offset.x, offset.y, offset.z);
            wrapInMatrixEntry(matrixStack, () -> {

                List<Direction> allDirections = new ArrayList<>(Arrays.asList(Direction.values()));
                allDirections.add(null);

                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                Minecraft.getInstance().getTextureManager().bind(AtlasTexture.LOCATION_BLOCKS);
                Minecraft.getInstance().getTextureManager().getTexture(AtlasTexture.LOCATION_BLOCKS).setBlurMipmap(false, false);
                float r = Colors.getRed(color) / 255f;
                float g = Colors.getGreen(color) / 255f;
                float b = Colors.getBlue(color) / 255f;
                Random random = new Random();
                for(Direction dir : allDirections) {
                    random.setSeed(42L);
                    for(BakedQuad quad: model.getQuads(null, dir, random, EmptyModelData.INSTANCE)) {
                        builder.putBulkData(matrixStack.last(), quad, r, g, b, combinedLight, combinedOverlay);
                    }

                }
            });

            alsoDo.accept(matrixStack);
        });
    }

    public static void wrapInMatrixEntry(MatrixStack matrixStack, Runnable thenDo) {
        matrixStack.pushPose();
        thenDo.run();
        matrixStack.popPose();
    }

    public static IBakedModel withTintIndex(IBakedModel original, int tintIndex) {
        return new IBakedModel() {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
                return original.getQuads(state, side, rand)
                       .stream().map(q -> new BakedQuad(q.getVertices(), tintIndex, q.getDirection(), q.getSprite(), q.shouldApplyDiffuseLighting()))
                       .collect(Collectors.toList());
            }

            @Override
            public boolean useAmbientOcclusion() {
                return original.useAmbientOcclusion();
            }

            @Override
            public boolean isGui3d() {
                return original.isGui3d();
            }

            @Override
            public boolean usesBlockLight() {
                return original.usesBlockLight();
            }

            @Override
            public boolean isCustomRenderer() {
                return original.isCustomRenderer();
            }

            @Override
            public TextureAtlasSprite getParticleIcon() {
                return original.getParticleIcon();
            }

            @Override
            public ItemOverrideList getOverrides() {
                return original.getOverrides();
            }
        };
    }

}
