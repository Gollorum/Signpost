package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.SimpleModelTransform;
import net.minecraftforge.common.util.Lazy;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
        return ModelLoader.instance().getUnbakedModel(modelLocation).bakeModel(
            ModelLoader.instance(),
            m -> Minecraft.getInstance().getAtlasSpriteGetter(m.getAtlasLocation()).apply(textLoc),
            new SimpleModelTransform(TransformationMatrix.identity()),
            modelLocation
        );
    }

    public static IBakedModel loadModel(ResourceLocation modelLocation, Supplier<ResourceLocation> textureLocation) {
        return ModelLoader.instance().getUnbakedModel(modelLocation).bakeModel(
            ModelLoader.instance(),
            m -> Minecraft.getInstance().getAtlasSpriteGetter(m.getAtlasLocation()).apply(trim(textureLocation.get())),
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

    public static IBakedModel loadModel(ResourceLocation modelLocation, Supplier<ResourceLocation> textureLocation1, Supplier<ResourceLocation> textureLocation2) {
        return ModelLoader.instance().getUnbakedModel(modelLocation).bakeModel(
            ModelLoader.instance(),
            m -> Minecraft.getInstance().getAtlasSpriteGetter(m.getAtlasLocation()).apply(
                trim((m.getTextureLocation().equals(PostModel.mainTextureMarker)
                    ? textureLocation1 : textureLocation2).get())
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
                combinedOverlay
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

    public static void renderGui(IBakedModel model, Point center, float yaw, float pitch, float scale, Vector3 offset) {
        MatrixStack matrixStack = new MatrixStack();
        Minecraft.getInstance().getTextureManager().bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
        Minecraft.getInstance().getTextureManager().getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).setBlurMipmapDirect(false, false);
        RenderSystem.enableRescaleNormal();
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.translate(center.x, center.y, 100);
        matrixStack.scale(scale, -scale, scale);
        matrixStack.rotate(new Quaternion(Vector3f.XP, pitch, true));
        matrixStack.rotate(new Quaternion(Vector3f.YP, yaw, true));
        matrixStack.translate(offset.x, offset.y, offset.z);
        matrixStack.translate(0.5f, 0.5f, 0);
        IRenderTypeBuffer.Impl renderTypeBuffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        RenderHelper.setupGuiFlatDiffuseLighting();

        Minecraft.getInstance().getItemRenderer().renderItem(
            new ItemStack(Blocks.OAK_LOG),
            ItemCameraTransforms.TransformType.GUI,
            false,
            matrixStack,
            renderTypeBuffer,
            0xf000f0,
            OverlayTexture.NO_OVERLAY,
            model
        );
        renderTypeBuffer.finish();
        RenderSystem.enableDepthTest();
        RenderHelper.setupGui3DDiffuseLighting();

        RenderSystem.disableAlphaTest();
        RenderSystem.disableRescaleNormal();
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
