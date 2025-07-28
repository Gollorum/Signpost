package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.vertex.*;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.utils.Lazy;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.*;

import java.lang.Math;
import java.util.function.Consumer;

public class RenderingUtil {

    public static ModelPart EMPTY_MODEL;
    public static PartDefinition EMPTY_PART;
    static {

        var meshDefinition = new MeshDefinition();
        var EMPTY_PART = meshDefinition.getRoot();
        EMPTY_PART.addOrReplaceChild(
            "waystone",
            CubeListBuilder.create(),
            PartPose.ZERO
        );
        EMPTY_MODEL = EMPTY_PART.bake(0,0);
    }

//    private record NoTexCacheKey(ResourceLocation modelLocation, ModelState modelState){}
//    private record SingleTexCacheKey(ResourceLocation modelLocation, ResourceLocation textureLocation, ModelState modelState){}
//    private record DoubleTexCacheKey(ResourceLocation modelLocation, ResourceLocation textureLocation1, ResourceLocation textureLocation2, ModelState modelState){}
//
//    private static final HashMap<NoTexCacheKey, BlockModelPart> noTexCache = new HashMap<>();
//    private static final HashMap<SingleTexCacheKey, BlockModelPart> singleTexCache = new HashMap<>();
//    private static final HashMap<DoubleTexCacheKey, BlockModelPart> doubleTexCache = new HashMap<>();
//
//    public static ModelState IdentityModelState = new ModelState() {};
//
//    public static BlockModelPart loadModel(ResourceLocation modelLocation, ModelState modelState) {
//        return noTexCache.computeIfAbsent(new NoTexCacheKey(modelLocation, modelState), key -> {
//            Function<Material, TextureAtlasSprite> textureGetter = m -> Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(m.texture());
//            return ClientServices.MODEL_FACTORY.bakeFor(textureGetter, key.modelLocation, modelState);
//        });
//    }
//
//    public static BlockModelPart loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation, ModelState modelState) {
//        final ResourceLocation textLoc = trim(textureLocation);
//        return singleTexCache.computeIfAbsent(new SingleTexCacheKey(modelLocation, textLoc, modelState), key -> {
//            Function<Material, TextureAtlasSprite> textureGetter = m -> Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(key.textureLocation);
//            return ClientServices.MODEL_FACTORY.bakeFor(textureGetter, key.modelLocation, modelState);
//        });
//    }
//
//    public static BlockModelPart loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation1, ResourceLocation textureLocation2, ModelState modelState) {
//        final ResourceLocation textLoc1 = trim(textureLocation1);
//        final ResourceLocation textLoc2 = trim(textureLocation2);
//        return doubleTexCache.computeIfAbsent(new DoubleTexCacheKey(modelLocation, textLoc1, textLoc2, modelState), key -> {
//            Function<Material, TextureAtlasSprite> textureGetter = m -> Minecraft.getInstance().getTextureAtlas(m.atlasLocation()).apply(
//                m.sprite().contents().name().equals(PostModelResources.mainTextureMarker)
//                    ? key.textureLocation1 : key.textureLocation2);
//            return ClientServices.MODEL_FACTORY.bakeFor(textureGetter, key.modelLocation, modelState);
//        });
//    }

    public static final Lazy<ModelBlockRenderer> Renderer = Lazy.of(() -> Minecraft.getInstance().getBlockRenderer().getModelRenderer());

    public static ResourceLocation trim(ResourceLocation textureLocation){
        if(textureLocation.getPath().startsWith("textures/"))
            textureLocation = ResourceLocation.fromNamespaceAndPath(textureLocation.getNamespace(), textureLocation.getPath().substring("textures/".length()));
        if(textureLocation.getPath().endsWith(".png"))
            textureLocation = ResourceLocation.fromNamespaceAndPath(textureLocation.getNamespace(), textureLocation.getPath().substring(0, textureLocation.getPath().length() - ".png".length()));
        return textureLocation;
    }

    public static void render(
        PoseStack blockToView,
        TexturedModel model,
        VertexConsumer buffer,
        int combinedLights,
        int combinedOverlay
    ){
//        wrapInMatrixEntry(blockToView, () ->
            model.model().render(blockToView, buffer, combinedLights, combinedOverlay, model.tint());
//            tesselateBlock(
//                world,
//                model,
//                state,
//                tints,
//                pos,
//                blockToView,
//                localToBlock,
//                buffer,
//                checkSides,
//                random,
//                rand,
//                combinedOverlay
//            )
//        );
    }

