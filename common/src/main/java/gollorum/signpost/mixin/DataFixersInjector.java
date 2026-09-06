package gollorum.signpost.mixin;

import com.mojang.datafixers.DataFixerBuilder;
import gollorum.signpost.migration.SignpostDataFixes;
import net.minecraft.util.datafix.DataFixers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Appends Signpost's fixes to Minecraft's data fixer chain.
 *
 * <p>None of the three loaders offers a data fixer API any more, so this is the only way in that works the
 * same everywhere. The injection point is the tail of {@code addFixers}, which runs inside
 * {@code DataFixers.<clinit>} before the builder is built - the last moment at which a fixer can still be
 * added.
 */
@Mixin(DataFixers.class)
public class DataFixersInjector {

    @Inject(method = "addFixers", at = @At("TAIL"))
    private static void signpost$addFixers(DataFixerBuilder builder, CallbackInfo ci) {
        SignpostDataFixes.addTo(builder);
    }

}
