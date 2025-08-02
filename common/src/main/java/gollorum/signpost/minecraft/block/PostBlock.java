package gollorum.signpost.minecraft.block;

import com.mojang.serialization.Codec;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.interactions.Interactable;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.data.PostData;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.minecraft.utils.Texture;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.IDelay;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.function.TriFunction;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public abstract class PostBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    @Override
    public boolean hasDynamicShape() {
        return true;
    }

    public static final EnumProperty<Direction> Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static class ModelType {

        private static final Map<String, ModelType> allTypes = new HashMap<>();

        public static final Codec<ModelType> CODEC = Codec.STRING.xmap(
            allTypes::get,
            type -> type.name
        );

        public static void register(ModelType modelType, String name) {
            if (name.length() < 3 || name.length() > 30) {
                throw new IllegalArgumentException("ModelType name must be between 3 and 30 characters");
            }
            allTypes.put(name, modelType);
        }
        public static void register(ModelType modelType) { register(modelType, modelType.name); }
        public static Optional<ModelType> getByName(String name, boolean logErrorIfNotPresent) {
            if (allTypes.containsKey(name)) return Optional.of(allTypes.get(name));
            else {
                if(logErrorIfNotPresent) Signpost.LOGGER.error("Tried to get invalid model type " + name);
                return Optional.empty();
            }
        }

        public static final ModelType Acacia = new ModelType("acacia",
            ResourceLocation.parse("acacia_log"),
            ResourceLocation.parse("stripped_acacia_log"),
            ResourceLocation.parse("acacia_log"),
            r -> Ingredient.of(Items.ACACIA_SIGN),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.ACACIA_LOGS)),
            r -> Ingredient.of(Items.ACACIA_SIGN)
        );
        public static final ModelType Birch = new ModelType("birch",
            ResourceLocation.parse("birch_log"),
            ResourceLocation.parse("stripped_birch_log"),
            ResourceLocation.parse("birch_log"),
            r -> Ingredient.of(Items.BIRCH_SIGN),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.BIRCH_LOGS)),
            r -> Ingredient.of(Items.BIRCH_SIGN)
        );
        public static final ModelType Iron = new ModelType("iron",
            ResourceLocation.parse("iron_block"),
            ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "iron"),
            ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "iron_dark"),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.SIGNS)),
            r -> Ingredient.of(Items.IRON_INGOT),
            r -> Ingredient.of(Items.IRON_INGOT)
        );
        public static final ModelType Jungle = new ModelType("jungle",
            ResourceLocation.parse("jungle_log"),
            ResourceLocation.parse("stripped_jungle_log"),
            ResourceLocation.parse("jungle_log"),
            r -> Ingredient.of(Items.JUNGLE_SIGN),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.JUNGLE_LOGS)),
            r -> Ingredient.of(Items.JUNGLE_SIGN)
        );
        public static final ModelType Oak = new ModelType("oak",
            ResourceLocation.parse("oak_log"),
            ResourceLocation.parse("stripped_oak_log"),
            ResourceLocation.parse("oak_log"),
            r -> Ingredient.of(Items.OAK_SIGN),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.OAK_LOGS)),
            r -> Ingredient.of(Items.OAK_SIGN)
        );
        public static final ModelType DarkOak = new ModelType("darkoak",
            ResourceLocation.parse("dark_oak_log"),
            ResourceLocation.parse("stripped_dark_oak_log"),
            ResourceLocation.parse("dark_oak_log"),
            r -> Ingredient.of(Items.DARK_OAK_SIGN),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.DARK_OAK_LOGS)),
            r -> Ingredient.of(Items.DARK_OAK_SIGN)
        );
        public static final ModelType Spruce = new ModelType("spruce",
            ResourceLocation.parse("spruce_log"),
            ResourceLocation.parse("stripped_spruce_log"),
            ResourceLocation.parse("spruce_log"),
            r -> Ingredient.of(Items.SPRUCE_SIGN),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.SPRUCE_LOGS)),
            r -> Ingredient.of(Items.SPRUCE_SIGN)
        );
        public static final ModelType Mangrove = new ModelType("mangrove",
            ResourceLocation.parse("mangrove_log"),
            ResourceLocation.parse("stripped_mangrove_log"),
            ResourceLocation.parse("mangrove_log"),
            r -> Ingredient.of(Items.MANGROVE_SIGN),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.MANGROVE_LOGS)),
            r -> Ingredient.of(Items.MANGROVE_SIGN)
        );
        public static final ModelType Bamboo = new ModelType("bamboo",
            ResourceLocation.parse("bamboo_block"),
            ResourceLocation.parse("stripped_bamboo_block"),
            ResourceLocation.parse("bamboo_block"),
            r -> Ingredient.of(Items.BAMBOO_SIGN),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.BAMBOO_BLOCKS)),
            r -> Ingredient.of(Items.BAMBOO_SIGN)
        );
        public static final ModelType Cherry = new ModelType("cherry",
            ResourceLocation.parse("cherry_log"),
            ResourceLocation.parse("stripped_cherry_log"),
            ResourceLocation.parse("cherry_log"),
            r -> Ingredient.of(Items.CHERRY_SIGN),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.CHERRY_LOGS)),
            r -> Ingredient.of(Items.CHERRY_SIGN)
        );
        public static final ModelType Stone = new ModelType("stone",
            ResourceLocation.parse("stone"),
            ResourceLocation.parse("stone"),
            ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "stone_dark"),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.SIGNS)),
            r -> Ingredient.of(Items.STONE),
            r -> Ingredient.of(Items.STONE)
        );
        public static final ModelType RedMushroom = new ModelType("red_mushroom",
            ResourceLocation.parse("red_mushroom_block"),
            ResourceLocation.parse("mushroom_stem"),
            ResourceLocation.parse("red_mushroom_block"),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.SIGNS)),
            r -> Ingredient.of(Items.RED_MUSHROOM_BLOCK),
            r -> Ingredient.of(Items.RED_MUSHROOM)
        );
        public static final ModelType BrownMushroom = new ModelType("brown_mushroom",
            ResourceLocation.parse("brown_mushroom_block"),
            ResourceLocation.parse("mushroom_stem"),
            ResourceLocation.parse("brown_mushroom_block"),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.SIGNS)),
            r -> Ingredient.of(Items.BROWN_MUSHROOM_BLOCK),
            r -> Ingredient.of(Items.BROWN_MUSHROOM)
        );
        public static final ModelType Warped = new ModelType("warped",
            ResourceLocation.parse("warped_stem"),
            ResourceLocation.parse("stripped_warped_stem"),
            ResourceLocation.parse("warped_stem"),
            r -> Ingredient.of(Items.WARPED_SIGN),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.WARPED_STEMS)),
            r -> Ingredient.of(Items.WARPED_SIGN)
        );
        public static final ModelType Crimson = new ModelType("crimson",
            ResourceLocation.parse("crimson_stem"),
            ResourceLocation.parse("stripped_crimson_stem"),
            ResourceLocation.parse("crimson_stem"),
            r -> Ingredient.of(Items.CRIMSON_SIGN),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.CRIMSON_STEMS)),
            r -> Ingredient.of(Items.CRIMSON_SIGN)
        );
        private static final Ingredient sandstone =
            Ingredient.of(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SMOOTH_SANDSTONE);
        public static final ModelType Sandstone = new ModelType("sandstone",
            ResourceLocation.parse("sandstone"),
            ResourceLocation.parse("stripped_jungle_log"),
            ResourceLocation.parse("sandstone_bottom"),
            r -> Ingredient.of(r.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.SIGNS)),
            r -> sandstone,
            r -> sandstone
        );

        public static Optional<ModelType> from(Item signItem, HolderLookup.Provider registryAccess) {
            return allTypes.values().stream()
                .filter(t -> t.addSignIngredient.apply(registryAccess).test(new ItemStack(signItem)))
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
        public final Function<HolderLookup.Provider, Ingredient> signIngredient;
        public final Function<HolderLookup.Provider, Ingredient> baseIngredient;
        public final Function<HolderLookup.Provider, Ingredient> addSignIngredient;

        ModelType(
            String name, ResourceLocation postTexture, ResourceLocation mainTexture, ResourceLocation secondaryTexture,
            Function<HolderLookup.Provider, Ingredient> signIngredient, Function<HolderLookup.Provider, Ingredient> baseIngredient, Function<HolderLookup.Provider, Ingredient> addSignIngredient) {
            this(name, expand(postTexture), expand(mainTexture), expand(secondaryTexture), signIngredient, baseIngredient, addSignIngredient);
        }


        public ModelType(String name, Texture postTexture, Texture mainTexture, Texture secondaryTexture, Function<HolderLookup.Provider, Ingredient> signIngredient, Function<HolderLookup.Provider, Ingredient> baseIngredient, Function<HolderLookup.Provider, Ingredient> addSignIngredient) {
            this.name = name;
            this.postTexture = postTexture;
            this.mainTexture = mainTexture;
            this.secondaryTexture = secondaryTexture;
            this.signIngredient = signIngredient;
            this.baseIngredient = baseIngredient;
            this.addSignIngredient = addSignIngredient;
        }

        private static Texture expand(ResourceLocation loc){
            return new Texture(ResourceLocation.fromNamespaceAndPath(
                loc.getNamespace(),
                loc.getPath().startsWith("block/") ? loc.getPath() : "block/"+loc.getPath()
            ));
        }

        private static final StreamCodec<RegistryFriendlyByteBuf, Function<HolderLookup.Provider, Ingredient>> ingredientGetterStreamCodec = StreamCodec.of(
            (buffer, i) -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, i.apply(buffer.registryAccess())),
            buffer -> {
                Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                return (r) -> ingredient;
            }
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ModelType> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, mt -> mt.name,
            Texture.STREAM_CODEC, mt -> mt.postTexture,
            Texture.STREAM_CODEC, mt -> mt.mainTexture,
            Texture.STREAM_CODEC, mt -> mt.secondaryTexture,
            ingredientGetterStreamCodec, mt -> mt.signIngredient,
            ingredientGetterStreamCodec, mt -> mt.baseIngredient,
            ingredientGetterStreamCodec, mt -> mt.addSignIngredient,
            ModelType::new
        );

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
            this.registryName = REGISTRY_NAME + "_" + registryName;
            this.properties = properties
                .setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, registryName)));
            this.type = type;
            this.tool = tool;
        }

        public PostBlock createBlock(TriFunction<Properties, ModelType, Variant, PostBlock> factory) {
            assert block == null;
            return block = factory.apply(properties, type, this);
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
    public final Variant variant;

    protected PostBlock(Properties properties, ModelType type, Variant variant) {
        super(properties.noOcclusion());
        this.type = type;
        this.variant = variant;
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack currentStack) {
        super.setPlacedBy(world, pos, state, placer, currentStack);
        ItemStack stack = currentStack.copy();
        IDelay.forFrames(6, world.isClientSide(), () ->
            TileEntityUtils.delayUntilTileEntityExists(world, pos, PostTile.getBlockEntityType(), tile -> {
                tile.setSignpostOwner(Optional.of(PlayerHandle.from(placer)));
                boolean shouldAddNewSign = placer instanceof ServerPlayer;
                if (!world.isClientSide()) {
                    var customData = stack.get(PostData.TYPE);
                    if(customData != null) {
                        tile.readData(customData);
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
                        PacketHandler.getInstance().sendToPlayer(
                            (ServerPlayer) placer,
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
        var silkTouchHolder = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        if(!player.isCreative() && tile instanceof PostTile && EnchantmentHelper.getItemEnchantmentLevel(silkTouchHolder, item) == 0) {
            dropPartItems((PostTile) tile, world, pos);
        }
        super.playerDestroy(world, player, pos, state, tile, item);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getInteractionShape(state, world, pos);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity t = level.getBlockEntity(pos);
        return t instanceof PostTile
            ? ((PostTile) t).getBounds()
            : Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getInteractionShape(state, level, pos);
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
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return getShape(state, worldIn, pos, context);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return use(level, pos, player, InteractionHand.MAIN_HAND);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack item, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return use(level, blockPos, player, hand);
    }

    public InteractionResult use(Level world, BlockPos pos, Player player, InteractionHand hand) {
        BlockEntity tileEntity = world.getBlockEntity(pos);
        return tileEntity instanceof PostTile tile
            ? onActivate(tile, world, player, hand)
            : InteractionResult.SUCCESS;
    }

    public static InteractionResult onActivate(PostTile tile, Level world, Player player, InteractionHand hand) {
        return switch (tile
            .trace(player)
            .map(p -> p.part.blockPart().interact(new InteractionInfo(
                InteractionInfo.Type.RightClick,
                player, hand, tile, p,
                () -> tile.notifyMutation(p.id, p.part, p.part.blockPart().getMeta().identifier()),
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
    protected boolean propagatesSkylightDown(BlockState state) {
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

}