    public static void drawString(GuiGraphics graphics, Font fontRenderer, String text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, int color, int maxWidth, boolean dropShadow){
        graphics.drawSpecial(buffer -> {
            int textWidth = fontRenderer.width(text);
            float scale = Math.min(1f, maxWidth / (float) textWidth);
            Matrix4f matrix = new Matrix4f().translation(
                Rect.xCoordinateFor(point.x, maxWidth, xAlignment) + maxWidth * 0.5f,
                Rect.yCoordinateFor(point.y, fontRenderer.lineHeight, yAlignment) + fontRenderer.lineHeight * 0.5f,
                100
            );
            if(scale < 1) matrix.scale(scale, scale, scale);
            fontRenderer.drawInBatch(
                text,
                (maxWidth - Math.min(maxWidth, textWidth)) * 0.5f,
                -fontRenderer.lineHeight * 0.5f,
                color,
                dropShadow,
                matrix,
                buffer,
                Font.DisplayMode.NORMAL,
                0,
                0xf000f0
            );
        });
    }

    public static void renderGui(TexturedModel model, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset, MultiBufferSource buffer, Consumer<PoseStack> alsoDo) {
        wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.translate(center.x, center.y, 0);
            matrixStack.scale(scale, -scale, scale);
            matrixStack.mulPose(new Quaternionf(new AxisAngle4f(pitch.radians(), new Vector3f(1, 0, 0))));
            if(isFlipped) matrixStack.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
            renderGui(model, matrixStack, offset, yaw, buffer, FLAT_LIGHT_PROBABLY, OverlayTexture.NO_OVERLAY, alsoDo);
        });
    }

    public static final int FLAT_LIGHT_PROBABLY = 0xf000f0;

    public static void renderGui(TexturedModel model, PoseStack matrixStack, Vector3 offset, Angle yaw, MultiBufferSource buffer, int combinedLight, int combinedOverlay, Consumer<PoseStack> alsoDo) {
        wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.mulPose(new Quaternionf(new AxisAngle4f(yaw.radians(), new Vector3f(0, 1, 0))));
            matrixStack.translate(offset.x(), offset.y(), offset.z());

            render(matrixStack, model, buffer.getBuffer(RenderType.guiTextured(model.texture())), combinedLight, combinedOverlay);

            alsoDo.accept(matrixStack);
        });
    }

    public static void wrapInMatrixEntry(PoseStack matrixStack, Runnable thenDo) {
        matrixStack.pushPose();
        thenDo.run();
        matrixStack.popPose();
    }

    // These are modified copies of stuff in the ModelBlockRenderer to allow custom tints and dynamic ao.
//    private static void tesselateBlock(
//        BlockAndTintGetter level,
//        BlockModelPart model,
//        BlockState state,
//        int[] tints,
//        BlockPos pos,
//        PoseStack blockToView,
//        Matrix4f localToBlock,
//        VertexConsumer vertexConsumer,
//        boolean checkSides,
//        RandomSource random,
//        long combinedLight,
//        int combinedOverlay
//    ) {
////        boolean useAmbientOcclusion = Minecraft.useAmbientOcclusion() && state.getLightEmission() == 0 && model.useAmbientOcclusion();
////        var vec3 = state.getOffset(pos);
////        blockToView.translate(vec3.x, vec3.y, vec3.z);
////        try {
//            Minecraft.getInstance().getBlockRenderer().getModelRenderer().tesselateBlock(
//                level,
//                List.of(new TintedModel(model, tints)),
//                state,
//                pos,
//                blockToView,
//                vertexConsumer,
//                checkSides,
//                combinedOverlay
//            );
////            return tesselate(level, model, state, tints, pos, blockToView, localToBlock, vertexConsumer, checkSides, random, combinedLight, combinedOverlay, useAmbientOcclusion);
////        } catch (Throwable throwable) {
////            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block model");
////            CrashReportCategory crashreportcategory = crashreport.addCategory("Block model being tesselated");
////            CrashReportCategory.populateBlockDetails(crashreportcategory, level, pos, state);
////            crashreportcategory.setDetail("Using AO", useAmbientOcclusion);
////            throw new ReportedException(crashreport);
////        }
//    }

