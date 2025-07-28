package gollorum.signpost.minecraft.models;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class WaystoneInPostModel {

    public static final PartDefinition MODEL;

    static {
        var meshDefinition = new MeshDefinition();
        MODEL = meshDefinition.getRoot();
        MODEL.addOrReplaceChild(
            "waystone",
            CubeListBuilder.create()
                .addBox(
                    -3, 0, -3,
                    6, 6, 6,
                    false
                ),
            PartPose.ZERO
        );
    }

}
