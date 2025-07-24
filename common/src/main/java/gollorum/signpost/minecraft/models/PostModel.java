package gollorum.signpost.minecraft.models;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class PostModel {

    public static PartDefinition createPostModel() {
        var meshDefinition = new MeshDefinition();
        var post = meshDefinition.getRoot();
        post.addOrReplaceChild(
            "post",
            CubeListBuilder.create()
                .addBox(
                    -2, 0, -2,
                    4, 16, 4,
                    false
                ),
            PartPose.ZERO
        );
        return post;
    }

}
