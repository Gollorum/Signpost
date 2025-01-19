package gollorum.signpost.minecraft.storage.loot;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.nbt.LootNbtProviderType;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public final class RegisteredWaystoneLootNbtProvider implements NbtProvider {

    public static LootNbtProviderType providerType = new LootNbtProviderType(MapCodec.unit(RegisteredWaystoneLootNbtProvider::new));

    @Nullable
    @Override
    public Tag get(LootContext context) {
        BlockEntity blockEntity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(blockEntity instanceof WaystoneTile waystoneTile) {
            CompoundTag ret = new CompoundTag();
            Optional<WaystoneHandle.Vanilla> handle = waystoneTile.getHandle()
                .or(() -> WaystoneLibrary.getInstance().getHandleByLocation(new WorldLocation(waystoneTile.getBlockPos(), waystoneTile.getLevel())));
            HolderLookup.Provider registries = waystoneTile.getLevel().registryAccess();
            handle.ifPresent(h -> ret.put("Handle", WaystoneHandle.Vanilla.CompoundSerializer.encode(h, registries)));
            waystoneTile.getName()
                .or(() -> handle.flatMap(h -> WaystoneLibrary.getInstance().getData(h).map(d -> d.name)))
                .ifPresent(n -> {
                    CompoundTag displayTag = new CompoundTag();
                    displayTag.putString("Name", Component.Serializer.toJson(Component.literal(n), registries));
                    ret.put("display", displayTag);
                });
            return ret;
        } else return null;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.BLOCK_ENTITY);
    }

    @Override
    public LootNbtProviderType getType() {
        return providerType;
    }

}
