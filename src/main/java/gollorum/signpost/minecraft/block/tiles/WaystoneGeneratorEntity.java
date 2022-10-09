package gollorum.signpost.minecraft.block.tiles;

import com.mojang.datafixers.types.Type;
import gollorum.signpost.minecraft.block.WaystoneGeneratorBlock;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.registry.BlockRegistry;
import gollorum.signpost.utils.Delay;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class WaystoneGeneratorEntity extends BlockEntity {

    public static final String REGISTRY_NAME = "waystone_generator";

    private static BlockEntityType<WaystoneGeneratorEntity> type = null;
    public static BlockEntityType<WaystoneGeneratorEntity> createType() {
        assert type == null;
        Type<?> type = Util.fetchChoiceType(References.BLOCK_ENTITY, REGISTRY_NAME);
        return WaystoneGeneratorEntity.type = BlockEntityType.Builder.of(
            WaystoneGeneratorEntity::new,
            BlockRegistry.WaystoneGenerator.get()
        ).build(type);
    }
    public static BlockEntityType<WaystoneGeneratorEntity> getBlockEntityType() {
        assert type != null;
        return type;
    }


    public WaystoneGeneratorEntity(BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);
        if(!Config.Server.worldGen.debugMode.get() && level instanceof ServerLevel l)
            Delay.onServerForFrames(1, () -> WaystoneGeneratorBlock.generate(getBlockState(), getBlockPos(), l));
    }
}
