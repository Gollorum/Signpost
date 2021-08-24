package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.modelGeneration.SignModel;
import gollorum.signpost.utils.modelGeneration.SignModelFactory;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class PostRenderer extends BlockPartRenderer<PostBlockPart> {

	private final boolean shouldRenderBaked = true;

	private static final Map<ResourceLocation, SignModel> cachedModels = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, IBakedModel> cachedBakedModels = new ConcurrentHashMap<>();

	private SignModel makeModel(PostBlockPart post) {
		return cachedModels
			.computeIfAbsent(post.getTexture(),
				x -> new SignModelFactory<ResourceLocation>()
					.makePost(x)
					.build(new SignModel(), SignModel::addCube)
			);
	}

	private IBakedModel makeBakedModel(PostBlockPart post) {
		return cachedBakedModels
			.computeIfAbsent(post.getTexture(),
				x -> RenderingUtil.loadModel(
					PostModel.postLocation,
					post.getTexture()
				)
			);
	}

	@Override
	public void render(
		PostBlockPart post,
		TileEntity tileEntity,
		TileEntityRendererDispatcher renderDispatcher,
		MatrixStack matrix,
		IRenderTypeBuffer buffer,
		int combinedLights,
		int combinedOverlay,
		Random random,
		long randomSeed
	) {
		RenderingUtil.render(matrix, renderModel -> {
			if(shouldRenderBaked)
				renderModel.render(
					makeBakedModel(post),
					tileEntity,
					buffer.getBuffer(RenderType.getSolid()),
					false,
					random,
					randomSeed,
					combinedOverlay,
					new Matrix4f(Quaternion.ONE)
				);
			else makeModel(post).render(
				matrix.getLast(),
				buffer,
				RenderType.getSolid(),
				combinedLights,
				combinedOverlay,
				1, 1, 1
			);
		});
	}

	@Override
	public void renderGui(PostBlockPart post, Point center, Angle yaw, Angle pitch, float scale, Vector3 offset) {
		RenderingUtil.renderGui(makeBakedModel(post), center, yaw, pitch, scale, offset);
	}
}
