package gollorum.signpost.minecraft.models;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class PostModel {

    public static final PartDefinition MODEL;
    static {
        var meshDefinition = new MeshDefinition();
        MODEL = meshDefinition.getRoot();
        MODEL.addOrReplaceChild(
            "post",
            CubeListBuilder.create()
                .texOffs(0, -4)
                .addBox(
                    -2, 0, -2,
                    4, 16, 4,
                    false
                ),
            PartPose.ZERO
        );
    }

}
