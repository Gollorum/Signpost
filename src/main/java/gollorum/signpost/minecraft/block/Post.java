package gollorum.signpost.minecraft.block;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.interactions.Interactable;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.SignGui;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.TileEntityUtils;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.ItemTags;
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
import net.minecraftforge.common.util.Lazy;

import javax.annotation.Nullable;
import java.util.*;

public class Post extends Block implements IWaterLoggable {

    public static class ModelType implements IStringSerializable {

        public static final Map<ModelType, Lazy<Ingredient>> signIngredientForType = new HashMap<>();
        public static final Map<ModelType, Lazy<Ingredient>> baseIngredientForType = new HashMap<>();

        private static final Map<String, ModelType> allTypes = new HashMap<>();

        public static void register(ModelType modelType, String name) { allTypes.put(name, modelType); }
        public static void register(ModelType modelType) { register(modelType, modelType.name); }
        public static Optional<ModelType> getByName(String name, boolean logErrorIfNotPresent) {
            if (allTypes.containsKey(name)) return Optional.of(allTypes.get(name));
            else {
                if(logErrorIfNotPresent) Signpost.LOGGER.error("Tried to get invalid model type " + name);
                return Optional.empty();
            }
        }

        public static final ModelType Acacia = new ModelType("acacia",
            new ResourceLocation("acacia_log"),
            new ResourceLocation("stripped_acacia_log"),
            new ResourceLocation("acacia_log")
        );
        public static final ModelType Birch = new ModelType("birch",
            new ResourceLocation("birch_log"),
            new ResourceLocation("stripped_birch_log"),
            new ResourceLocation("birch_log")
        );
        public static final ModelType Iron = new ModelType("iron",
            new ResourceLocation("iron_block"),
            new ResourceLocation(Signpost.MOD_ID, "iron"),
            new ResourceLocation(Signpost.MOD_ID, "iron_dark")
        );
        public static final ModelType Jungle = new ModelType("jungle",
            new ResourceLocation("jungle_log"),
            new ResourceLocation("stripped_jungle_log"),
            new ResourceLocation("jungle_log")
        );
        public static final ModelType Oak = new ModelType("oak",
            new ResourceLocation("oak_log"),
            new ResourceLocation("stripped_oak_log"),
            new ResourceLocation("oak_log")
        );
        public static final ModelType DarkOak = new ModelType("darkoak",
            new ResourceLocation("dark_oak_log"),
            new ResourceLocation("stripped_dark_oak_log"),
            new ResourceLocation("dark_oak_log")
        );
        public static final ModelType Spruce = new ModelType("spruce",
            new ResourceLocation("spruce_log"),
            new ResourceLocation("stripped_spruce_log"),
            new ResourceLocation("spruce_log")
        );
        public static final ModelType Stone = new ModelType("stone",
            new ResourceLocation("stone"),
            new ResourceLocation("stone"),
            new ResourceLocation(Signpost.MOD_ID, "stone_dark")
        );
        public static final ModelType Warped = new ModelType("warped",
            new ResourceLocation("warped_stem"),
            new ResourceLocation("stripped_warped_stem"),
            new ResourceLocation("warped_stem")
        );
        public static final ModelType Crimson = new ModelType("crimson",
            new ResourceLocation("crimson_stem"),
            new ResourceLocation("stripped_crimson_stem"),
            new ResourceLocation("crimson_stem")
        );

