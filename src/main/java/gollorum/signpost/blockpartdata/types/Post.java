package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.Waystone;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.PaintPostGui;
import gollorum.signpost.minecraft.gui.SignGui;
import gollorum.signpost.minecraft.gui.WaystoneGui;
import gollorum.signpost.minecraft.items.Brush;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SignItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class Post implements BlockPart<Post> {

    private static final int maxSignCount = 10;

    private static final AABB BOUNDS = new AABB(
        new Vector3(-2, -8, -2),
        new Vector3(2, 8, 2)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<Post> METADATA = new BlockPartMetadata<>(
        "Post",
        (post, compound) -> compound.putString("texture", post.texture.toString()),
        (compound) -> new Post(new ResourceLocation(compound.getString("texture"))),
        Post.class
    );

    private ResourceLocation texture;

    public Post(ResourceLocation texture) {
        setTexture(texture);
    }

    public void setTexture(ResourceLocation texture){
        this.texture = texture;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    @Override
    public Intersectable<Ray, Float> getIntersection() { return BOUNDS; }

    @Override
    public InteractionResult interact(InteractionInfo info) {
        ItemStack heldItem = info.player.getHeldItem(info.hand);
        PlayerHandle playerHandle = PlayerHandle.from(info.player);
        if(isValidSign(heldItem)) return attachSign(info, heldItem);
        else if(isWaystone(heldItem)) return attachWaystone(info, heldItem, playerHandle);
        else if(isBrush(heldItem)) return paint(info);
        else return InteractionResult.Ignored;
    }

    private InteractionResult attachWaystone(InteractionInfo info, ItemStack heldItem, PlayerHandle playerHandle) {
        if(info.tile.getParts().stream().noneMatch(p -> p.blockPart instanceof Waystone))
            if(!info.isRemote && BlockRestrictions.getInstance().tryDecrementRemaining(BlockRestrictions.Type.Waystone, playerHandle)) {
                info.tile.addPart(
                    new BlockPartInstance(new gollorum.signpost.blockpartdata.types.Waystone(playerHandle), Vector3.ZERO),
                    new ItemStack(heldItem.getItem()),
                    PlayerHandle.from(info.player)
                );
                info.tile.markDirty();
            }
            else WaystoneGui.display(new WorldLocation(info.tile.getPos(), info.player.world), Optional.empty());
        return InteractionResult.Accepted;
    }

    private InteractionResult attachSign(InteractionInfo info, ItemStack heldItem) {
        if (info.isRemote && info.tile.getParts().stream().filter(i -> i.blockPart instanceof Sign).count() < maxSignCount) {
            SignGui.display(
                info.tile,
                gollorum.signpost.minecraft.block.Post.ModelType.from(info.player.getHeldItem(info.hand).getItem()),
                info.traceResult.hitPos,
                new ItemStack(heldItem.getItem(), 1)
            );
        }
        return InteractionResult.Accepted;
    }

    private InteractionResult paint(InteractionInfo info) {
        if(info.isRemote) {
            PaintPostGui.display(info.tile, this, info.traceResult.id);
        }
        return InteractionResult.Accepted;
    }

    private static boolean isValidSign(ItemStack itemStack) {
        if(itemStack == null || itemStack.getCount() < 1) return false;
        Item item = itemStack.getItem();
        return item instanceof SignItem || item.equals(Items.IRON_INGOT) || item.equals(Items.STONE);
    }

    private static boolean isWaystone(ItemStack itemStack) {
        if(itemStack == null || itemStack.getCount() < 1) return false;
        Item item = itemStack.getItem();
        return item.equals(Waystone.INSTANCE.asItem());
    }

    private static boolean isBrush(ItemStack itemStack) {
        if(itemStack == null || itemStack.getCount() < 1) return false;
        Item item = itemStack.getItem();
        return item instanceof Brush;
    }

    @Override
    public BlockPartMetadata<Post> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundNBT compound) {
        METADATA.write(this, compound);
    }

    private void notifyTextureChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("type", "texture");
        compound.putString("texture", texture.toString());
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile, PlayerEntity editingPlayer) {
        setTexture(new ResourceLocation(compound.getString("texture")));
    }

    @Override
    public boolean hasThePermissionToEdit(WithOwner owner, PlayerEntity player) { return true; }

    @Override
    public Collection<ItemStack> getDrops(PostTile tile) {
        Signpost.LOGGER.error("Sign post only was broken. This should not happen.");
        return Collections.emptySet();
    }

}
