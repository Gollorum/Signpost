package gollorum.signpost.minecraft.block;

import gollorum.signpost.Signpost;
import gollorum.signpost.interactions.Interactable;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.SignGui;
import gollorum.signpost.signtypes.PostModel;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.TileEntityUtils;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;

public class Post extends Block implements IWaterLoggable {

    public static enum ModelType implements IStringSerializable {
        Acacia(new ResourceLocation("acacia_log"), new ResourceLocation(Signpost.MOD_ID, "acacia"), new ResourceLocation(Signpost.MOD_ID, "acacia_dark")),
        Birch(new ResourceLocation("birch_log"), new ResourceLocation(Signpost.MOD_ID, "birch"), new ResourceLocation("birch_log")),
        Iron(new ResourceLocation("iron_block"), new ResourceLocation(Signpost.MOD_ID, "iron"), new ResourceLocation(Signpost.MOD_ID, "iron_dark")),
        Jungle(new ResourceLocation("jungle_log"), new ResourceLocation(Signpost.MOD_ID, "jungle"), new ResourceLocation("jungle_log")),
        Oak(new ResourceLocation("oak_log"), new ResourceLocation(Signpost.MOD_ID, "oak"), new ResourceLocation(Signpost.MOD_ID, "oak_dark")),
        Spruce(new ResourceLocation("spruce_log"), new ResourceLocation(Signpost.MOD_ID, "spruce"), new ResourceLocation(Signpost.MOD_ID, "spruce_dark")),
        Stone(new ResourceLocation("stone"), new ResourceLocation("stone"), new ResourceLocation(Signpost.MOD_ID, "stone_dark"));

        public final ResourceLocation postLocation;
        public final ResourceLocation signTextureLocation;
        public final ResourceLocation darkSignTextureLocation;

        ModelType(ResourceLocation postLocation, ResourceLocation largeSignTextureLocation, ResourceLocation darkSignTextureLocation) {
            this.postLocation = expand(postLocation);
            this.signTextureLocation = expand(largeSignTextureLocation);
            this.darkSignTextureLocation = expand(darkSignTextureLocation);
        }

        private ResourceLocation expand(ResourceLocation loc){
            return new ResourceLocation(loc.getNamespace(), "textures/block/"+loc.getPath()+".png");
        }

        @Override
        public String func_176610_l() {
            return name().toLowerCase();
        }
    }

    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.makeCuboidShape(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);

    public static class Info {
        public final Post post;
        public final String registryName;
        public final Properties properties;
        public final ModelType type;

        public Info(Properties properties, ModelType type, String registryName) {
            this.properties = properties;
            this.type = type;
            this.post = newPost();
            this.registryName = REGISTRY_NAME + "_" + registryName;
        }

        public Post newPost(){
            return new Post(properties, type);
        }
    }

    public static final String REGISTRY_NAME = "post";

    public static final Info STONE = new Info(PropertiesUtil.STONE, ModelType.Stone, "stone");
    public static final Info IRON = new Info(PropertiesUtil.IRON, ModelType.Iron, "iron");
    public static final Info OAK = new Info(PropertiesUtil.wood(PropertiesUtil.WoodType.Oak), ModelType.Oak, "oak");
    public static final Info SPRUCE = new Info(PropertiesUtil.wood(PropertiesUtil.WoodType.Spruce), ModelType.Spruce, "spruce");
    public static final Info BIRCH = new Info(PropertiesUtil.wood(PropertiesUtil.WoodType.Birch), ModelType.Birch, "birch");
    public static final Info JUNGLE =new Info(PropertiesUtil.wood(PropertiesUtil.WoodType.Jungle), ModelType.Jungle, "jungle");
    public static final Info ACACIA = new Info(PropertiesUtil.wood(PropertiesUtil.WoodType.Acacia), ModelType.Acacia, "acacia");

    public static final Info[] All_INFOS = new Info[]{STONE, IRON, OAK, SPRUCE, BIRCH, JUNGLE, ACACIA};
    public static final Block[] ALL = Arrays.stream(All_INFOS).map(i -> i.post).toArray(Block[]::new);

    public final ModelType type;

    private Post(Properties properties, ModelType type) {
        super(properties.notSolid());
        this.type = type;
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        Delay.until(() -> TileEntityUtils.findTileEntity(world, pos, PostTile.class).isPresent(),
            () -> {
                PostTile tile = TileEntityUtils.findTileEntity(world, pos, PostTile.class).get();
                if (world.isRemote) {
                    Minecraft.getInstance().displayGuiScreen(new SignGui(tile, new Vector3(0, 1, 0)));
                } else {
                    tile.addPart(new BlockPartInstance(new PostModel(type.postLocation), Vector3.ZERO));
                    tile.markDirty();
                }
            }, 100
        );
    }

    @Override
    public void onPlayerDestroy(IWorld world, BlockPos pos, BlockState state) {
        super.onPlayerDestroy(world, pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        TileEntity t = world.getTileEntity(pos);
        return t instanceof PostTile
            ? ((PostTile) t).getBounds()
            : VoxelShapes.empty();
    }

    @SuppressWarnings("deprecation")
    @Override
    public IFluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @Override
    public boolean receiveFluid(IWorld worldIn, BlockPos pos, BlockState state, IFluidState fluidStateIn) {
        return IWaterLoggable.super.receiveFluid(worldIn, pos, state, fluidStateIn);
    }

    @Override
    public boolean canContainFluid(IBlockReader worldIn, BlockPos pos, BlockState state, Fluid fluidIn) {
        return IWaterLoggable.super.canContainFluid(worldIn, pos, state, fluidIn);
    }

    @Override
    public boolean hasTileEntity(BlockState state) { return true; }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new PostTile(type);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return getShape(state, worldIn, pos, context);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if(!(tileEntity instanceof PostTile)) return ActionResultType.SUCCESS;
        PostTile tile = (PostTile) tileEntity;
        switch (tile
            .trace(player)
            .map(p -> p.part.interact(new InteractionInfo(
                InteractionInfo.Type.RightClick,
                player,
                tile,
                p.hitPos,
                data -> tile.notifyMutation(p.id, data),
                world.isRemote
            )))
            .orElse(Interactable.InteractionResult.Ignored)
        ){
            case Accepted:
            case Ignored:
            default:
                return ActionResultType.SUCCESS;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return super.getStateForPlacement(context)
            .with(WATERLOGGED, context.getWorld().getFluidState(context.getPos()).getFluid() == Fluids.WATER);
    }
}