//    private static final AmbientOcclusionsAccessor ambientOcclusionAccessor = Services.load(AmbientOcclusionsAccessor.class);

//    public static boolean tesselate(
//        BlockAndTintGetter level,
//        BlockModelPart model,
//        BlockState state,
//        int[] tints,
//        BlockPos pos,
//        PoseStack blockToView,
//        Matrix4f localToBlock,
//        VertexConsumer vertexConsumer,
//        boolean checkSides,
//        RandomSource random,
//        long combinedLight,
//        int combinedOverlay,
//        boolean useAmbientOcclusion
//    ) {
//        boolean flag = false;
//        float[] aoValues = useAmbientOcclusion ? new float[Direction.values().length * 2] : null;
//        BitSet bitset = new BitSet(3);
//        BlockPos.MutableBlockPos mutablePos = pos.mutable();
//
//        var localToBlockNormal = new Matrix3f(localToBlock);
//        localToBlockNormal.invert();
//        localToBlockNormal.transpose();
//
//        for(Direction direction : Direction.values()) {
//            random.setSeed(combinedLight);
//            List<BakedQuad> list = model.getQuads(direction);
//            if (!list.isEmpty()) {
//                mutablePos.setWithOffset(pos, direction);
//                if (!checkSides || Block.shouldRenderFace(state, level.getBlockState(pos), direction)) {
//                    if(useAmbientOcclusion)
//                        renderModelFaceAO(level, state, tints, pos, blockToView, vertexConsumer, list, aoValues, bitset, combinedOverlay, localToBlock, localToBlockNormal);
//                    else
//                        renderModelWithoutAo(level, state, tints, pos, LevelRenderer.getLightColor(LevelRenderer.BrightnessGetter.DEFAULT, level, state, mutablePos), combinedOverlay, false, blockToView, vertexConsumer, list, bitset, localToBlock, localToBlockNormal);
//                    flag = true;
//                }
//            }
//        }
//
//        random.setSeed(combinedLight);
//        List<BakedQuad> quads = model.getQuads(null);
//        if (!quads.isEmpty()) {
//            if(useAmbientOcclusion)
//                renderModelFaceAO(level, state, tints, pos, blockToView, vertexConsumer, quads, aoValues, bitset, combinedOverlay, localToBlock, localToBlockNormal);
//            else
//                renderModelWithoutAo(level, state, tints, pos, -1, combinedOverlay, true, blockToView, vertexConsumer, quads, bitset, localToBlock, localToBlockNormal);
//            flag = true;
//        }
//
//        return flag;
//    }

//    public static BlockModelPart withReplacedTexture(BlockModelPart model, Function<TextureAtlasSprite, TextureAtlasSprite> mapping) {
//
//        return new BlockModelPart() {
//            @Override
//            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
//                return model.getQuads(state, side, rand).stream().map(q -> {
//                    var tas = mapping.apply(q.sprite());
//                    if(tas == null) return q;
//                    var vertices = q.vertices().clone();
//                    for(var vertex = 0; vertex < 4; vertex++){
//                        var i = vertex * 8 + 4;
//                        var u0 = q.sprite().getU0();
//                        var u1 = q.sprite().getU1();
//                        var v0 = q.sprite().getV0();
//                        var v1 = q.sprite().getV1();
//                        var rawU = (Float.intBitsToFloat(vertices[i]) - u0) / (u1 - u0);
//                        var rawV = (Float.intBitsToFloat(vertices[i + 1]) - v0) / (v1 - v0);
//                        vertices[i] = Float.floatToRawIntBits(tas.getU(rawU));
//                        vertices[i + 1] = Float.floatToRawIntBits(tas.getV(rawV));
//                    }
//                    return new BakedQuad(vertices, q.tintIndex(), q.direction(), tas, q.shade());
//                }).collect(Collectors.toList());
//            }
//
//            @Override
//            public boolean useAmbientOcclusion() {
//                return model.useAmbientOcclusion();
//            }
//
//            @Override
//            public boolean isGui3d() {
//                return model.isGui3d();
//            }
//
//            @Override
//            public boolean usesBlockLight() {
//                return model.usesBlockLight();
//            }
//
//            @Override
//            public boolean isCustomRenderer() {
//                return model.isCustomRenderer();
//            }
//
//            @Override
//            public TextureAtlasSprite getParticleIcon() {
//                return model.getParticleIcon();
//            }
//
//            @Override
//            public ItemTransforms getTransforms() {
//                return model.getTransforms();
//            }
//
//            @Override
//            public ItemOverrides getOverrides() {
//                return model.getOverrides();
//            }
//    };
//}

