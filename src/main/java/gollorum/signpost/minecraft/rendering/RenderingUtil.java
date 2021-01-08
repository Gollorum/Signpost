package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.gui.Point;
import gollorum.signpost.minecraft.gui.Rect;
import gollorum.signpost.utils.modelGeneration.SignModel;
import gollorum.signpost.utils.modelGeneration.SignModelFactory;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.SimpleModelTransform;
import net.minecraftforge.common.util.Lazy;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RenderingUtil {

    public static IBakedModel loadModel(ResourceLocation location) {
        return ModelLoader.instance().getBakedModel(
            location,
            new SimpleModelTransform(TransformationMatrix.identity()),
            RenderMaterial::getSprite);
    }

    public static IBakedModel loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation) {
        final ResourceLocation textLoc = trim(textureLocation);
        return ModelLoader.instance().getUnbakedModel(modelLocation).bakeModel(ModelLoader.instance(),
            m -> Minecraft.getInstance().getAtlasSpriteGetter(m.getAtlasLocation()).apply(textLoc),
            new SimpleModelTransform(TransformationMatrix.identity()),
            modelLocation
        );
    }

    public static IBakedModel loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation1, ResourceLocation textureLocation2) {
        final ResourceLocation textLoc1 = trim(textureLocation1);
        final ResourceLocation textLoc2 = trim(textureLocation2);
        return ModelLoader.instance().getUnbakedModel(modelLocation).bakeModel(
            ModelLoader.instance(),
            m -> Minecraft.getInstance().getAtlasSpriteGetter(m.getAtlasLocation()).apply(
                m.getTextureLocation().equals(PostModel.mainTextureMarker)
                    ? textLoc1 : textLoc2
            ),
            new SimpleModelTransform(TransformationMatrix.identity()),
            modelLocation
        );
    }

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
                combinedOverlay//,
//                tileEntity.getModelData()
            );
        });
        matrix.pop();
    }

    public static int drawString(FontRenderer fontRenderer, String text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, int color, int maxWidth, boolean dropShadow){
        IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
        int textWidth = fontRenderer.getStringWidth(text);
        float scale = Math.min(1f, maxWidth / (float) textWidth);
        Matrix4f matrix = Matrix4f.makeTranslate(
            Rect.xCoordinateFor(point.x, maxWidth, xAlignment) + maxWidth * 0.5f,
            Rect.yCoordinateFor(point.y, fontRenderer.FONT_HEIGHT, yAlignment) + fontRenderer.FONT_HEIGHT * 0.5f,
            100
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
            0xf000f0
        );
        buffer.finish();
        return i;
    }

    public static IBakedModel withTintIndex(IBakedModel original, int tintIndex) {
        return new IBakedModel() {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
                return original.getQuads(state, side, rand)
                           .stream().map(q -> new BakedQuad(q.getVertexData(), tintIndex, q.getFace(), q.getSprite(), q.applyDiffuseLighting()))
                           .collect(Collectors.toList());
            }

            @Override
            public boolean isAmbientOcclusion() {
                return original.isAmbientOcclusion();
            }

            @Override
            public boolean isGui3d() {
                return original.isGui3d();
            }

            @Override
            public boolean isSideLit() {
                return original.isSideLit();
            }

            @Override
            public boolean isBuiltInRenderer() {
                return original.isBuiltInRenderer();
            }

            @Override
            public TextureAtlasSprite getParticleTexture() {
                return original.getParticleTexture();
            }

            @Override
            public ItemOverrideList getOverrides() {
                return original.getOverrides();
            }
        };
    }

    public static IBakedModel withTransformedDirections(IBakedModel original, boolean isFlipped, float yaw) {
        Map<Direction, Direction> directionMapping = new HashMap<>();
        if(isFlipped){
            directionMapping.put(Direction.UP, Direction.DOWN);
            directionMapping.put(Direction.DOWN, Direction.UP);
        } else {
            directionMapping.put(Direction.UP, Direction.UP);
            directionMapping.put(Direction.DOWN, Direction.DOWN);
        }
        Direction[] dir = new Direction[]{
            Direction.NORTH,
            isFlipped ? Direction.WEST : Direction.EAST,
            Direction.SOUTH,
            isFlipped ? Direction.EAST : Direction.WEST
        };
        int indexOffset = Math.round((yaw / 360) * dir.length) % dir.length;
        if (indexOffset < 0) indexOffset += dir.length;
        directionMapping.put(Direction.NORTH, dir[indexOffset]);
        directionMapping.put(Direction.EAST, dir[(indexOffset + 1) % dir.length]);
        directionMapping.put(Direction.SOUTH, dir[(indexOffset + 2) % dir.length]);
        directionMapping.put(Direction.WEST, dir[(indexOffset + 3) % dir.length]);
        return new IBakedModel() {
            @Override
            public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
                return original.getQuads(state, directionMapping.get(side), rand)
                           .stream().map(q -> new BakedQuad(q.getVertexData(), q.getTintIndex(), directionMapping.get(q.getFace()), q.getSprite(), q.applyDiffuseLighting()))
                           .collect(Collectors.toList());
            }

            @Override
            public boolean isAmbientOcclusion() {
                return original.isAmbientOcclusion();
            }

            @Override
            public boolean isGui3d() {
                return original.isGui3d();
            }

            @Override
            public boolean isSideLit() {
                return original.isSideLit();
            }

            @Override
            public boolean isBuiltInRenderer() {
                return original.isBuiltInRenderer();
            }

            @Override
            public TextureAtlasSprite getParticleTexture() {
                return original.getParticleTexture();
            }

            @Override
            public ItemOverrideList getOverrides() {
                return original.getOverrides();
            }
        };
    }

}