        static {
            signIngredientForType.put(Acacia, Lazy.of(() -> Ingredient.fromItems(Items.ACACIA_SIGN)));
            signIngredientForType.put(Birch, Lazy.of(() -> Ingredient.fromItems(Items.BIRCH_SIGN)));
            signIngredientForType.put(Iron, Lazy.of(() -> Ingredient.fromTag(ItemTags.SIGNS)));
            signIngredientForType.put(Stone, Lazy.of(() -> Ingredient.fromTag(ItemTags.SIGNS)));
            signIngredientForType.put(Jungle, Lazy.of(() -> Ingredient.fromItems(Items.JUNGLE_SIGN)));
            signIngredientForType.put(Oak, Lazy.of(() -> Ingredient.fromItems(Items.OAK_SIGN)));
            signIngredientForType.put(DarkOak, Lazy.of(() -> Ingredient.fromItems(Items.DARK_OAK_SIGN)));
            signIngredientForType.put(Spruce, Lazy.of(() -> Ingredient.fromItems(Items.SPRUCE_SIGN)));
            signIngredientForType.put(Warped, Lazy.of(() -> Ingredient.fromItems(Items.WARPED_SIGN)));
            signIngredientForType.put(Crimson, Lazy.of(() -> Ingredient.fromItems(Items.CRIMSON_SIGN)));

            baseIngredientForType.put(Acacia, Lazy.of(() -> Ingredient.fromTag(ItemTags.ACACIA_LOGS)));
            baseIngredientForType.put(Birch, Lazy.of(() -> Ingredient.fromTag(ItemTags.BIRCH_LOGS)));
            baseIngredientForType.put(Iron, Lazy.of(() -> Ingredient.fromItems(Items.IRON_INGOT)));
            baseIngredientForType.put(Stone, Lazy.of(() -> Ingredient.fromItems(Items.STONE)));
            baseIngredientForType.put(Jungle, Lazy.of(() -> Ingredient.fromTag(ItemTags.JUNGLE_LOGS)));
            baseIngredientForType.put(Oak, Lazy.of(() -> Ingredient.fromTag(ItemTags.OAK_LOGS)));
            baseIngredientForType.put(DarkOak, Lazy.of(() -> Ingredient.fromTag(ItemTags.DARK_OAK_LOGS)));
            baseIngredientForType.put(Spruce, Lazy.of(() -> Ingredient.fromTag(ItemTags.SPRUCE_LOGS)));
            baseIngredientForType.put(Warped, Lazy.of(() -> Ingredient.fromTag(ItemTags.WARPED_STEMS)));
            baseIngredientForType.put(Crimson, Lazy.of(() -> Ingredient.fromTag(ItemTags.CRIMSON_STEMS)));

            register(Acacia);
            register(Birch);
            register(Iron);
            register(Stone);
            register(Jungle);
            register(Oak);
            register(DarkOak);
            register(Spruce);
            register(Warped);
            register(Crimson);
        }

        public final String name;
        public final ResourceLocation postTexture;
        public final ResourceLocation mainTexture;
        public final ResourceLocation secondaryTexture;

        ModelType(String name, ResourceLocation postTexture, ResourceLocation mainTexture, ResourceLocation secondaryTexture) {
            this.name = name;
            this.postTexture = expand(postTexture);
            this.mainTexture = expand(mainTexture);
            this.secondaryTexture = expand(secondaryTexture);
        }

        private static ResourceLocation expand(ResourceLocation loc){
            return new ResourceLocation(loc.getNamespace(), "block/"+loc.getPath());
        }

        @Override
        public String getString() {
            return name;
        }

        public static ModelType from(Item signItem) {
            if(signItem.equals(Items.ACACIA_SIGN))
                return Acacia;
            else if(signItem.equals(Items.BIRCH_SIGN))
                return Birch;
            else if(signItem.equals(Items.DARK_OAK_SIGN))
                return DarkOak;
            else if(signItem.equals(Items.IRON_INGOT))
                return Iron;
            else if(signItem.equals(Items.JUNGLE_SIGN))
                return Jungle;
            else if(signItem.equals(Items.OAK_SIGN))
                return Oak;
            else if(signItem.equals(Items.SPRUCE_SIGN))
                return Spruce;
            else if(signItem.equals(Items.WARPED_SIGN))
                return Warped;
            else if(signItem.equals(Items.CRIMSON_SIGN))
                return Crimson;
            else if(signItem.equals(Items.STONE))
                return Stone;
            else
                return Oak;
        }
    }

    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static class Variant {
        public final Post block;
        public final String registryName;
        public final Properties properties;
        public final ModelType type;

        public Variant(Properties properties, ModelType type, String registryName) {
            this.properties = properties;
            this.type = type;
            this.block = new Post(properties, type);
            this.registryName = REGISTRY_NAME + "_" + registryName;
        }

    }

