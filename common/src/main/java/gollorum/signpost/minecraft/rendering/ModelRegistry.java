package gollorum.signpost.minecraft.rendering;

import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.LargeSignBlockPart;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.gui.PostModelResources;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelRegistry<M> {

//	public static ModelRegistry<BlockStateModelPart> LargeBakedSign = new ModelRegistry<>(
//		(mainTexture, secondaryTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.largeLocation, mainTexture, secondaryTexture, modelState
//		),
//		(overlayTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.largeOverlayLocation, overlayTexture, modelState),
//		(mainTexture, secondaryTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.largeFlippedLocation, mainTexture, secondaryTexture, modelState
//		),
//		(overlayTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.largeOverlayFlippedLocation, overlayTexture, modelState
//		),
//		LargeSignBlockPart.class
//	);
//
//	public static ModelRegistry<BlockStateModelPart> WideBakedSign = new ModelRegistry<>(
//		(mainTexture, secondaryTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.wideLocation, mainTexture, secondaryTexture, modelState
//		),
//		(overlayTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.wideOverlayLocation, overlayTexture, modelState
//		),
//		(mainTexture, secondaryTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.wideFlippedLocation, mainTexture, secondaryTexture, modelState
//		),
//		(overlayTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.wideOverlayFlippedLocation, overlayTexture, modelState
//		),
//		SmallWideSignBlockPart.class
//	);
//
//	public static ModelRegistry<BlockStateModelPart> ShortBakedSign = new ModelRegistry<>(
//		(mainTexture, secondaryTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.shortLocation, mainTexture, secondaryTexture, modelState
//		),
//		(overlayTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.shortOverlayLocation, overlayTexture, modelState
//		),
//		(mainTexture, secondaryTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.shortFlippedLocation, mainTexture, secondaryTexture, modelState
//		),
//		(overlayTexture, modelState) -> RenderingUtil.loadModel(
//			PostModelResources.shortOverlayFlippedLocation, overlayTexture, modelState
//		),
//		SmallShortSignBlockPart.class
//	);
//
//	public interface ModelConstructor<M> {
//		M makeModel(Identifier mainTexture, Identifier secondaryTexture, ModelState modelState);
//	}
//
//	public interface OverlayModelConstructor<M> {
//		M makeOverlayModel(Identifier overlayTexture, ModelState modelState);
//	}
//
//	private final Map<Identifier, Map<Identifier, M>> cachedModels = new ConcurrentHashMap<>();
//	private final Map<Identifier, M> cachedOverlayModels = new ConcurrentHashMap<>();
//
//	private final Map<Identifier, Map<Identifier, M>> cachedFlippedModels = new ConcurrentHashMap<>();
//	private final Map<Identifier, M> cachedFlippedOverlayModels = new ConcurrentHashMap<>();
//
//	private final ModelConstructor<M> modelConstructor;
//	private final OverlayModelConstructor<M> overlayModelConstructor;
//
//	private final ModelConstructor<M> flippedModelConstructor;
//	private final OverlayModelConstructor<M> flippedOverlayModelConstructor;
//
//	private final Class<? extends SignBlockPart> signClass;
//
//	public ModelRegistry(
//		ModelConstructor<M> modelConstructor,
//		OverlayModelConstructor<M> overlayModelConstructor,
//		ModelConstructor<M> flippedModelConstructor,
//		OverlayModelConstructor<M> flippedOverlayModelConstructor,
//		Class<? extends SignBlockPart> signClass
//	) {
//		this.modelConstructor = modelConstructor;
//		this.overlayModelConstructor = overlayModelConstructor;
//		this.flippedModelConstructor = flippedModelConstructor;
//		this.flippedOverlayModelConstructor = flippedOverlayModelConstructor;
//		this.signClass = signClass;
//	}
//
//	public M makeModel(SignBlockPart sign) {
//		return (sign.isFlipped() ? cachedFlippedModels : cachedModels)
//			.computeIfAbsent(sign.getMainTexture().identifier(), x -> new ConcurrentHashMap<>())
//			.computeIfAbsent(sign.getSecondaryTexture().identifier(),
//				x -> (sign.isFlipped() ? flippedModelConstructor : modelConstructor)
//					.makeModel(
//						sign.getMainTexture().identifier(),
//						sign.getSecondaryTexture().identifier(),
//						new RotatedModelState(sign.getAngle().get())
//					)
//			);
//	}
//
//	public M makeOverlayModel(SignBlockPart sign, Overlay overlay) {
//		Identifier texture = overlay.textureFor(signClass);
//		return (sign.isFlipped() ? cachedFlippedOverlayModels : cachedOverlayModels)
//			.computeIfAbsent(texture,
//				x -> (sign.isFlipped() ? flippedOverlayModelConstructor : overlayModelConstructor)
//					.makeOverlayModel(texture, new RotatedModelState(sign.getAngle().get())));
//	}

}
