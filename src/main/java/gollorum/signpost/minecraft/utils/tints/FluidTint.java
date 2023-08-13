package gollorum.signpost.minecraft.utils.tints;

import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ResourceLocationSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

public record FluidTint(Fluid fluid) implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return fluid.getAttributes().getColor(level, pos);
    }

    public static void register() {
        Tint.Serialization.register("fluid", serializer);
    }

    public static final CompoundSerializable<FluidTint> serializer = new CompoundSerializable<>() {
        @Override
        public CompoundTag write(FluidTint fluidTint, CompoundTag compound) {
            ResourceLocationSerializer.Instance.write(ForgeRegistries.FLUIDS.getKey(fluidTint.fluid), compound);
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return ResourceLocationSerializer.Instance.isContainedIn(compound);
        }

        @Override
        public FluidTint read(CompoundTag compound) {
            return new FluidTint(ForgeRegistries.FLUIDS.getValue(ResourceLocationSerializer.Instance.read(compound)));
        }

        @Override
        public void write(FluidTint fluidTint, FriendlyByteBuf buffer) {
            ResourceLocationSerializer.Instance.write(ForgeRegistries.FLUIDS.getKey(fluidTint.fluid), buffer);
        }

        @Override
        public FluidTint read(FriendlyByteBuf buffer) {
            return new FluidTint(ForgeRegistries.FLUIDS.getValue(ResourceLocationSerializer.Instance.read(buffer)));
        }

        @Override
        public Class<FluidTint> getTargetClass() {
            return FluidTint.class;
        }
    };

}
