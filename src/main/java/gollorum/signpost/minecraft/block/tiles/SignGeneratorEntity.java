package gollorum.signpost.minecraft.block.tiles;

import com.mojang.datafixers.types.Type;
import gollorum.signpost.minecraft.block.SignGeneratorBlock;
import gollorum.signpost.minecraft.block.WaystoneGeneratorBlock;
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

public class SignGeneratorEntity extends BlockEntity {

    public static final String REGISTRY_NAME = "sign_generator";

    private static BlockEntityType<SignGeneratorEntity> type = null;
    public static BlockEntityType<SignGeneratorEntity> createType() {
        assert type == null;
        Type<?> type = Util.fetchChoiceType(References.BLOCK_ENTITY, REGISTRY_NAME);
        return SignGeneratorEntity.type = BlockEntityType.Builder.of(
            SignGeneratorEntity::new,
            BlockRegistry.SignGenerator.get()
        ).build(type);
    }
    public static BlockEntityType<SignGeneratorEntity> getBlockEntityType() {
        assert type != null;
        return type;
    }

    private final boolean shouldInstantlyGenerate = true;

    public SignGeneratorEntity(BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);
        if(shouldInstantlyGenerate && level instanceof ServerLevel l)
            Delay.onServerForFrames(1, () -> SignGeneratorBlock.generate(this, l));
    }

}
