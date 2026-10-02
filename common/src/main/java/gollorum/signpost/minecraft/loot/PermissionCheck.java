package gollorum.signpost.minecraft.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.platform.Services;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class PermissionCheck implements LootItemCondition {

    public static enum Type {
        CanPickWaystone("pick_waystone");

        public final String name;
        Type(String name) { this.name = name; }
    }

    // Not "type": since 26.3 that is the key LootItemCondition dispatches on, so a field of the same name collides
    // with it. This is loot table JSON the mod generates itself, not save data, so the name is free to change.
    public static MapCodec<PermissionCheck> createConditionCodec() {
        return Codec.STRING.fieldOf("permission").flatXmap(
        str -> Arrays.stream(Type.values()).filter(t -> t.name.equals(str)).findFirst().map(PermissionCheck::new).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown permission check type: " + str)),
        check -> DataResult.success(check.type.name)
    ); }

    private final Type type;

    public PermissionCheck(Type type) {
        this.type = type;
    }

    @Override
    public MapCodec<? extends LootItemCondition> codec() {
        return Services.LOOT_ITEM_CONDITION_REGISTRY.getPermissionCheck();
    }

    @Override
    public boolean test(LootContext lootContext) {
        if(!lootContext.hasParameter(LootContextParams.LAST_DAMAGE_PLAYER)) return true;
        Player thisEntity = lootContext.getOptional(LootContextParams.LAST_DAMAGE_PLAYER);
        if(thisEntity.permissions().hasPermission(IConfig.IServer.getInstance().permissions().pickUnownedWaystonePermission())) return true;

        if(!lootContext.hasParameter(LootContextParams.BLOCK_ENTITY)) return false;
        BlockEntity blockEntity = lootContext.getOptional(LootContextParams.BLOCK_ENTITY);
        if(!(blockEntity instanceof WaystoneTile)) return false;
        WaystoneTile waystoneTile = (WaystoneTile) blockEntity;
        return waystoneTile.getWaystoneOwner()
            .map(owner -> owner.equals(PlayerHandle.from(thisEntity)))
            .orElse(true);
    }

    public static class Builder implements LootItemCondition.Builder {

        private final Type type;

        public Builder(Type type) {
            this.type = type;
        }

        @Override
        public @NotNull PermissionCheck build() {
            return new PermissionCheck(type);
        }
    }

}