//    private static void renderModelFaceAO(BlockAndTintGetter level, BlockState state, int[] tints, BlockPos pos, PoseStack blockToView, VertexConsumer vertexConsumer, List<BakedQuad> quads, float[] aoFloats, BitSet bitset, int combinedOverlay, Matrix4f localToBlock, Matrix3f localToBlockNormal) {
//        var poseMatrix = blockToView.last();
//        for(BakedQuad bakedquad : quads) {
//            bakedquad = transform(bakedquad, localToBlock, localToBlockNormal);
//            var shadingQuad = clampWithinUnitCube(bakedquad);
//            ((BlockModelRendererAccessor)Renderer.get()).shapeCalculation(level, state, pos, shadingQuad.vertices(), shadingQuad.direction(), aoFloats, bitset);
////            if (!ForgeHooksClient.calculateFaceWithoutAO(level, state, pos, bakedquad, bitset.get(0), brightness, lightmap))
//            ambientOcclusionAccessor.calculate(level, state, pos, shadingQuad.direction(), aoFloats, bitset, shadingQuad.shade());
//            var brightness = ambientOcclusionAccessor.getBrightness();
//            var lightmap = ambientOcclusionAccessor.getLightMap();
//            putQuadData(tints, vertexConsumer, poseMatrix, bakedquad, brightness[0], brightness[1], brightness[2], brightness[3], lightmap[0], lightmap[1], lightmap[2], lightmap[3], combinedOverlay);
//        }
//
//    }
//
//    private static void renderModelWithoutAo(BlockAndTintGetter level, BlockState state, int[] tints, BlockPos pos, int lightColor, int combinedOverlay, boolean p_111007_, PoseStack blockToView, VertexConsumer vertexConsumer, List<BakedQuad> quads, BitSet bitSet, Matrix4f localToBlock, Matrix3f localToBlockNormal) {
//        var poseMatrix = blockToView.last();
//        for(BakedQuad bakedquad : quads) {
//            bakedquad = transform(bakedquad, localToBlock, localToBlockNormal);
//            if (p_111007_) {
//                var shadingQuad = clampWithinUnitCube(bakedquad);
//                ((BlockModelRendererAccessor)Renderer.get()).shapeCalculation(level, state, pos, shadingQuad.vertices(), shadingQuad.direction(), (float[])null, bitSet);
//                BlockPos blockpos = bitSet.get(0) ? pos.relative(shadingQuad.direction()) : pos;
//                lightColor = LevelRenderer.getLightColor(LevelRenderer.BrightnessGetter.DEFAULT, level, state, blockpos);
//            }
//
//            float f = level.getShade(bakedquad.direction(), bakedquad.shade());
//            putQuadData(tints, vertexConsumer, poseMatrix, bakedquad, f, f, f, f, lightColor, lightColor, lightColor, lightColor, combinedOverlay);
//        }
//    }
//
//    private static Direction transform(Direction dir, Matrix4f localPose) {
//        var normal = new Vector4f(dir.getStepX(), dir.getStepY(), dir.getStepZ(), 0);
//        normal.mul(localPose);
//        var x = Math.abs(normal.x());
//        var y = Math.abs(normal.y());
//        var z = Math.abs(normal.z());
//        if (x > z && x > y) {
//            return normal.x() < 0 ? Direction.WEST : Direction.EAST;
//        } else if (y > z) {
//            return normal.y() < 0 ? Direction.DOWN : Direction.UP;
//        } else {
//            return normal.z() < 0 ? Direction.NORTH : Direction.SOUTH;
//        }
//    }
//
//    private static void putQuadData(int[] tints, VertexConsumer vertexConsumer, PoseStack.Pose pose, BakedQuad quad, float aor, float aog, float aob, float aoa, int lr, int lg, int lb, int la, int combinedOverlay) {
//        float r;
//        float g;
//        float b;
//        if (quad.isTinted()) {
//            int i = tints[quad.tintIndex()];
//            r = (float)(i >> 16 & 255) / 255.0F;
//            g = (float)(i >> 8 & 255) / 255.0F;
//            b = (float)(i & 255) / 255.0F;
//        } else {
//            r = 1.0F;
//            g = 1.0F;
//            b = 1.0F;
//        }
//
//        vertexConsumer.putBulkData(pose, quad, new float[]{aor, aog, aob, aoa}, r, g, b, 1.0f, new int[]{lr, lg, lb, la}, combinedOverlay, true);
//    }
//
//    // I got these from IQuadTransformer
//    private static int STRIDE = STRIDE = DefaultVertexFormat.BLOCK.getVertexSize() / 4;
//    private static int POSITION = findOffset(VertexFormatElement.POSITION);
//    private static int NORMAL = findOffset(VertexFormatElement.NORMAL);
//    private static int findOffset(VertexFormatElement element) {
//        int index = DefaultVertexFormat.BLOCK.getOffset(element);
//        return index < 0 ? -1 : index / 4;
//    }
//
//    private static BakedQuad transform(BakedQuad model, Matrix4f localToBlock, Matrix3f localToBlockNormal) {
//        var dir = transform(model.direction(), localToBlock);
//        int[] vertices = model.vertices();
//        vertices = Arrays.copyOf(vertices, vertices.length);
//
//        int i;
//        int offset;
//        float xx;
//        float y;
//        for(i = 0; i < 4; ++i) {
//            offset = i * STRIDE + POSITION;
//            float x = Float.intBitsToFloat(vertices[offset]);
//            xx = Float.intBitsToFloat(vertices[offset + 1]);
//            y = Float.intBitsToFloat(vertices[offset + 2]);
//            Vector4f pos = new Vector4f(x, xx, y, 1.0F);
//            pos.mul(localToBlock);
//            pos.div(pos.w);
//            vertices[offset] = Float.floatToRawIntBits(pos.x());
//            vertices[offset + 1] = Float.floatToRawIntBits(pos.y());
//            vertices[offset + 2] = Float.floatToRawIntBits(pos.z());
//        }
//
//        for(i = 0; i < 4; ++i) {
//            offset = i * STRIDE + NORMAL;
//            int normalIn = vertices[offset];
//            if ((normalIn & 16777215) != 0) {
//                xx = (float)((byte)(normalIn & 255)) / 127.0F;
//                y = (float)((byte)(normalIn >> 8 & 255)) / 127.0F;
//                float z = (float)((byte)(normalIn >> 16 & 255)) / 127.0F;
//                Vector3f posx = new Vector3f(xx, y, z);
//                posx.mul(localToBlockNormal);
//                posx.normalize();
//                vertices[offset] = (byte)((int)(posx.x() * 127.0F)) & 255 | ((byte)((int)(posx.y() * 127.0F)) & 255) << 8 | ((byte)((int)(posx.z() * 127.0F)) & 255) << 16 | normalIn & -16777216;
//            }
//        }
//        return new BakedQuad(vertices, model.tintIndex(), dir, model.sprite(), model.shade(), model.lightEmission());
//    }
//
//    private static BakedQuad clampWithinUnitCube(BakedQuad quad){
//        var oldData = quad.vertices();
//        var newData = new int[oldData.length];
//        for (int i = 0; i < 4; i++)
//        {
//            float x = Math.min(1, Math.max(0, Float.intBitsToFloat(oldData[i * 8])));
//            float y = Math.min(1, Math.max(0, Float.intBitsToFloat(oldData[i * 8 + 1])));
//            float z = Math.min(1, Math.max(0, Float.intBitsToFloat(oldData[i * 8 + 2])));
//
//            newData[i * 8] = Float.floatToRawIntBits(x);
//            newData[i * 8 + 1] = Float.floatToRawIntBits(y);
//            newData[i * 8 + 2] = Float.floatToRawIntBits(z);
//        }
//        return new BakedQuad(newData, quad.tintIndex(), quad.direction(), quad.sprite(), quad.shade(), quad.lightEmission());
//    }

}
