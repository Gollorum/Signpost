package gollorum.signpost.signtypes;

import gollorum.signpost.Teleport;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.SignGui;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.serialization.OptionalSerializer;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Lazy;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Sign<Self extends Sign<Self>> implements BlockPart<Self> {

    protected Angle angle;
    protected int color;
    protected boolean flip;
    protected ResourceLocation mainTexture;
    protected ResourceLocation secondaryTexture;
    protected Optional<UUID> destination;
    protected Post.ModelType modelType;
    public Optional<UUID> getDestination() { return destination; }

    protected TransformedBox transformedBounds;
    protected Lazy<IBakedModel> model;

    public Optional<ItemStack> itemToDropOnBreak;

    public Sign(Angle angle, boolean flip, ResourceLocation mainTexture, ResourceLocation secondaryTexture, int color, Optional<UUID> destination, Post.ModelType modelType, Optional<ItemStack> itemToDropOnBreak) {
        this.color = color;
        this.destination = destination;
        this.modelType = modelType;
        this.itemToDropOnBreak = itemToDropOnBreak;
        setAngle(angle);
        setTextures(mainTexture, secondaryTexture);
        setFlip(flip);
    }

    public void setAngle(Angle angle) {
        this.angle = angle;
        regenerateTransformedBox();
    }

    public void setFlip(boolean flip) {
        this.flip = flip;
        regenerateTransformedBox();
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setDestination(Optional<UUID> destination) {
        this.destination = destination;
    }

    public void setItemToDropOnBreak(Optional<ItemStack> itemToDropOnBreak) {
        this.itemToDropOnBreak = itemToDropOnBreak;
    }

    private void setModelType(Post.ModelType modelType) {
        this.modelType = modelType;
    }

    public boolean isFlipped() { return flip; }

    protected abstract ResourceLocation getModel();

    public int getColor() { return color; }

    public void setTextures(ResourceLocation texture, ResourceLocation textureDark) {
        model = RenderingUtil.loadModel(getModel(), texture, textureDark);
        this.mainTexture = texture;
        this.secondaryTexture = textureDark;
    }

    protected abstract void regenerateTransformedBox();

    @Override
    public Intersectable<Ray, Float> getIntersection() {
        return transformedBounds;
    }

    @Override
    public InteractionResult interact(InteractionInfo info) {
        // TODO Implement.
        if (!info.isRemote) {
            if(holdsAngleTool(info)) {
                setAngle(angle.add(Angle.fromDegrees(15)));
                notifyAngleChanged(info);
            } else if(!holdsEditTool(info))
                destination.ifPresent(uuid -> Teleport.toWaystone(uuid, info.player));
        } else if(holdsEditTool(info)) {
            Minecraft.getInstance().displayGuiScreen(new SignGui(info.tile, modelType, this, new PostTile.TilePartInfo(info.tile, info.traceResult.id)));
        }
        return InteractionResult.Accepted;
    }

    private boolean holdsAngleTool(InteractionInfo info) {
        ItemStack itemStack = info.player.getHeldItem(info.hand);
        return !itemStack.isEmpty() && PostTile.isAngleTool(itemStack.getItem());
    }

    private boolean holdsEditTool(InteractionInfo info) {
        ItemStack itemStack = info.player.getHeldItem(info.hand);
        return !itemStack.isEmpty() && PostTile.isEditTool(itemStack.getItem());
    }

    protected void notifyAngleChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        Angle.SERIALIZER.writeTo(angle, compound, "Angle");
        info.mutationDistributor.accept(compound);
    }

    protected void notifyTextureChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("Texture", mainTexture.toString());
        compound.putString("TextureDark", secondaryTexture.toString());
        info.mutationDistributor.accept(compound);
    }

    protected void notifyFlipChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putBoolean("Flip", flip);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile) {
        if(Angle.SERIALIZER.isContainedIn(compound, "Angle"))
            setAngle(Angle.SERIALIZER.read(compound, "Angle"));

        boolean updateTextures = false;
        if(compound.contains("Texture")) {
            mainTexture = new ResourceLocation(compound.getString("Texture"));
            updateTextures = true;
        }
        if(compound.contains("TextureDark")){
            secondaryTexture = new ResourceLocation(compound.getString("TextureDark"));
            updateTextures = true;
        }
        if(updateTextures) setTextures(mainTexture, secondaryTexture);

        if(compound.contains("Flip")) setFlip(compound.getBoolean("Flip"));
        if(compound.contains("Color")) setColor(compound.getInt("Color"));
        if(OptionalSerializer.UUID.isContainedIn(compound, "Destination"))
            setDestination(OptionalSerializer.UUID.read(compound, "Destination"));
        if(OptionalSerializer.ItemStack.isContainedIn(compound, "ItemToDropOnBreak")) {
            setItemToDropOnBreak(OptionalSerializer.ItemStack.read(compound, "ItemToDropOnBreak"));
        }
        if(compound.contains("ModelType"))
            setModelType(Post.ModelType.valueOf(compound.getString("ModelType")));
        tile.markDirty();
    }

    @Override
    public Collection<ItemStack> getDrops(PostTile tile) {
        return itemToDropOnBreak.map(Collections::singleton).orElseGet(Collections::emptySet);
    }

    private void dropOn(World world, BlockPos pos) {
        if(itemToDropOnBreak.isPresent() && !world.isRemote) {
            ItemEntity itementity = new ItemEntity(
                world,
                pos.getX() + world.rand.nextFloat() * 0.5 + 0.25,
                pos.getY() + world.rand.nextFloat() * 0.5 + 0.25,
                pos.getZ() + world.rand.nextFloat() * 0.5 + 0.25,
                itemToDropOnBreak.get()
            );
            itementity.setDefaultPickupDelay();
            world.addEntity(itementity);
        }
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
                    .stream().map(q -> new BakedQuad(q.getVertexData(), q.getTintIndex(), directionMapping.get(q.getFace()), q.func_187508_a(), q.shouldApplyDiffuseLighting()))
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
            public boolean func_230044_c_() {
                return original.func_230044_c_();
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
