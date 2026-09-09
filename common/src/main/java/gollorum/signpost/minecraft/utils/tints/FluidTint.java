package gollorum.signpost.minecraft.utils.tints;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import gollorum.signpost.minecraft.gui.utils.IFluidTextureProvider;
import gollorum.signpost.utils.Tint;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;

public record FluidTint(Fluid fluid) implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return IFluidTextureProvider.getInstance().getTintColor(fluid.defaultFluidState(), level, pos);
    }

    public static void register() {
        Tint.Serialization.register("fluid", new Serializer(FluidTint.class, CODEC, STREAM_CODEC));
    }

    private static Registry<Fluid> getFluidRegistry() {
        return BuiltInRegistries.FLUID;
    }

    public static final MapCodec<FluidTint> CODEC = Codec.STRING.fieldOf("ResourceLocation").xmap(
        s -> new FluidTint(getFluidRegistry().get(Identifier.parse(s)).get().value()),
        t -> getFluidRegistry().getKey(t.fluid).toString()
    );

    public static final StreamCodec<ByteBuf, FluidTint> STREAM_CODEC = Identifier.STREAM_CODEC.map(
        rl -> new FluidTint(getFluidRegistry().getValue(rl)),
        t -> getFluidRegistry().getKey(t.fluid)
    );

}
