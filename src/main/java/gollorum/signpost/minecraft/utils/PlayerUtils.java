package gollorum.signpost.minecraft.utils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

public class PlayerUtils {

    public static Optional<Block> findBlockLookedAtBy(Player player) {
        HitResult hitResult = player.pick(20, 0, false);
        if(hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            Block block = player.level.getBlockState(blockHit.getBlockPos()).getBlock();
            return Optional.of(block);
        }
        return Optional.empty();
    }

}
