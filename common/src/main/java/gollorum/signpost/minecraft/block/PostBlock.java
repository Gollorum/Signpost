package gollorum.signpost.minecraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.interactions.Interactable;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.data.ModelTypeRegistry;
import gollorum.signpost.minecraft.data.PostData;
import gollorum.signpost.minecraft.gui.RequestSignGui;
import gollorum.signpost.minecraft.utils.Texture;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.StreamCodecUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public final class PostBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    @Override
    public boolean hasDynamicShape() {
        return true;
    }

    public static final EnumProperty<Direction> Facing = BlockStateProperties.HORIZONTAL_FACING;

    public static record ModelType(
        MaterialType materialType,
        Ingredient addSignIngredient,
        Texture postTexture,
        Texture mainTexture,
        Texture secondaryTexture
    ) {
        public ItemStack getItemStack(ResourceKey<ModelType> key, int count) {
            var item = new ItemStack(materialType.getBlock(), count);
            item.applyComponents(applyTo(key, DataComponentPatch.builder()).build());
            return item;
        }

        public static DataComponentPatch.Builder applyTo(ResourceKey<ModelType> holder, DataComponentPatch.Builder patchBuilder) {
            patchBuilder.set(PostData.TYPE, new PostData(holder, Map.of()));
            patchBuilder.set(DataComponents.ITEM_NAME, Component.translatable("item.signpost." + getLangRegistryName(holder.location())));
            return patchBuilder;
        }

        public static DataComponentMap.Builder applyTo(ResourceKey<ModelType> holder, DataComponentMap.Builder builder) {
            builder.set(PostData.TYPE, new PostData(holder, Map.of()));
            builder.set(DataComponents.ITEM_NAME, Component.translatable("item.signpost." + getLangRegistryName(holder.location())));
            return builder;
        }

        public static Optional<ResourceKey<ModelType>> from(Item signItem, HolderLookup.Provider registryAccess) {
            return ModelTypeRegistry.getAllModelTypeHolders(registryAccess)
                .filter(t -> t.value().addSignIngredient.test(new ItemStack(signItem)))
                .findFirst()
                .map(h -> h.unwrapKey().get());
        }

        private static String getLangRegistryName(ResourceLocation id) {
            return REGISTRY_NAME + "." + id.getNamespace() + "." + id.getPath();
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, ModelType> STREAM_CODEC = StreamCodec.composite(
            MaterialType.STREAM_CODEC, ModelType::materialType,
            Ingredient.CONTENTS_STREAM_CODEC, ModelType::addSignIngredient,
            Texture.STREAM_CODEC, ModelType::postTexture,
            Texture.STREAM_CODEC, ModelType::mainTexture,
            Texture.STREAM_CODEC, ModelType::secondaryTexture,
            ModelType::new
        );

        public static final Codec<ModelType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MaterialType.CODEC.fieldOf("materialType").forGetter(ModelType::materialType),
            Ingredient.CODEC.fieldOf("addSignIngredient").forGetter(ModelType::addSignIngredient),
            Texture.CODEC_V2.fieldOf("postTexture").forGetter(ModelType::postTexture),
            Texture.CODEC_V2.fieldOf("mainTexture").forGetter(ModelType::mainTexture),
            Texture.CODEC_V2.fieldOf("secondaryTexture").forGetter(ModelType::secondaryTexture)
        ).apply(instance, ModelType::new));

        public static final Codec<Holder<ModelType>> HOLDER_CODEC = RegistryFixedCodec.create(ModelTypeRegistry.REGISTRY_KEY);
    }

    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final String REGISTRY_NAME = "post";

    public final MaterialType materialType;

    public static enum RequiredTool {
        Axe, Pickaxe
    }

    public enum MaterialType {
        Wood(PropertiesUtil.wood(PropertiesUtil.WoodType.Oak), RequiredTool.Axe, "wood"),
        Stone(PropertiesUtil.STONE, RequiredTool.Pickaxe, "stone"),
        Metal(PropertiesUtil.IRON, RequiredTool.Pickaxe, "metal"),
        Mushroom(PropertiesUtil.mushroom(MapColor.DIRT), RequiredTool.Axe, "mushroom");

        public static final StreamCodec<ByteBuf, MaterialType> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(
                name -> Arrays.stream(values())
                    .filter(mt -> mt.id.equals(name))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid MaterialType name: " + name)),
                mt -> mt.id
            );

        public static final Codec<MaterialType> CODEC = Codec.STRING.xmap(
            name -> Arrays.stream(values())
                .filter(mt -> mt.id.equals(name))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Invalid MaterialType name: " + name)),
            mt -> mt.id
        );

        public final Properties properties;
        public final RequiredTool tool;
        public final String id;
        public final String blockRegistryName;
        private PostBlock block;

        public PostBlock createBlock() {
            assert block == null;
            return block = new PostBlock(this);
        }

        public PostBlock getBlock() {
            assert block != null;
            return block;
        }

        MaterialType(Properties properties, RequiredTool tool, String registryName) {
            this.id = registryName;
            this.blockRegistryName = REGISTRY_NAME + "_" + registryName;
            this.properties = properties
                .setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, blockRegistryName)));
            this.tool = tool;
        }
    }

    public static final Stream<PostBlock> all() {
        return Arrays.stream(MaterialType.values())
            .map(MaterialType::getBlock);
    }

    public PostBlock(MaterialType type) {
        super(type.properties.noOcclusion());
        this.materialType = type;
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack currentStack) {
        super.setPlacedBy(world, pos, state, placer, currentStack);
        ItemStack stack = currentStack.copy(); // stack might be changed in the delay (set block -> block no longer in inventory)
        TileEntityUtils.delayUntilTileEntityExists(world, pos, PostTile.getBlockEntityType(), tile -> {
            tile.setSignpostOwner(Optional.of(PlayerHandle.from(placer)));
            boolean shouldAddNewSign = placer instanceof ServerPlayer;
            if (!world.isClientSide()) {
                var data = stack.get(PostData.TYPE);
                if (data == null) {
                    data = new PostData(switch (materialType) {
                        case Wood -> ResourceKey.create(ModelTypeRegistry.REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "oak"));
                        case Stone -> ResourceKey.create(ModelTypeRegistry.REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "stone"));
                        case Metal -> ResourceKey.create(ModelTypeRegistry.REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "iron"));
                        case Mushroom -> ResourceKey.create(ModelTypeRegistry.REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "mushroom"));
                    }, Map.of());
                }
                tile.readData(data, world.registryAccess(), materialType);
                if (data.parts().isEmpty()){
                    tile.addPart(
                        new BlockPartInstance(new PostBlockPart(tile.modelType().postTexture), Vector3.ZERO),
                        ItemStack.EMPTY,
                        PlayerHandle.from(placer)
                    );
                } else {
                    shouldAddNewSign = false;
                    tile.getWaystonePart().ifPresent(waystone -> WaystoneBlock.registerOwnerAndSeeIfHasName(tile, world, pos, placer, stack));
                }
                tile.setChanged();
                world.sendBlockUpdated(pos, state, state, 3);
                if(shouldAddNewSign)
                    PacketHandler.getInstance().sendToPlayer(
                        (ServerPlayer) placer,
                        RequestSignGui.ForNewSign.Package.from(
                            WorldLocation.from(pos, world),
                            tile.modelTypeKey(),
                            new Vector3(0, 1, 0),
                            ItemStack.EMPTY
                        )
                    );
            }
        }, 100, Optional.of(() -> Signpost.LOGGER.error("Could not initialize placed signpost: BlockEntity never appeared.")));
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

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PostTile(pos, state);
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

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack ret = super.getCloneItemStack(level,  pos, state, includeData);
        var tileOpt = level.getBlockEntity(pos, PostTile.getBlockEntityType());
        tileOpt.ifPresent(tile -> {
            var patchBuilder = DataComponentPatch.builder();
            ModelType.applyTo(tile.modelTypeKey(), patchBuilder);
            if (includeData)
                patchBuilder.set(PostData.TYPE, new PostData(tile.modelTypeKey(), tile.parts()));
            ret.applyComponents(patchBuilder.build());
        });
        return ret;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return RecordCodecBuilder.mapCodec((builder) -> builder.group(
            Codec.STRING.fieldOf("materialType").forGetter(block -> ((PostBlock)block).materialType.id)
        ).apply(builder, variantName ->
            all().filter(v -> Objects.equals(v.materialType.id, variantName)).findAny().orElseThrow()
        ));
    }

}
