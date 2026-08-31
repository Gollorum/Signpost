package gollorum.signpost.migration;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

/**
 * Turns a pre-2.04 post item stack into a material post item that carries its type as data.
 *
 * <p>The id alone used to say which post type a stack was, so renaming it without writing the type down would
 * quietly turn every stockpiled spruce post into an oak one. This writes the {@code signpost:post_data}
 * component (and the matching {@code minecraft:item_name}) before renaming, producing exactly what the current
 * code would have written for the same stack.
 */
public class PostItemStackFix extends DataFix {

    public PostItemStackFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return writeFixAndRead(
            "Signpost post item type",
            getInputSchema().getType(References.ITEM_STACK),
            getOutputSchema().getType(References.ITEM_STACK),
            PostItemStackFix::fixStack
        );
    }

    private static <T> Dynamic<T> fixStack(Dynamic<T> stack) {
        String id = stack.get("id").asString("");
        LegacyPostTypes.Entry entry = LegacyPostTypes.BY_LEGACY_ID.get(NamespacedSchema.ensureNamespaced(id));
        if (entry == null) return stack;

        Dynamic<T> components = stack.get("components").orElseEmptyMap();
        boolean hadPostData = components.get(Signpost.MOD_ID + ":post_data").result().isPresent();
        Dynamic<T> postData = components.get(Signpost.MOD_ID + ":post_data").orElseEmptyMap()
            .set("ModelType", stack.createString(entry.modelTypeId()));
        if (!hadPostData) {
            // A component written from scratch has to say which PostData version it is, or it is read as
            // version 1 - where the parts are keyed by part type and the model type field does not exist.
            postData = postData.set("DataVersion", stack.createInt(2))
                .set("Parts", stack.emptyMap());
        }
        components = components.set(Signpost.MOD_ID + ":post_data", postData)
            .set("minecraft:item_name", stack.emptyMap()
                .set("translate", stack.createString(PostBlock.ModelType.langKeyFor(Signpost.MOD_ID, entry.modelTypeName()))));

        return stack.set("id", stack.createString(entry.currentBlockId()))
            .set("components", components);
    }

}
