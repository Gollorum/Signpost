package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.common.util.Lazy;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.minecraft.client.renderer.LevelRenderer.DIRECTIONS;

public class RenderingUtil {

    public static BakedModel loadModel(ResourceLocation location) {
        return ForgeModelBakery.instance().bake(
            location,
            new SimpleModelState(Transformation.identity()),
            Material::sprite
        );
    }

    public static BakedModel loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation) {
        final ResourceLocation textLoc = trim(textureLocation);
        return ForgeModelBakery.instance().getModel(modelLocation).bake(
            ForgeModelBakery.instance(),
            m -> Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(textLoc),
            new SimpleModelState(Transformation.identity()),
            modelLocation
        );
    }

    public static BakedModel loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation1, ResourceLocation textureLocation2) {
        final ResourceLocation textLoc1 = trim(textureLocation1);
        final ResourceLocation textLoc2 = trim(textureLocation2);
        return ForgeModelBakery.instance().getModel(modelLocation).bake(
            ForgeModelBakery.instance(),
            m -> Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(
                m.texture().equals(PostModel.mainTextureMarker)
                    ? textLoc1 : textLoc2
            ),
            new SimpleModelState(Transformation.identity()),
            modelLocation
        );
    }

    public static final Lazy<ModelBlockRenderer> Renderer = Lazy.of(() -> Minecraft.getInstance().getBlockRenderer().getModelRenderer());

    public static interface RenderModel {
        void render(
            BakedModel model,
            Level world,
            BlockState state,
            BlockPos pos,
            VertexConsumer buffer,
            boolean checkSides,
            Random random,
            long rand,
            int combinedOverlay,
            Matrix4f rotationMatrix,
            int[] tints
        );
    }

    public static ResourceLocation trim(ResourceLocation textureLocation){
        if(textureLocation.getPath().startsWith("textures/"))
            textureLocation = new ResourceLocation(textureLocation.getNamespace(), textureLocation.getPath().substring("textures/".length()));
        if(textureLocation.getPath().endsWith(".png"))
            textureLocation = new ResourceLocation(textureLocation.getNamespace(), textureLocation.getPath().substring(0, textureLocation.getPath().length() - ".png".length()));
        return textureLocation;
    }

    public static void render(PoseStack matrix, Consumer<RenderModel> inner){
        matrix.pushPose();
        inner.accept((model, world, state, pos, buffer, checkSides, random, rand, combinedOverlay, rotationMatrix, tints) -> {
            tesselateBlock(
                world,
                model,
                state,
                tints,
                pos,
                matrix,
                rotationMatrix,
                buffer,
                checkSides,
                random,
                rand,
                combinedOverlay,
                EmptyModelData.INSTANCE
            );
        });
        matrix.popPose();
    }

    public static int drawString(Font fontRenderer, String text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, int color, int maxWidth, boolean dropShadow){
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
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

    public static void renderGui(BakedModel model, PoseStack matrixStack, int[] tints, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset, RenderType renderType, Consumer<PoseStack> alsoDo) {
        wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.translate(center.x, center.y, 0);
            matrixStack.scale(scale, -scale, scale);
            matrixStack.mulPose(new Quaternion(Vector3f.XP, pitch.radians(), false));
            if(isFlipped) matrixStack.mulPose(new Quaternion(Vector3f.YP, (float) Math.PI, false));
            MultiBufferSource.BufferSource renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
            renderGui(model, matrixStack, tints, offset, yaw, renderTypeBuffer.getBuffer(renderType), 0xf000f0, OverlayTexture.NO_OVERLAY, alsoDo);
            renderTypeBuffer.endBatch();
        });
    }

    public static void renderGui(BakedModel model, PoseStack matrixStack, int[] tints, Vector3 offset, Angle yaw, VertexConsumer builder, int combinedLight, int combinedOverlay, Consumer<PoseStack> alsoDo) {
        wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.mulPose(new Quaternion(Vector3f.YP, yaw.radians(), false));
            matrixStack.translate(offset.x, offset.y, offset.z);
            wrapInMatrixEntry(matrixStack, () -> {

                List<Direction> allDirections = new ArrayList<>(Arrays.asList(Direction.values()));
                allDirections.add(null);

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
                Minecraft.getInstance().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).setFilter(false, false);
                Random random = new Random();
                for(Direction dir : allDirections) {
                    random.setSeed(42L);
                    for(BakedQuad quad: model.getQuads(null, dir, random, EmptyModelData.INSTANCE)) {
                        float r = 1;
                        float g = 1;
                        float b = 1;
                        if(quad.isTinted()) {
                            var tint = tints[quad.getTintIndex()];
                            r *= Colors.getRed(tint) / 255f;
                            g *= Colors.getGreen(tint) / 255f;
                            b *= Colors.getBlue(tint) / 255f;
                        }
                        builder.putBulkData(matrixStack.last(), quad, r, g, b, combinedLight, combinedOverlay);
                    }

                }
            });

            alsoDo.accept(matrixStack);
        });
    }

    public static void wrapInMatrixEntry(PoseStack matrixStack, Runnable thenDo) {
        matrixStack.pushPose();
        thenDo.run();
        matrixStack.popPose();
    }

    // These are modified copies of stuff in the ModelBlockRenderer to allow custom tints.
    private static boolean tesselateBlock(
        BlockAndTintGetter level,
        BakedModel model,
        BlockState state,
        int[] tints,
        BlockPos pos,
        PoseStack pose,
        Matrix4f rotationMatrix,
        VertexConsumer vertexConsumer,
        boolean checkSides,
        Random random,
        long combinedLight,
        int combinedOverlay,
        net.minecraftforge.client.model.data.IModelData modelData
    ) {
        boolean useAmbientOcclusion = Minecraft.useAmbientOcclusion() && state.getLightEmission(level, pos) == 0 && model.useAmbientOcclusion();
        Vec3 vec3 = state.getOffset(level, pos);
        pose.translate(vec3.x, vec3.y, vec3.z);
        modelData = model.getModelData(level, pos, state, modelData);
        try {
            return tesselate(level, model, state, tints, pos, pose, rotationMatrix, vertexConsumer, checkSides, random, combinedLight, combinedOverlay, modelData, useAmbientOcclusion);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block model");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block model being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, level, pos, state);
            crashreportcategory.setDetail("Using AO", useAmbientOcclusion);
            throw new ReportedException(crashreport);
        }
    }

    public static boolean tesselate(
        BlockAndTintGetter level,
        BakedModel model,
        BlockState state,
        int[] tints,
        BlockPos pos,
        PoseStack pose,
        Matrix4f rotationMatrix,
        VertexConsumer vertexConsumer,
        boolean checkSides,
        Random random,
        long combinedLight,
        int combinedOverlay,
        net.minecraftforge.client.model.data.IModelData modelData,
        boolean useAmbientOcclusion) {
        boolean flag = false;
        float[] aoValues = useAmbientOcclusion ? new float[DIRECTIONS.length * 2] : null;
        BitSet bitset = new BitSet(3);
        ModelBlockRenderer.AmbientOcclusionFace aoFace = useAmbientOcclusion ? Renderer.get().new AmbientOcclusionFace() : null;
        BlockPos.MutableBlockPos mutablePos = pos.mutable();

        for(Direction direction : DIRECTIONS) {
            random.setSeed(combinedLight);
            List<BakedQuad> list = model.getQuads(state, direction, random, modelData);
            if (!list.isEmpty()) {
                mutablePos.setWithOffset(pos, direction);
                if (!checkSides || Block.shouldRenderFace(state, level, pos, direction, mutablePos)) {
                    if(useAmbientOcclusion)
                        renderModelFaceAO(level, state, tints, pos, pose, rotationMatrix, vertexConsumer, list, aoValues, bitset, aoFace, combinedOverlay);
                    else
                        renderModelFaceFlat(level, state, tints, pos, LevelRenderer.getLightColor(level, state, mutablePos), combinedOverlay, false, pose, rotationMatrix, vertexConsumer, list, bitset);
                    flag = true;
                }
            }
        }

        random.setSeed(combinedLight);
        List<BakedQuad> quads = model.getQuads(state, null, random, modelData);
        if (!quads.isEmpty()) {
            if(useAmbientOcclusion)
                renderModelFaceAO(level, state, tints, pos, pose, rotationMatrix, vertexConsumer, quads, aoValues, bitset, aoFace, combinedOverlay);
            else
                renderModelFaceFlat(level, state, tints, pos, -1, combinedOverlay, true, pose, rotationMatrix, vertexConsumer, quads, bitset);
            flag = true;
        }

        return flag;
    }

    private static void renderModelFaceAO(BlockAndTintGetter level, BlockState state, int[] tints, BlockPos pos, PoseStack pose, Matrix4f rotationMatrix, VertexConsumer vertexConsumer, List<BakedQuad> quads, float[] aoFloats, BitSet bitset, ModelBlockRenderer.AmbientOcclusionFace aoFace, int combinedOverlay) {
        var poseMatrix = pose.last();
        for(BakedQuad bakedquad : quads) {
            var dir = transform(bakedquad.getDirection(), rotationMatrix);
            Renderer.get().calculateShape(level, state, pos, bakedquad.getVertices(), dir, aoFloats, bitset);
            aoFace.calculate(level, state, pos, dir, aoFloats, bitset, bakedquad.isShade());
            putQuadData(tints, vertexConsumer, poseMatrix, bakedquad, aoFace.brightness[0], aoFace.brightness[1], aoFace.brightness[2], aoFace.brightness[3], aoFace.lightmap[0], aoFace.lightmap[1], aoFace.lightmap[2], aoFace.lightmap[3], combinedOverlay);
        }

    }

    private static void renderModelFaceFlat(BlockAndTintGetter level, BlockState state, int[] tints, BlockPos pos, int lightColor, int combinedOverlay, boolean p_111007_, PoseStack pose, Matrix4f rotationMatrix, VertexConsumer vertexConsumer, List<BakedQuad> quads, BitSet bitSet) {
        var poseMatrix = pose.last();
        for(BakedQuad bakedquad : quads) {
            var dir = transform(bakedquad.getDirection(), rotationMatrix);
            if (p_111007_) {
                Renderer.get().calculateShape(level, state, pos, bakedquad.getVertices(), dir, (float[])null, bitSet);
                BlockPos blockpos = bitSet.get(0) ? pos.relative(dir) : pos;
                lightColor = LevelRenderer.getLightColor(level, state, blockpos);
            }

            float f = level.getShade(dir, bakedquad.isShade());
            putQuadData(tints, vertexConsumer, poseMatrix, bakedquad, f, f, f, f, lightColor, lightColor, lightColor, lightColor, combinedOverlay);
        }
    }

    private static Direction transform(Direction dir, Matrix4f localPose) {
        var rawNormal = dir.getNormal();
        var normal = new com.mojang.math.Vector4f(rawNormal.getX(), rawNormal.getY(), rawNormal.getZ(), 0);
        normal.transform(localPose);
        return Direction.getNearest(normal.x(), normal.y(), normal.z());
    }

    private static void putQuadData(int[] tints, VertexConsumer vertexConsumer, PoseStack.Pose pose, BakedQuad quad, float aor, float aog, float aob, float aoa, int lr, int lg, int lb, int la, int combinedOverlay) {
        float r;
        float g;
        float b;
        if (quad.isTinted()) {
            int i = tints[quad.getTintIndex()];
            r = (float)(i >> 16 & 255) / 255.0F;
            g = (float)(i >> 8 & 255) / 255.0F;
            b = (float)(i & 255) / 255.0F;
        } else {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
        }

        vertexConsumer.putBulkData(pose, quad, new float[]{aor, aog, aob, aoa}, r, g, b, new int[]{lr, lg, lb, la}, combinedOverlay, true);
    }

}
