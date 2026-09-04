package gollorum.signpost.mixin;

import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.worldgen.WaystoneJigsawPiece;
import gollorum.signpost.worldgen.Villages;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;

/**
 * Guarantees a village waystone in every village.
 *
 * <p>Adding the waystone to the vanilla {@code village/<type>/houses} pools as one more weighted entry
 * (which is what Signpost used to do) only ever makes it <em>possible</em>: those pools carry a combined
 * weight of ~87, so a weight of one means most villages end up without a waystone.
 *
 * <p>Instead, whenever a village asks for a house and has no waystone yet, we hand the jigsaw a pool that
 * contains nothing but the waystone and whose fallback is the houses pool it replaced. One {@code Placer}
 * is created per structure start, so the state below is naturally scoped to a single village.
 *
 * <p>The offer is not always taken: the piece still has to fit, and at {@code depth == maxDepth} vanilla
 * skips the requested pool entirely and uses only the fallback. So rather than swapping once and hoping -
 * which is what the Waystones mod does, and it costs it roughly a third of all villages - we keep
 * offering until the waystone is actually chosen. {@link #signpost$noteWaystoneChosen} sees that happen:
 * the piece is constructed synchronously, inside the same jigsaw connection that was offered the pool,
 * so the flag is already set before the next connection asks for its pool and no village gets two.
 *
 * <p>Only the vanilla village pools have a Signpost counterpart. Repurposed Structures villages fall
 * through here and are covered by its own {@code rs_pieces_spawn_counts} mechanism instead.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement$Placer")
public class JigsawPlacementPlacerInjector {

    @Unique private boolean signpost$hasWaystone = false;

    @ModifyArg(
        method = "tryPlacingChildren",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/Registry;get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;"
        )
    )
    private ResourceKey<StructureTemplatePool> signpost$forceWaystonePool(ResourceKey<StructureTemplatePool> poolKey) {
        if(signpost$hasWaystone) return poolKey;

        IConfig.IServer config = IConfig.IServer.getInstance();
        if(config.isLoaded() && !config.worldGen().isVillageGenerationEnabled()) return poolKey;

        Optional<ResourceKey<StructureTemplatePool>> waystonePool = Villages.waystonePoolFor(poolKey);
        return waystonePool.orElse(poolKey);
    }

    @ModifyArg(
        method = "tryPlacingChildren",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/PoolElementStructurePiece;<init>("
                + "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;"
                + "Lnet/minecraft/world/level/levelgen/structure/pools/StructurePoolElement;"
                + "Lnet/minecraft/core/BlockPos;"
                + "I"
                + "Lnet/minecraft/world/level/block/Rotation;"
                + "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;"
                + "Lnet/minecraft/world/level/levelgen/structure/templatesystem/LiquidSettings;)V"
        ),
        index = 1
    )
    private StructurePoolElement signpost$noteWaystoneChosen(StructurePoolElement element) {
        if(element instanceof WaystoneJigsawPiece) signpost$hasWaystone = true;
        return element;
    }

}
