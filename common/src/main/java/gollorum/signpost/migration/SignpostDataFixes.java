package gollorum.signpost.migration;

import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.fixes.BlockRenameFix;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

/**
 * Signpost's fixes, hung off the end of Minecraft's own data fixer chain by
 * {@code gollorum.signpost.mixin.DataFixersInjector}.
 *
 * <p>A data fixer only runs when the save's <em>Minecraft</em> data version is behind the current one, which
 * is why this could not have worked for an in-place mod update. It works here because Signpost never shipped
 * for this Minecraft version: every world that can hold the pre-2.04 post ids was written by an older
 * Minecraft, so the chain runs and these fixes get their turn at the end of it.
 *
 * <p>Both fixes are idempotent - the rename map has no entry whose value is also a key - so a save that
 * somehow passes through twice is unharmed.
 */
public final class SignpostDataFixes {

    public static void addTo(DataFixerBuilder builder) {
        // Sub-version 1 rather than 0: it sorts after any schema Minecraft itself might one day register at
        // the current data version, and a duplicate key would silently replace that one.
        Schema schema = builder.addSchema(
            SharedConstants.getCurrentVersion().getDataVersion().getVersion(), 1,
            NamespacedSchema::new);
        builder.addFixer(BlockRenameFix.create(schema, "Signpost post block types", LegacyPostTypes::rename));
        builder.addFixer(new PostItemStackFix(schema));
    }

    private SignpostDataFixes() {}

}
