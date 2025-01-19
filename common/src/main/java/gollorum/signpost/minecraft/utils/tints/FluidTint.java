package gollorum.signpost.minecraft.utils.tints;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.gui.utils.IFluidTextureProvider;
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
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;

public record FluidTint(Fluid fluid) implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return IFluidTextureProvider.getInstance().getTintColor(fluid.defaultFluidState(), level, pos);
    }

    public static void register() {
        Tint.Serialization.register("fluid", compoundSerializer);
    }

    private static Registry<Fluid> getFluidRegistry() {
        assert Signpost.getServerType().isServer;
        return Signpost.getServerInstance().registryAccess().get(Registries.FLUID).get().value();
    }

    public static final CompoundSerializable<FluidTint> compoundSerializer = new CompoundSerializable<>() {

        @Override
        public void encode(CompoundTag compound, FluidTint fluidTint, HolderLookup.Provider provider) {
            ResourceLocationSerializer.Instance.encode(compound, getFluidRegistry().getKey(fluidTint.fluid), provider);
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return ResourceLocationSerializer.Instance.isContainedIn(compound);
        }

        @Override
        public FluidTint decode(CompoundTag compound, HolderLookup.Provider provider) {
            return new FluidTint(getFluidRegistry().get(ResourceLocationSerializer.Instance.decode(compound, provider)).get().value());
        }
    };

    public static final BufferSerializable<FluidTint> bufferSerializer = new BufferSerializable<>() {

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FluidTint fluidTint) {
            ResourceLocationSerializer.Instance.encode(buffer, getFluidRegistry().getKey(fluidTint.fluid));
        }

        @Override
        public FluidTint decode(RegistryFriendlyByteBuf buffer) {
            return new FluidTint(getFluidRegistry().get(ResourceLocationSerializer.Instance.decode(buffer)).get().value());
        }

        @Override
        public Class<FluidTint> getTargetClass() {
            return FluidTint.class;
        }
    };

}
