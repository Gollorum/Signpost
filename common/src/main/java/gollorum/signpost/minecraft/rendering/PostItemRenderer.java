package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.data.ModelTypeRegistry;
import gollorum.signpost.minecraft.data.PostData;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.utils.AngleProvider;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.NameProvider;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 1.21.1 has no {@code special} item model and no {@code SpecialModelRenderer}: an item that wants to
 * draw a block entity's model uses a {@link BlockEntityWithoutLevelRenderer}, referenced from an item
 * model with {@code "parent": "builtin/entity"} and registered per loader. There is therefore no
 * {@code Unbaked} variant and no model json to carry the fallback material - it comes from the item's
 * own block instead.
 */
public class PostItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static PostItemRenderer instance;
    public static PostItemRenderer getInstance() {
        if(instance == null) instance = new PostItemRenderer();
        return instance;
    }

    private PostItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static List<BlockPartInstance> fallbackParts(PostBlock.ModelType type) {
        var parts = new ArrayList<BlockPartInstance>();
        parts.add(new BlockPartInstance(new PostBlockPart(type.postTexture()), Vector3.ZERO));
        parts.add(new BlockPartInstance(new SmallWideSignBlockPart(
            new AngleProvider.Literal(Angle.fromDegrees(180)), new NameProvider.Literal(""), true,
            type.mainTexture(), type.secondaryTexture(),
            Optional.empty(), Colors.white, Optional.empty(), Optional.empty(),
            ResourceKey.create(ModelTypeRegistry.REGISTRY_KEY,
                ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "fallback")),
            false, false
            ),
            new Vector3(0, 0.75f, 0)));
        return parts;
    }

    @Override
    public void renderByItem(
        ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
        MultiBufferSource buffer, int packedLight, int packedOverlay
    ) {
        if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof PostBlock postBlock)) {
            Signpost.LOGGER.error("Tried to render a non-post item with the post renderer");
            super.renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
            return;
        }

        var materialType = postBlock.materialType;
        var data = stack.get(PostData.TYPE);
        List<BlockPartInstance> parts;
        if (data != null && !data.parts().isEmpty()) {
            parts = new ArrayList<>(data.parts().values());
        } else {
            var level = Minecraft.getInstance().level;
            var type = data == null || level == null
                ? ModelTypeRegistry.fallbackModelType(materialType)
                : ModelTypeRegistry.getOrFallbackModelType(
                    level.registryAccess(), data.modelType().orElse(null), () -> materialType);
            parts = fallbackParts(type);
        }

        RenderingUtil.wrapInMatrixEntry(poseStack, () -> {
            poseStack.translate(0.5, 0, 0.5);
            switch (displayContext) {
                case GUI -> {
                    poseStack.mulPose(Axis.YP.rotationDegrees(90));
                    poseStack.scale(0.9f, 0.9f, 0.9f);
                }
                case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                    poseStack.mulPose(Axis.YP.rotationDegrees(270));
                }
                case THIRD_PERSON_LEFT_HAND -> {
                    poseStack.mulPose(Axis.YP.rotationDegrees(90));
                }
                case FIXED -> {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                }
            }

            for (BlockPartInstance now: parts) {
                RenderingUtil.wrapInMatrixEntry(poseStack, () -> {
                    poseStack.translate(now.offset().x(), now.offset().y(), now.offset().z());
                    BlockPartRenderer.renderDynamic(
                        now.blockPart(),
                        Minecraft.getInstance().level,
                        Minecraft.getInstance().player.blockPosition(),
                        poseStack,
                        buffer,
                        packedLight,
                        packedOverlay,
                        displayContext == ItemDisplayContext.GUI
                            ? t -> RenderType.solid()
                            : RenderType::entityCutout
                    );
                });
            }
        });
    }
}
