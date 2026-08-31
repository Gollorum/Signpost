package gollorum.signpost.migration;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;

public class SignpostDataFixers {
    public static DataFixer create() {
        Schema parent = null; // Use null or a default schema if available
        Schema schema = new Schema(0, parent);
        DataFixerBuilder builder = new DataFixerBuilder(0);
        builder.addFixer(new PostBlockItemVariantFix(schema));
        builder.addFixer(new PostBlockVariantFix(schema));
        return builder.build().fixer();
    }
}
