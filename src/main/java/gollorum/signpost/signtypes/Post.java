package gollorum.signpost.signtypes;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.Signpost;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.tiles.PostTile;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SignItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraftforge.common.util.Lazy;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;

public class Post implements BlockPart<Post> {

    private static final AABB BOUNDS = new AABB(
        new Vector3(-2, -8, -2),
        new Vector3(2, 8, 2)
    ).map(RenderingUtil::voxelToLocal);

    public static final BlockPartMetadata<Post> METADATA = new BlockPartMetadata<>(
        "Post",
        (post, keyPrefix, compound) -> {
            compound.putString(keyPrefix + "texture", post.texture.toString());
        },
        (compound, keyPrefix) -> new Post(new ResourceLocation(compound.getString(keyPrefix + "texture")))
    );

    private Lazy<IBakedModel> model;
    private ResourceLocation texture;

    public Post(ResourceLocation texture) {
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
        ItemStack heldItem = info.player.getHeldItem(info.hand);
        if(isValidSign(heldItem)) {
            if (info.isRemote) {
                Minecraft.getInstance().displayGuiScreen(new SignGui(
                    info.tile,
                    gollorum.signpost.minecraft.block.Post.ModelType.from(info.player.getHeldItem(info.hand).getItem()),
                    info.traceResult.hitPos,
                    Optional.of(new ItemStack(heldItem.getItem(), 1))
                ));
            }
            info.player.inventory.func_234564_a_(i -> i.getItem().equals(heldItem.getItem()), 1, info.player.container.func_234641_j_());
            return InteractionResult.Accepted;
        } else return InteractionResult.Ignored;
    }

    private static boolean isValidSign(ItemStack itemStack) {
        if(itemStack == null || itemStack.getCount() < 1) return false;
        Item item = itemStack.getItem();
        return item instanceof SignItem || item.equals(Items.IRON_INGOT) || item.equals(Items.STONE);
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
            combinedOverlay,
            new Matrix4f(Quaternion.ONE)
        ));
    }

    @Override
    public BlockPartMetadata<Post> getMeta() {
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

    @Override
    public Collection<ItemStack> getDrops(PostTile tile) {
        Signpost.LOGGER.error("Sign post only was broken. This should not happen.");
        return Collections.emptySet();
    }
}