    public static final String REGISTRY_NAME = "post";

    public static final Variant STONE = new Variant(PropertiesUtil.STONE, ModelType.Stone, "stone");
    public static final Variant IRON = new Variant(PropertiesUtil.IRON, ModelType.Iron, "iron");
    public static final Variant OAK = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Oak), ModelType.Oak, "oak");
    public static final Variant DARK_OAK = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.DarkOak), ModelType.DarkOak, "dark_oak");
    public static final Variant SPRUCE = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Spruce), ModelType.Spruce, "spruce");
    public static final Variant BIRCH = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Birch), ModelType.Birch, "birch");
    public static final Variant JUNGLE = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Jungle), ModelType.Jungle, "jungle");
    public static final Variant ACACIA = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Acacia), ModelType.Acacia, "acacia");
    public static final Variant WARPED = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Warped), ModelType.Warped, "warped");
    public static final Variant CRIMSON = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Crimson), ModelType.Crimson, "crimson");

    public static final Variant[] AllVariants = new Variant[]{OAK, BIRCH, SPRUCE, JUNGLE, DARK_OAK, ACACIA, STONE, IRON, WARPED, CRIMSON};
    public static final Block[] ALL = Arrays.stream(AllVariants).map(i -> i.block).toArray(Block[]::new);

    public final ModelType type;

    private Post(Properties properties, ModelType type) {
        super(properties.notSolid());
        this.type = type;
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntityUtils.delayUntilTileEntityExists(world, pos, PostTile.class,
            tile -> {
                if (world.isRemote) {
                    SignGui.display(
                        tile,
                        tile.modelType,
                        new Vector3(0, 1, 0),
                        ItemStack.EMPTY
                    );
                } else {
                    tile.addPart(
                        new BlockPartInstance(new gollorum.signpost.blockpartdata.types.Post(type.postTexture), Vector3.ZERO),
                        ItemStack.EMPTY,
                        PlayerHandle.from(placer)
                    );
                    tile.markDirty();
                }
            }, 100, Optional.of(() -> Signpost.LOGGER.error("Could not initialize placed signpost: TileEntity never appeared."))
        );
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        TileEntity tileentity = builder.get(LootParameters.BLOCK_ENTITY);
        List<ItemStack> result = (tileentity instanceof PostTile)
            ? new ArrayList<>(((PostTile) tileentity).getDrops())
            : Collections.singletonList(new ItemStack(this));
        return result;
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
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @Override
    public boolean receiveFluid(IWorld worldIn, BlockPos pos, BlockState state, FluidState fluidStateIn) {
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
        return new PostTile(type, new ItemStack(this));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
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
        return onActivate(tile, world, player, hand);
    }

    public static ActionResultType onActivate(PostTile tile, World world, PlayerEntity player, Hand hand) {
        switch (tile
            .trace(player)
            .map(p -> p.part.blockPart.interact(new InteractionInfo(
                InteractionInfo.Type.RightClick,
                player, hand, tile, p,
                data -> tile.notifyMutation(p.id, data, p.part.blockPart.getMeta().identifier),
                world.isRemote
            )))
            .orElse(Interactable.InteractionResult.Ignored)
        ){
            case Accepted:
                return ActionResultType.SUCCESS;
            case Ignored:
            default:
                return ActionResultType.PASS;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return super.getStateForPlacement(context)
            .with(WATERLOGGED, context.getWorld().getFluidState(context.getPos()).getFluid() == Fluids.WATER);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
        return !state.get(WATERLOGGED);
    }

}
