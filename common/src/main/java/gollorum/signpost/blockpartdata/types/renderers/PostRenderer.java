package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.models.PostModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.minecraft.rendering.TexturedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.function.Function;

public class PostRenderer extends BlockPartRenderer<PostBlockPart> {

	@Override
	public void render(
        PostBlockPart post,
        Level level,
        BlockPos pos,
        PoseStack blockToView,
        SubmitNodeCollector nodeCollector,
        MaterialSet materials,
        int combinedLights,
        int combinedOverlay,
        Function<ResourceLocation, RenderType> renderTypeFactory,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
		RenderingUtil.render(
            blockToView,
			new TexturedModel(
				PostModel.MODEL,
				post.getTexture().toMaterial(),
				post.getTexture().tint().map(tint -> tint.getColorAt(level, pos)).orElse(Colors.white)
			),
            nodeCollector,
            materials,
            combinedLights,
            combinedOverlay,
            renderTypeFactory,
            crumblingOverlay
        );
	}

}
