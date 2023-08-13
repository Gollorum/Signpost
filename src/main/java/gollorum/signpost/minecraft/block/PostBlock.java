package gollorum.signpost.minecraft.block;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.interactions.Interactable;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.minecraft.utils.Texture;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.security.WithCountRestriction;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PostBlock extends BaseEntityBlock implements SimpleWaterloggedBlock, WithCountRestriction {

    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static class ModelType {

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
            new ResourceLocation("acacia_log"),
            Lazy.of(() -> Ingredient.of(Items.ACACIA_SIGN)),
            Lazy.of(() -> Ingredient.of(ItemTags.ACACIA_LOGS)),
            Lazy.of(() -> Ingredient.of(Items.ACACIA_SIGN))
        );
        public static final ModelType Birch = new ModelType("birch",
            new ResourceLocation("birch_log"),
            new ResourceLocation("stripped_birch_log"),
            new ResourceLocation("birch_log"),
            Lazy.of(() -> Ingredient.of(Items.BIRCH_SIGN)),
            Lazy.of(() -> Ingredient.of(ItemTags.BIRCH_LOGS)),
            Lazy.of(() -> Ingredient.of(Items.BIRCH_SIGN))
        );
        public static final ModelType Iron = new ModelType("iron",
            new ResourceLocation("iron_block"),
            new ResourceLocation(Signpost.MOD_ID, "iron"),
            new ResourceLocation(Signpost.MOD_ID, "iron_dark"),
            Lazy.of(() -> Ingredient.of(ItemTags.SIGNS)),
            Lazy.of(() -> Ingredient.of(Items.IRON_INGOT)),
            Lazy.of(() -> Ingredient.of(Items.IRON_INGOT))
        );
        public static final ModelType Jungle = new ModelType("jungle",
            new ResourceLocation("jungle_log"),
            new ResourceLocation("stripped_jungle_log"),
            new ResourceLocation("jungle_log"),
            Lazy.of(() -> Ingredient.of(Items.JUNGLE_SIGN)),
            Lazy.of(() -> Ingredient.of(ItemTags.JUNGLE_LOGS)),
            Lazy.of(() -> Ingredient.of(Items.JUNGLE_SIGN))
        );
        public static final ModelType Oak = new ModelType("oak",
            new ResourceLocation("oak_log"),
            new ResourceLocation("stripped_oak_log"),
            new ResourceLocation("oak_log"),
            Lazy.of(() -> Ingredient.of(Items.OAK_SIGN)),
            Lazy.of(() -> Ingredient.of(ItemTags.OAK_LOGS)),
            Lazy.of(() -> Ingredient.of(Items.OAK_SIGN))
        );
        public static final ModelType DarkOak = new ModelType("darkoak",
            new ResourceLocation("dark_oak_log"),
            new ResourceLocation("stripped_dark_oak_log"),
            new ResourceLocation("dark_oak_log"),
            Lazy.of(() -> Ingredient.of(Items.DARK_OAK_SIGN)),
            Lazy.of(() -> Ingredient.of(ItemTags.DARK_OAK_LOGS)),
            Lazy.of(() -> Ingredient.of(Items.DARK_OAK_SIGN))
        );
        public static final ModelType Spruce = new ModelType("spruce",
            new ResourceLocation("spruce_log"),
            new ResourceLocation("stripped_spruce_log"),
            new ResourceLocation("spruce_log"),
            Lazy.of(() -> Ingredient.of(Items.SPRUCE_SIGN)),
            Lazy.of(() -> Ingredient.of(ItemTags.SPRUCE_LOGS)),
            Lazy.of(() -> Ingredient.of(Items.SPRUCE_SIGN))
        );
        public static final ModelType Mangrove = new ModelType("mangrove",
            new ResourceLocation("mangrove_log"),
            new ResourceLocation("stripped_mangrove_log"),
            new ResourceLocation("mangrove_log"),
            Lazy.of(() -> Ingredient.of(Items.MANGROVE_SIGN)),
            Lazy.of(() -> Ingredient.of(ItemTags.MANGROVE_LOGS)),
            Lazy.of(() -> Ingredient.of(Items.MANGROVE_SIGN))
        );
        public static final ModelType Bamboo = new ModelType("bamboo",
            new ResourceLocation("bamboo_block"),
            new ResourceLocation("stripped_bamboo_block"),
            new ResourceLocation("bamboo_block"),
            Lazy.of(() -> Ingredient.of(Items.BAMBOO_SIGN)),
            Lazy.of(() -> Ingredient.of(ItemTags.BAMBOO_BLOCKS)),
            Lazy.of(() -> Ingredient.of(Items.BAMBOO_SIGN))
        );
        public static final ModelType Cherry = new ModelType("cherry",
            new ResourceLocation("cherry_log"),
            new ResourceLocation("stripped_cherry_log"),
            new ResourceLocation("cherry_log"),
            Lazy.of(() -> Ingredient.of(Items.CHERRY_SIGN)),
            Lazy.of(() -> Ingredient.of(ItemTags.CHERRY_LOGS)),
            Lazy.of(() -> Ingredient.of(Items.CHERRY_SIGN))
        );
        public static final ModelType Stone = new ModelType("stone",
            new ResourceLocation("stone"),
            new ResourceLocation("stone"),
            new ResourceLocation(Signpost.MOD_ID, "stone_dark"),
            Lazy.of(() -> Ingredient.of(ItemTags.SIGNS)),
            Lazy.of(() -> Ingredient.of(Items.STONE)),
            Lazy.of(() -> Ingredient.of(Items.STONE))
        );
        public static final ModelType RedMushroom = new ModelType("red_mushroom",
            new ResourceLocation("red_mushroom_block"),
            new ResourceLocation("mushroom_stem"),
            new ResourceLocation("red_mushroom_block"),
            Lazy.of(() -> Ingredient.of(ItemTags.SIGNS)),
            Lazy.of(() -> Ingredient.of(Items.RED_MUSHROOM_BLOCK)),
            Lazy.of(() -> Ingredient.of(Items.RED_MUSHROOM))
        );
        public static final ModelType BrownMushroom = new ModelType("brown_mushroom",
            new ResourceLocation("brown_mushroom_block"),
            new ResourceLocation("mushroom_stem"),
            new ResourceLocation("brown_mushroom_block"),
            Lazy.of(() -> Ingredient.of(ItemTags.SIGNS)),
            Lazy.of(() -> Ingredient.of(Items.BROWN_MUSHROOM_BLOCK)),
            Lazy.of(() -> Ingredient.of(Items.BROWN_MUSHROOM))
        );
        public static final ModelType Warped = new ModelType("warped",
            new ResourceLocation("warped_stem"),
            new ResourceLocation("stripped_warped_stem"),
            new ResourceLocation("warped_stem"),
            Lazy.of(() -> Ingredient.of(Items.WARPED_SIGN)),
            Lazy.of(() -> Ingredient.of(ItemTags.WARPED_STEMS)),
            Lazy.of(() -> Ingredient.of(Items.WARPED_SIGN))
        );
        public static final ModelType Crimson = new ModelType("crimson",
            new ResourceLocation("crimson_stem"),
            new ResourceLocation("stripped_crimson_stem"),
            new ResourceLocation("crimson_stem"),
            Lazy.of(() -> Ingredient.of(Items.CRIMSON_SIGN)),
            Lazy.of(() -> Ingredient.of(ItemTags.CRIMSON_STEMS)),
            Lazy.of(() -> Ingredient.of(Items.CRIMSON_SIGN))
        );
        private static final Lazy<Ingredient> sandstone = Lazy.of(() ->
            Ingredient.of(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SMOOTH_SANDSTONE));
        public static final ModelType Sandstone = new ModelType("sandstone",
            new ResourceLocation("sandstone"),
            new ResourceLocation("stripped_jungle_log"),
            new ResourceLocation("sandstone_bottom"),
            Lazy.of(() -> Ingredient.of(ItemTags.SIGNS)),
            sandstone,
            sandstone
        );

        public static Optional<ModelType> from(Item signItem) {
            return allTypes.values().stream()
                .filter(t -> t.addSignIngredient.get().test(new ItemStack(signItem)))
                .findFirst();
        }

        static {
            register(Acacia);
            register(Birch);
            register(Iron);
            register(Stone);
            register(Jungle);
            register(Oak);
            register(DarkOak);
            register(Spruce);
            register(Mangrove);
            register(Bamboo);
            register(Cherry);
            register(Warped);
            register(Crimson);
            register(Sandstone);
            register(BrownMushroom);
            register(RedMushroom);
        }

        public final String name;
        public final Texture postTexture;
        public final Texture mainTexture;
        public final Texture secondaryTexture;
        public final Lazy<Ingredient> signIngredient;
        public final Lazy<Ingredient> baseIngredient;
        public final Lazy<Ingredient> addSignIngredient;

        ModelType(
            String name, ResourceLocation postTexture, ResourceLocation mainTexture, ResourceLocation secondaryTexture,
            Lazy<Ingredient> signIngredient, Lazy<Ingredient> baseIngredient, Lazy<Ingredient> addSignIngredient) {
            this.name = name;
            this.postTexture = expand(postTexture);
            this.mainTexture = expand(mainTexture);
            this.secondaryTexture = expand(secondaryTexture);
            this.signIngredient = signIngredient;
            this.baseIngredient = baseIngredient;
            this.addSignIngredient = addSignIngredient;
        }

        ModelType(
            String name, Texture postTexture, Texture mainTexture, Texture secondaryTexture,
            Lazy<Ingredient> signIngredient, Lazy<Ingredient> baseIngredient, Lazy<Ingredient> addSignIngredient) {
            this.name = name;
            this.postTexture = postTexture;
            this.mainTexture = mainTexture;
            this.secondaryTexture = secondaryTexture;
            this.signIngredient = signIngredient;
            this.baseIngredient = baseIngredient;
            this.addSignIngredient = addSignIngredient;
        }

        private static Texture expand(ResourceLocation loc){
            return new Texture(new ResourceLocation(
                loc.getNamespace(),
                loc.getPath().startsWith("block/") ? loc.getPath() : "block/"+loc.getPath()
            ), Optional.empty());
        }

        public static BufferSerializable<ModelType> Serializer = new SerializerImpl();
        public static final class SerializerImpl implements BufferSerializable<ModelType> {
            @Override
            public Class<ModelType> getTargetClass() {
                return ModelType.class;
            }

            @Override
            public void write(ModelType modelType, FriendlyByteBuf buffer) {
                StringSerializer.instance.write(modelType.name, buffer);
                Texture.Serializer.write(modelType.postTexture, buffer);
                Texture.Serializer.write(modelType.mainTexture, buffer);
                Texture.Serializer.write(modelType.secondaryTexture, buffer);
                modelType.signIngredient.get().toNetwork(buffer);
                modelType.baseIngredient.get().toNetwork(buffer);
                modelType.addSignIngredient.get().toNetwork(buffer);
            }

            private <T> Lazy<T> constLazy(T t) { return Lazy.of(() -> t); }
            @Override
            public ModelType read(FriendlyByteBuf buffer) {
                return new ModelType(
                    StringSerializer.instance.read(buffer),
                    Texture.Serializer.read(buffer),
                    Texture.Serializer.read(buffer),
                    Texture.Serializer.read(buffer),
                    constLazy(Ingredient.fromNetwork(buffer)),
                    constLazy(Ingredient.fromNetwork(buffer)),
                    constLazy(Ingredient.fromNetwork(buffer))
                );
            }
        };

    }

    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static class Variant {

        public static enum RequiredTool {
            Axe, Pickaxe
        }

        private PostBlock block = null;
        public final String registryName;
        public final Properties properties;
        public final ModelType type;
        public final RequiredTool tool;

        public Variant(Properties properties, ModelType type, String registryName, RequiredTool tool) {
            this.properties = properties;
            this.type = type;
            this.tool = tool;
            this.registryName = REGISTRY_NAME + "_" + registryName;
        }

        public PostBlock createBlock() {
            assert block == null;
            return block = new PostBlock(properties, type);
        }

        public PostBlock getBlock() {
            assert block != null;
            return block;
        }

    }

    public static final String REGISTRY_NAME = "post";

    public static final Variant STONE = new Variant(PropertiesUtil.STONE, ModelType.Stone, "stone", Variant.RequiredTool.Pickaxe);
    public static final Variant IRON = new Variant(PropertiesUtil.IRON, ModelType.Iron, "iron", Variant.RequiredTool.Pickaxe);
    public static final Variant OAK = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Oak), ModelType.Oak, "oak", Variant.RequiredTool.Axe);
    public static final Variant DARK_OAK = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.DarkOak), ModelType.DarkOak, "dark_oak", Variant.RequiredTool.Axe);
    public static final Variant SPRUCE = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Spruce), ModelType.Spruce, "spruce", Variant.RequiredTool.Axe);
    public static final Variant BIRCH = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Birch), ModelType.Birch, "birch", Variant.RequiredTool.Axe);
    public static final Variant JUNGLE = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Jungle), ModelType.Jungle, "jungle", Variant.RequiredTool.Axe);
    public static final Variant ACACIA = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Acacia), ModelType.Acacia, "acacia", Variant.RequiredTool.Axe);
    public static final Variant MANGROVE = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Mangrove), ModelType.Mangrove, "mangrove", Variant.RequiredTool.Axe);
    public static final Variant BAMBOO = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Bamboo), ModelType.Bamboo, "bamboo", Variant.RequiredTool.Axe);
    public static final Variant CHERRY = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Cherry), ModelType.Cherry, "cherry", Variant.RequiredTool.Axe);
    public static final Variant WARPED = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Warped), ModelType.Warped, "warped", Variant.RequiredTool.Axe);
    public static final Variant CRIMSON = new Variant(PropertiesUtil.wood(PropertiesUtil.WoodType.Crimson), ModelType.Crimson, "crimson", Variant.RequiredTool.Axe);
    public static final Variant SANDSTONE = new Variant(PropertiesUtil.STONE, ModelType.Sandstone, "sandstone", Variant.RequiredTool.Pickaxe);
    public static final Variant BROWN_MUSHROOM = new Variant(PropertiesUtil.mushroom(MapColor.DIRT), ModelType.BrownMushroom, "brown_mushroom", Variant.RequiredTool.Axe);
    public static final Variant RED_MUSHROOM = new Variant(PropertiesUtil.mushroom(MapColor.COLOR_RED), ModelType.RedMushroom, "red_mushroom", Variant.RequiredTool.Axe);

    public static final List<Variant> AllVariants = Arrays.asList(OAK, BIRCH, SPRUCE, JUNGLE, DARK_OAK, ACACIA, MANGROVE, BAMBOO, CHERRY, STONE, IRON, WARPED, CRIMSON, SANDSTONE, BROWN_MUSHROOM, RED_MUSHROOM);
    public static Block[] getAllBlocks() {
        return AllVariants.stream().map(Variant::getBlock).toArray(Block[]::new);
    }

    public final ModelType type;

    private PostBlock(Properties properties, ModelType type) {
        super(properties.noOcclusion());
        this.type = type;
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack currentStack) {
        super.setPlacedBy(world, pos, state, placer, currentStack);
        ItemStack stack = currentStack.copy();
        Delay.forFrames(6, world.isClientSide(), () ->
            TileEntityUtils.delayUntilTileEntityExists(world, pos, PostTile.getBlockEntityType(), tile -> {
                tile.setSignpostOwner(Optional.of(PlayerHandle.from(placer)));
                boolean shouldAddNewSign = placer instanceof ServerPlayer;
                if (!world.isClientSide()) {
                    if(stack.hasTag() && stack.getTag().contains("Parts")) {
                        tile.readParts(stack.getTag().getCompound("Parts"));
                        shouldAddNewSign = false;
                    } else {
                        tile.addPart(
                            new BlockPartInstance(new PostBlockPart(type.postTexture), Vector3.ZERO),
                            ItemStack.EMPTY,
                            PlayerHandle.from(placer)
                        );
                    }
                    tile.setChanged();
                    world.sendBlockUpdated(pos, state, state, 3);
                    if(shouldAddNewSign)
                        PacketHandler.send(
                            PacketDistributor.PLAYER.with(() -> (ServerPlayer) placer),
                            new RequestSignGui.ForNewSign.Package(
                                new WorldLocation(pos, world),
                                tile.modelType,
                                new Vector3(0, 1, 0),
                                ItemStack.EMPTY
                            )
                        );
                }
            }, 100, Optional.of(() -> Signpost.LOGGER.error("Could not initialize placed signpost: BlockEntity never appeared."))));
    }

    private void dropPartItems(PostTile tile, Level world, BlockPos pos) {
        NonNullList<ItemStack> drops = NonNullList.create();
        drops.addAll(tile.getDrops());

        Containers.dropContents(
            world,
            pos,
            drops
        );
    }

    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity tile, ItemStack item) {
        if(!player.isCreative() && tile instanceof PostTile && !EnchantmentHelper.getEnchantments(item).containsKey(Enchantments.SILK_TOUCH)) {
            dropPartItems((PostTile) tile, world, pos);
        }
        super.playerDestroy(world, player, pos, state, tile, item);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player) {
        ItemStack ret = super.getCloneItemStack(state, target, world, pos, player);
        world.getBlockEntity(pos, PostTile.getBlockEntityType()).ifPresent(tile -> {
            if(!ret.hasTag()) ret.setTag(new CompoundTag());
            ret.getTag().put("Parts", tile.writeParts(false));
        });
        return ret;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        BlockEntity t = world.getBlockEntity(pos);
        return t instanceof PostTile
            ? ((PostTile) t).getBounds()
            : Shapes.empty();
    }

    @SuppressWarnings("deprecation")
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PostTile(type, new ItemStack(this), pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Facing).add(WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return getShape(state, worldIn, pos, context);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity tileEntity = world.getBlockEntity(pos);
        if(!(tileEntity instanceof PostTile)) return InteractionResult.SUCCESS;
        PostTile tile = (PostTile) tileEntity;
        return onActivate(tile, world, player, hand);
    }

    public static InteractionResult onActivate(PostTile tile, Level world, Player player, InteractionHand hand) {
        return switch (tile
            .trace(player)
            .map(p -> p.part.blockPart.interact(new InteractionInfo(
                InteractionInfo.Type.RightClick,
                player, hand, tile, p,
                data -> tile.notifyMutation(p.id, data, p.part.blockPart.getMeta().identifier),
                world.isClientSide()
            )))
            .orElse(Interactable.InteractionResult.Ignored)
            ) {
            case Accepted -> InteractionResult.SUCCESS;
            case Ignored -> InteractionResult.PASS;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var res = super.getStateForPlacement(context);
        if(res == null) res = defaultBlockState();
        return res
            .setValue(Facing, context.getHorizontalDirection())
            .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if(!state.hasProperty(Facing)) return state;
        return state.setValue(Facing, rot.rotate(state.getValue(Facing)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        if(!state.hasProperty(Facing)) return state;
        return state.setValue(Facing, state.getValue(Facing).getOpposite());
    }

    @Override
    public BlockRestrictions.Type getBlockRestrictionType() {
        return BlockRestrictions.Type.Signpost;
    }

}
