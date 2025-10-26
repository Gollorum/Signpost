package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.WaystoneBlockPart;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.minecraft.models.WaystoneInPostModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.minecraft.rendering.TexturedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.function.Function;

public class WaystoneRenderer extends BlockPartRenderer<WaystoneBlockPart> {


	@Override
	public void render(
		WaystoneBlockPart part,
        Level level,
        BlockPos pos,
		PoseStack blockToView,
		MultiBufferSource buffer,
		int combinedLights,
		int combinedOverlay,
        Function<ResourceLocation, RenderType> renderTypeFactory
    ) {
		RenderingUtil.render(
			blockToView,
			new TexturedModel(
				WaystoneInPostModel.MODEL,
				TextureResource.waystoneTextureLocation.toMaterial(),
				Colors.white
			),
			buffer,
			combinedLights,
			combinedOverlay,
            renderTypeFactory
		);
	}
}
