package gollorum.signpost.signtypes;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.gui.SignGui;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Lazy;

import java.util.Random;

public class PostModel implements BlockPart<PostModel> {

    private static final AABB BOUNDS = new AABB(
        new Vector3(-2, -8, -2),
        new Vector3(2, 8, 2)
    ).map(RenderingUtil::voxelToLocal);

    public static final BlockPartMetadata<PostModel> METADATA = new BlockPartMetadata<>(
        "Post",
        (post, keyPrefix, compound) -> {
            compound.putString(keyPrefix + "texture", post.texture.toString());
        },
        (compound, keyPrefix) -> new PostModel(new ResourceLocation(compound.getString(keyPrefix + "texture")))
    );

    private Lazy<IBakedModel> model;
    private ResourceLocation texture;

    public PostModel(ResourceLocation texture) {
        setTexture(texture);
    }

    public void setTexture(ResourceLocation texture){
        model = RenderingUtil.loadModel(RenderingUtil.ModelPost, texture);
        this.texture = texture;
    }

    @Override
    public Intersectable<Ray, Float> getIntersection() { return BOUNDS; }

    @Override
    public InteractionResult interact(InteractionInfo info) {
        if(info.isRemote)
            Minecraft.getInstance().displayGuiScreen(new SignGui(info.tile, info.localHitPos));
        return InteractionResult.Accepted;
    }

    @Override
    public void render(TileEntity tileEntity, TileEntityRendererDispatcher renderDispatcher, MatrixStack matrix, IRenderTypeBuffer buffer, int combinedLights, int combinedOverlay, Random random, long randomSeed) {
        RenderingUtil.render(matrix, renderModel -> renderModel.render(
            model.get(),
            tileEntity,
            buffer.getBuffer(RenderType.getSolid()),
            false,
            random,
            randomSeed,
            combinedOverlay
        ));
    }

    @Override
    public BlockPartMetadata<PostModel> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundNBT compound, String keyPrefix) {
        METADATA.writeTo(this, compound, keyPrefix);
    }

    private void notifyTextureChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("type", "texture");
        compound.putString("texture", texture.toString());
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile) {
        setTexture(new ResourceLocation(compound.getString("texture")));
    }
}
