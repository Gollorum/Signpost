package gollorum.signpost.minecraft.utils.tints;

import gollorum.signpost.Signpost;
import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ResourceLocationSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;

import java.util.Objects;

public record BlockColorTint(Block block, int tintIndex) implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return net.minecraft.client.Minecraft.getInstance().getBlockColors().getColor(block.defaultBlockState(), level, pos, tintIndex);
    }

    public static void register() {
        Tint.Serialization.register("blockColor", compoundSerializer);
    }

    private static Registry<Block> getBlockRegistry() {
        return Signpost.getServerInstance().registryAccess().get(Registries.BLOCK).get().value();
    }

    private static Block getBlock(ResourceLocation key) {
        return getBlockRegistry().get(key).get().value();
    }

    private static ResourceLocation getKey(Block block) {
        return Objects.requireNonNull(getBlockRegistry().getKey(block));
    }

    public static final CompoundSerializable<BlockColorTint> compoundSerializer = new CompoundSerializable<BlockColorTint>() {

        @Override
        public void encode(CompoundTag compound, BlockColorTint tint, HolderLookup.Provider provider) {
            ResourceLocationSerializer.Instance.encode(compound, getKey(tint.block), provider);
            compound.putInt("TintIndex", tint.tintIndex);
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return ResourceLocationSerializer.Instance.isContainedIn(compound);
        }

        @Override
        public BlockColorTint decode(CompoundTag compound, HolderLookup.Provider provider) {
            return new BlockColorTint(
                getBlock(ResourceLocationSerializer.Instance.decode(compound, provider)),
                compound.getInt("TintIndex")
            );
        }
    };

    public static final BufferSerializable<BlockColorTint> bufferSerializer = new BufferSerializable<BlockColorTint>() {

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, BlockColorTint tint) {
            ResourceLocationSerializer.Instance.encode(buffer, getKey(tint.block));
            buffer.writeInt(tint.tintIndex);
        }

        @Override
        public BlockColorTint decode(RegistryFriendlyByteBuf buffer) {
            return new BlockColorTint(
                getBlock(ResourceLocationSerializer.Instance.decode(buffer)),
                buffer.readInt()
            );
        }

        @Override
        public Class<BlockColorTint> getTargetClass() {
            return BlockColorTint.class;
        }
    };

}
