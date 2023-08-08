package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.PaintPostGui;
import gollorum.signpost.minecraft.gui.RequestWaystoneGui;
import gollorum.signpost.minecraft.gui.SignGui;
import gollorum.signpost.minecraft.items.Brush;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.minecraft.utils.SideUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class PostBlockPart implements BlockPart<PostBlockPart> {

    private static final int maxSignCount = 10;

    private static final AABB BOUNDS = new AABB(
        new Vector3(-2, -8, -2),
        new Vector3(2, 8, 2)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<PostBlockPart> METADATA = new BlockPartMetadata<>(
        "Post",
        (post, compound) -> compound.putString("texture", post.texture.toString()),
        (compound) -> new PostBlockPart(new ResourceLocation(compound.getString("texture"))),
        PostBlockPart.class
    );

    private ResourceLocation texture;

    public PostBlockPart(ResourceLocation texture) {
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
        ItemStack heldItem = info.player.getItemInHand(info.hand);
        PlayerHandle playerHandle = PlayerHandle.from(info.player);
        if(isValidSign(heldItem)) return attachSign(info, heldItem);
        else if(isWaystone(heldItem)) return attachWaystone(info, heldItem, playerHandle);
        else if(isBrush(heldItem)) return paint(info);
        else return InteractionResult.Ignored;
    }

    private InteractionResult attachWaystone(InteractionInfo info, ItemStack heldItem, PlayerHandle playerHandle) {
        if(info.tile.getParts().stream().noneMatch(p -> p.blockPart instanceof WaystoneBlockPart)) {
            if (!info.isRemote && BlockRestrictions.getInstance().tryDecrementRemaining(BlockRestrictions.Type.Waystone, playerHandle)) {
                info.tile.addPart(
                    new BlockPartInstance(new WaystoneBlockPart(playerHandle), Vector3.ZERO),
                    new ItemStack(heldItem.getItem()),
                    PlayerHandle.from(info.player)
                );
                info.tile.setChanged();
                SideUtils.makePlayerPay(info.player, new ItemStack(WaystoneBlock.getInstance()));
                if(info.player instanceof ServerPlayer)
                    PacketHandler.send(
                        PacketDistributor.PLAYER.with(() -> (ServerPlayer) info.player),
                        new RequestWaystoneGui.Package(new WorldLocation(info.tile.getBlockPos(), info.player.level()), Optional.empty())
                    );
                else Signpost.LOGGER.error("Tried to ask player to open the waystone GUI, but it was a " + info.player.getClass().getName());
            }
            return InteractionResult.Accepted;
        } else return InteractionResult.Ignored;
    }

    private InteractionResult attachSign(InteractionInfo info, ItemStack heldItem) {
        if (info.isRemote && info.tile.getParts().stream().filter(i -> i.blockPart instanceof SignBlockPart).count() < maxSignCount) {
            SignGui.display(
                info.tile,
                PostBlock.ModelType.from(info.player.getItemInHand(info.hand).getItem()).get(),
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
        return PostBlock.ModelType.from(item).isPresent();
    }

    private static boolean isWaystone(ItemStack itemStack) {
        if(itemStack == null || itemStack.getCount() < 1) return false;
        Item item = itemStack.getItem();
        return item.equals(WaystoneBlock.getInstance().asItem());
    }

    private static boolean isBrush(ItemStack itemStack) {
        if(itemStack == null || itemStack.getCount() < 1) return false;
        Item item = itemStack.getItem();
        return item instanceof Brush;
    }

    @Override
    public BlockPartMetadata<PostBlockPart> getMeta() {
        return METADATA;
    }

    @Override
    public void writeTo(CompoundTag compound) {
        METADATA.write(this, compound);
    }

    private void notifyTextureChanged(InteractionInfo info) {
        CompoundTag compound = new CompoundTag();
        compound.putString("type", "texture");
        compound.putString("texture", texture.toString());
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundTag compound, BlockEntity tile, Player editingPlayer) {
        setTexture(new ResourceLocation(compound.getString("texture")));
    }

    @Override
    public boolean hasThePermissionToEdit(WithOwner owner, Player player) { return true; }

    @Override
    public Collection<ItemStack> getDrops(PostTile tile) {
        return Collections.emptySet();
    }

    @Override
    public Collection<ResourceLocation> getAllTextures() {
        return Collections.singleton(getTexture());
    }

}
