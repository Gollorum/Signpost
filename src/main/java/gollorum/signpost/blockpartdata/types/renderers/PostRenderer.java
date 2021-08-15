package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.blockpartdata.types.Post;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
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

public class PostRenderer extends BlockPartRenderer<Post> {

	private static final Map<ResourceLocation, IBakedModel> cachedModels = new ConcurrentHashMap<>();

	protected IBakedModel makeModel(Post post) {
		return cachedModels
			.computeIfAbsent(post.getTexture(),
				x -> RenderingUtil.loadModel(
					PostModel.postLocation,
					post.getTexture()
				)
			);
	}

	@Override
	public void render(
		Post post,
		TileEntity tileEntity,
		TileEntityRendererDispatcher renderDispatcher,
		MatrixStack matrix,
		IRenderTypeBuffer buffer,
		int combinedLights,
		int combinedOverlay,
		Random random,
		long randomSeed
	) {
		RenderingUtil.render(matrix, renderModel -> renderModel.render(
			makeModel(post),
			tileEntity,
			buffer.getBuffer(RenderType.getSolid()),
			false,
			random,
			randomSeed,
			combinedOverlay,
			new Matrix4f(Quaternion.ONE)
		));
	}

	@Override
	public void renderGui(Post post, Point center, Angle yaw, Angle pitch, float scale, Vector3 offset) {
		RenderingUtil.renderGui(makeModel(post), center, yaw, pitch, scale, offset);
	}
}
