package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import gollorum.signpost.Signpost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.SimpleModelTransform;
import net.minecraftforge.common.util.Lazy;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RenderingUtil {

    public static final float VOXEL_SIZE = 1f / 16f;
    public static final float FONT_TO_VOXEL_SIZE = VOXEL_SIZE / 8f;

    public static IBakedModel loadModel(ResourceLocation location) {
        return ModelLoader.instance().getBakedModel(
            location,
            new SimpleModelTransform(TransformationMatrix.identity()),
            Material::getSprite);
    }

    private static final Map<ResourceLocation, Map<ResourceLocation, Lazy<IBakedModel>>> cachedModels = new ConcurrentHashMap<>();

    public static Lazy<IBakedModel> loadModel(ResourceLocation modelLocation, ResourceLocation textureLocation) {
        final ResourceLocation textLoc = trim(textureLocation);
        return cachedModels.computeIfAbsent(modelLocation, x -> new ConcurrentHashMap<>())
            .computeIfAbsent(
                textLoc,
                x -> Lazy.of(() ->ModelLoader.instance().getUnbakedModel(modelLocation).bakeModel(ModelLoader.instance(),
                    m -> Minecraft.getInstance().getAtlasSpriteGetter(m.getAtlasLocation()).apply(textLoc),
                    new SimpleModelTransform(TransformationMatrix.identity()),
                    modelLocation
                ))
            );
    }

//    public static IBakedModel loadObj(ResourceLocation resourceLocation){
//        OBJModel objModel = OBJLoader.INSTANCE.loadModel(new OBJModel.ModelSettings(
//            resourceLocation, false, true, false, true, null));
//        objModel.bake(Minecraft.getInstance().getModelManager().)
//    }

    public static final ResourceLocation MODEL_SIGN = new ResourceLocation(Signpost.MOD_ID, "block/small_wide_sign");
    public static final ResourceLocation MODEL_POST = new ResourceLocation(Signpost.MOD_ID, "block/post_only");

    private static final Lazy<BlockModelRenderer> RENDERER = Lazy.of(() -> Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelRenderer());
    private static final Lazy<Tessellator> TESSELLATOR = Lazy.of(() -> Tessellator.getInstance());
    private static final Lazy<BufferBuilder> BUFFER_BUILDER = Lazy.of(() -> TESSELLATOR.get().getBuffer());

    public static interface RenderModel {
        void render(
            IBakedModel model,
            TileEntity tileEntity,
            IVertexBuilder buffer,
            boolean checkSides,
            Random random,
            long rand,
            int combinedOverlay
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
        inner.accept((model, tileEntity, buffer, checkSides, random, rand, combinedOverlay) -> {
            if(!tileEntity.hasWorld()) throw new RuntimeException("TileEntity without world cannot be rendered.");
            BUFFER_BUILDER.get().begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            RENDERER.get().renderModel(
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
            TESSELLATOR.get().draw();
        });
        matrix.pop();
    }

    public static float voxelToLocal(float voxelPos) {
        return voxelPos * VOXEL_SIZE + 0.5f;
    }

}
