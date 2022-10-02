package gollorum.signpost.minecraft.storage.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.registry.LootItemConditionRegistry;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class PermissionCheck implements LootItemCondition {

    public static enum Type {
        CanPickWaystone("pick_waystone");

        public final String name;
        Type(String name) { this.name = name; }
    }

    public static LootItemConditionType createConditionType() { return new LootItemConditionType(new Serializer()); }

    private final Type type;

    public PermissionCheck(Type type) {
        this.type = type;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditionRegistry.permissionCheck.get();
    }

    @Override
    public boolean test(LootContext lootContext) {
        if(!lootContext.hasParam(LootContextParams.THIS_ENTITY)) return true;
        Entity thisEntity = lootContext.getParam(LootContextParams.THIS_ENTITY);
        if(thisEntity.hasPermissions(Config.Server.permissions.pickUnownedWaystonePermissionLevel.get())) return true;

        if(!lootContext.hasParam(LootContextParams.BLOCK_ENTITY)) return false;
        BlockEntity blockEntity = lootContext.getParam(LootContextParams.BLOCK_ENTITY);
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

    private static final class Serializer implements net.minecraft.world.level.storage.loot.Serializer<PermissionCheck> {

        @Override
        public void serialize(JsonObject jsonObject, PermissionCheck instance, JsonSerializationContext context) {
            jsonObject.addProperty("type", instance.type.name);
        }

        @Override
        public PermissionCheck deserialize(JsonObject jsonObject, JsonDeserializationContext context) {
            String type = GsonHelper.getAsString(jsonObject, "type");
            return new PermissionCheck(Arrays.stream(Type.values()).filter(t -> t.name.equals(type)).findFirst().get());
        }
    }

}
