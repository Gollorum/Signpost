package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.gui.Point;
import gollorum.signpost.minecraft.gui.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.SimpleModelTransform;
import net.minecraftforge.common.util.Lazy;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RenderingUtil {

    public static final float VoxelSize = 1f / 16f;
    public static final float FontToVoxelSize = VoxelSize / 8f;

    public static IBakedModel loadModel(ResourceLocation location) {
        return ModelLoader.instance().getBakedModel(
            location,
            new SimpleModelTransform(TransformationMatrix.identity()),
            RenderMaterial::getSprite);
    }

    private static final Map<ResourceLocation, Map<ResourceLocation, Lazy<IBakedModel>>> cachedModels = new ConcurrentHashMap<>();

    public static Lazy<IBakedModel> loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation) {
        final ResourceLocation textLoc = trim(textureLocation);
        return cachedModels.computeIfAbsent(modelLocation, x -> new ConcurrentHashMap<>())
            .computeIfAbsent(
                textLoc,
                x -> Lazy.of(() -> ModelLoader.instance().getUnbakedModel(modelLocation).bakeModel(ModelLoader.instance(),
                    m -> Minecraft.getInstance().getAtlasSpriteGetter(m.getAtlasLocation()).apply(textLoc),
                    new SimpleModelTransform(TransformationMatrix.identity()),
                    modelLocation
                ))
            );
    }

    private static final Map<ResourceLocation, Map<ResourceLocation, Map<ResourceLocation, Lazy<IBakedModel>>>> cachedTwoTexturedModels = new ConcurrentHashMap<>();
    private static final ResourceLocation texture1Marker = new ResourceLocation(Signpost.MOD_ID, "block/oak_wood");

    public static Lazy<IBakedModel> loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation1, ResourceLocation textureLocation2) {
        final ResourceLocation textLoc1 = trim(textureLocation1);
        final ResourceLocation textLoc2 = trim(textureLocation2);
        return cachedTwoTexturedModels
            .computeIfAbsent(modelLocation, x -> new ConcurrentHashMap<>())
            .computeIfAbsent(textLoc1, x -> new ConcurrentHashMap<>())
            .computeIfAbsent(textLoc2,
                x -> Lazy.of(() -> ModelLoader.instance().getUnbakedModel(modelLocation).bakeModel(ModelLoader.instance(),
                    m -> Minecraft.getInstance().getAtlasSpriteGetter(m.getAtlasLocation()).apply(
                        m.getTextureLocation().equals(texture1Marker)
                            ? textLoc1 : textLoc2
                    ),
                    new SimpleModelTransform(TransformationMatrix.identity()),
                    modelLocation
                ))
            );
    }

    public static final ResourceLocation ModelWideSign = new ResourceLocation(Signpost.MOD_ID, "block/small_wide_sign");
    public static final ResourceLocation ModelShortSign = new ResourceLocation(Signpost.MOD_ID, "block/small_short_sign");
    public static final ResourceLocation ModelLargeSign = new ResourceLocation(Signpost.MOD_ID, "block/large_sign");
    public static final ResourceLocation ModelPost = new ResourceLocation(Signpost.MOD_ID, "block/post_only");

    private static final Lazy<BlockModelRenderer> Renderer = Lazy.of(() -> Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelRenderer());
    private static final Lazy<Tessellator> Tesselator = Lazy.of(Tessellator::getInstance);
    private static final Lazy<BufferBuilder> BufferBuilder = Lazy.of(() -> Tesselator.get().getBuffer());

    public static interface RenderModel {
        void render(
            IBakedModel model,
            TileEntity tileEntity,
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
        matrix.push();
        inner.accept((model, tileEntity, buffer, checkSides, random, rand, combinedOverlay, rotationMatrix) -> {
            if(!tileEntity.hasWorld()) throw new RuntimeException("TileEntity without world cannot be rendered.");
            BufferBuilder.get().begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            Renderer.get().renderModel(
                tileEntity.getWorld(),
                model,
                tileEntity.getBlockState(),
                tileEntity.getPos(),
                matrix,
                buffer,
                checkSides,
                random,
                rand,
                combinedOverlay,
                tileEntity.getModelData()
            );
            Tesselator.get().draw();
        });
        matrix.pop();
    }

    public static float voxelToLocal(float voxelPos) {
        return voxelPos * VoxelSize + 0.5f;
    }

    public static int drawString(FontRenderer fontRenderer, String text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, int color, int maxWidth, boolean dropShadow){
        IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer()); // copied from fontRenderer
        int textWidth = fontRenderer.getStringWidth(text);
        float scale = Math.min(1f, maxWidth / (float) textWidth);
        Matrix4f matrix = Matrix4f.makeTranslate(
            Rect.xCoordinateFor(point.x, maxWidth, xAlignment) + maxWidth * 0.5f,
            Rect.yCoordinateFor(point.y, fontRenderer.FONT_HEIGHT, yAlignment) + fontRenderer.FONT_HEIGHT * 0.5f,
            0
        );
        if(scale < 1) matrix.mul(Matrix4f.makeScale(scale, scale, scale));
        int i = fontRenderer.renderString(
            text,
            (maxWidth - Math.min(maxWidth, textWidth)) * 0.5f,
            -fontRenderer.FONT_HEIGHT * 0.5f,
            color,
            dropShadow,
            matrix,
            buffer,
            false,
            0,
            15728880 // copied from fontRenderer
        );
        buffer.finish();
        return i;
    }

}
