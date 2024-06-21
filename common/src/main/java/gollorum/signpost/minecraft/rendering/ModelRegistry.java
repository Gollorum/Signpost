package gollorum.signpost.minecraft.rendering;

import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.LargeSignBlockPart;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.gui.PostModelResources;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelRegistry<M> {

	public static ModelRegistry<BakedModel> LargeBakedSign = new ModelRegistry<>(
		(mainTexture, secondaryTexture) -> RenderingUtil.loadModel(
			PostModelResources.largeLocation, mainTexture, secondaryTexture
		),
		overlayTexture -> RenderingUtil.loadModel(PostModelResources.largeOverlayLocation, overlayTexture),
		(mainTexture, secondaryTexture) -> RenderingUtil.loadModel(
			PostModelResources.largeFlippedLocation, mainTexture, secondaryTexture
		),
		overlayTexture -> RenderingUtil.loadModel(PostModelResources.largeOverlayFlippedLocation, overlayTexture),
		LargeSignBlockPart.class
	);

	public static ModelRegistry<BakedModel> WideBakedSign = new ModelRegistry<>(
		(mainTexture, secondaryTexture) -> RenderingUtil.loadModel(
			PostModelResources.wideLocation, mainTexture, secondaryTexture
		),
		overlayTexture -> RenderingUtil.loadModel(PostModelResources.wideOverlayLocation, overlayTexture),
		(mainTexture, secondaryTexture) -> RenderingUtil.loadModel(
			PostModelResources.wideFlippedLocation, mainTexture, secondaryTexture
		),
		overlayTexture -> RenderingUtil.loadModel(PostModelResources.wideOverlayFlippedLocation, overlayTexture),
		SmallWideSignBlockPart.class
	);

	public static ModelRegistry<BakedModel> ShortBakedSign = new ModelRegistry<>(
		(mainTexture, secondaryTexture) -> RenderingUtil.loadModel(
			PostModelResources.shortLocation, mainTexture, secondaryTexture
		),
		overlayTexture -> RenderingUtil.loadModel(PostModelResources.shortOverlayLocation, overlayTexture),
		(mainTexture, secondaryTexture) -> RenderingUtil.loadModel(
			PostModelResources.shortFlippedLocation, mainTexture, secondaryTexture
		),
		overlayTexture -> RenderingUtil.loadModel(PostModelResources.shortOverlayFlippedLocation, overlayTexture),
		SmallShortSignBlockPart.class
	);

	public interface ModelConstructor<M> {
		M makeModel(ResourceLocation mainTexture, ResourceLocation secondaryTexture);
	}

	public interface OverlayModelConstructor<M> {
		M makeOverlayModel(ResourceLocation overlayTexture);
	}

	private final Map<ResourceLocation, Map<ResourceLocation, M>> cachedModels = new ConcurrentHashMap<>();
	private final Map<ResourceLocation, M> cachedOverlayModels = new ConcurrentHashMap<>();

	private final Map<ResourceLocation, Map<ResourceLocation, M>> cachedFlippedModels = new ConcurrentHashMap<>();
	private final Map<ResourceLocation, M> cachedFlippedOverlayModels = new ConcurrentHashMap<>();

	private final ModelConstructor<M> modelConstructor;
	private final OverlayModelConstructor<M> overlayModelConstructor;

	private final ModelConstructor<M> flippedModelConstructor;
	private final OverlayModelConstructor<M> flippedOverlayModelConstructor;

	private final Class<? extends SignBlockPart> signClass;

	public ModelRegistry(
		ModelConstructor<M> modelConstructor,
		OverlayModelConstructor<M> overlayModelConstructor,
		ModelConstructor<M> flippedModelConstructor,
		OverlayModelConstructor<M> flippedOverlayModelConstructor,
		Class<? extends SignBlockPart> signClass
	) {
		this.modelConstructor = modelConstructor;
		this.overlayModelConstructor = overlayModelConstructor;
		this.flippedModelConstructor = flippedModelConstructor;
		this.flippedOverlayModelConstructor = flippedOverlayModelConstructor;
		this.signClass = signClass;
	}

	public M makeModel(SignBlockPart sign) {
		return (sign.isFlipped() ? cachedFlippedModels : cachedModels)
			.computeIfAbsent(sign.getMainTexture().location(), x -> new ConcurrentHashMap<>())
			.computeIfAbsent(sign.getSecondaryTexture().location(),
				x -> (sign.isFlipped() ? flippedModelConstructor : modelConstructor)
					.makeModel(sign.getMainTexture().location(), sign.getSecondaryTexture().location())
			);
	}

	public M makeOverlayModel(SignBlockPart sign, Overlay overlay) {
		ResourceLocation texture = overlay.textureFor(signClass);
		return (sign.isFlipped() ? cachedFlippedOverlayModels : cachedOverlayModels)
			.computeIfAbsent(texture,
				x -> (sign.isFlipped() ? flippedOverlayModelConstructor : overlayModelConstructor)
					.makeOverlayModel(texture));
	}

}
