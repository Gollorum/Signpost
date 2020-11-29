package gollorum.signpost.signtypes;

import gollorum.signpost.Teleport;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
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

    protected TransformedBox transformedBounds;
    protected Lazy<IBakedModel> model;

    public Sign(Angle angle, boolean flip, ResourceLocation mainTexture, ResourceLocation secondaryTexture, int color, Optional<UUID> destination) {
        this.color = color;
        this.destination = destination;
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

    protected abstract ResourceLocation getModel();

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
            } else  destination.ifPresent(uuid -> Teleport.toWaystone(uuid, info.player));
        }
        return InteractionResult.Accepted;
    }

    private boolean holdsAngleTool(InteractionInfo info) {
        ItemStack itemStack = info.player.getHeldItem(info.hand);
        return !itemStack.isEmpty() && itemStack.getItem().equals(Items.STICK);
    }

    protected void notifyAngleChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("type", "angle");
        compound.putFloat("angle_radians", angle.radians());
        info.mutationDistributor.accept(compound);
    }

    protected void notifyTextureChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("type", "texture");
        compound.putString("texture", mainTexture.toString());
        compound.putString("textureDark", secondaryTexture.toString());
        info.mutationDistributor.accept(compound);
    }

    protected void notifyFlipChanged(InteractionInfo info) {
        CompoundNBT compound = new CompoundNBT();
        compound.putString("type", "flip");
        compound.putBoolean("flip", flip);
        info.mutationDistributor.accept(compound);
    }

    @Override
    public void readMutationUpdate(CompoundNBT compound, TileEntity tile) {
        switch (compound.getString("type")) {
            case "angle":
                setAngle(Angle.fromRadians(compound.getFloat("angle_radians")));
                break;
            case "texture":
                setTextures(new ResourceLocation(compound.getString("texture")), new ResourceLocation(compound.getString("textureDark")));
                break;
            case "flip":
                flip = compound.getBoolean("flip");
                break;
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
