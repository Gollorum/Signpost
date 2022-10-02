package gollorum.signpost.minecraft.storage.loot;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.registry.NbtProviderRegistry;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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

    public static LootNbtProviderType createProviderType() { return new LootNbtProviderType(new Serializer()); };

    @Nullable
    @Override
    public Tag get(LootContext context) {
        BlockEntity blockEntity = context.getParam(LootContextParams.BLOCK_ENTITY);
        if(blockEntity instanceof WaystoneTile) {
            WaystoneTile waystoneTile = (WaystoneTile) blockEntity;
            CompoundTag ret = new CompoundTag();
            Optional<WaystoneHandle.Vanilla> handle = waystoneTile.getHandle()
                .or(() -> WaystoneLibrary.getInstance().getHandleByLocation(new WorldLocation(waystoneTile.getBlockPos(), waystoneTile.getLevel())));
            handle.ifPresent(h -> ret.put("Handle", WaystoneHandle.Vanilla.Serializer.write(h)));
            waystoneTile.getName()
                .or(() -> handle.flatMap(h -> WaystoneLibrary.getInstance().getData(h).map(d -> d.name)))
                .ifPresent(n -> {
                    CompoundTag displayTag = new CompoundTag();
                    displayTag.putString("Name", Component.Serializer.toJson(Component.literal(n)));
                    ret.put("display", displayTag);
                });
            return ret;
        } else return null;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.BLOCK_ENTITY);
    }

    @Override
    public LootNbtProviderType getType() {
        return NbtProviderRegistry.RegisteredWaystone.get();
    }

    public static final class Serializer implements net.minecraft.world.level.storage.loot.Serializer<RegisteredWaystoneLootNbtProvider> {

        @Override
        public void serialize(JsonObject jsonObject, RegisteredWaystoneLootNbtProvider instance, JsonSerializationContext context) { }

        @Override
        public RegisteredWaystoneLootNbtProvider deserialize(JsonObject jsonObject, JsonDeserializationContext context) {
            return new RegisteredWaystoneLootNbtProvider();
        }
    }

}
