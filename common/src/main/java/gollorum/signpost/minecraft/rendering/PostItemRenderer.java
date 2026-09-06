package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.data.ModelTypeRegistry;
import gollorum.signpost.minecraft.data.PostData;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.NameProvider;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.AngleProvider;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;
import java.util.function.Consumer;

public class PostItemRenderer implements SpecialModelRenderer<PostData> {

    private final PostBlock.MaterialType materialType;
    private final MaterialSet materials;

    public PostItemRenderer(PostBlock.MaterialType materialType, MaterialSet materials) {
        this.materialType = materialType;
        this.materials = materials;
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(-1, 0, -1));
        consumer.accept(new Vector3f(-1, 0, 1));
        consumer.accept(new Vector3f(1, 0, 1));
        consumer.accept(new Vector3f(1, 0, -1));
    }

    public PostData extractArgument(ItemStack itemStack) {
        return itemStack.get(PostData.TYPE);
    }

    @Override
    public void submit(PostData data, ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
        List<BlockPartInstance> parts;
        if (data != null && !data.parts().isEmpty()) {
            parts = new ArrayList<>(data.parts().values());
        } else {
            parts = new ArrayList<>();
            var level = Minecraft.getInstance().level;
            var type = data == null || level == null
                ? ModelTypeRegistry.fallbackModelType(materialType)
                : ModelTypeRegistry.getOrFallbackModelType(
                    level.registryAccess(), data.modelType().orElse(null), () -> materialType);
            parts.add(new BlockPartInstance(new PostBlockPart(type.postTexture()), Vector3.ZERO));
            parts.add(new BlockPartInstance(new SmallWideSignBlockPart(
                new AngleProvider.Literal(Angle.fromDegrees(180)), new NameProvider.Literal(""), true, type.mainTexture(), type.secondaryTexture(),
                    Optional.empty(), Colors.white, Optional.empty(), Optional.empty(), ResourceKey.create(ModelTypeRegistry.REGISTRY_KEY, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "fallback")), false, false
                ),
                new Vector3(0, 0.75f, 0)));
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
                        nodeCollector,
                        materials,
                        packedLight,
                        packedOverlay,
                        displayContext == ItemDisplayContext.GUI
                            ? t -> RenderTypes.solidMovingBlock()
                            : RenderTypes::entityCutout,
                        null
                    );
                });
            }
        });
    }

    public record Unbaked(PostBlock.MaterialType materialType) implements SpecialModelRenderer.Unbaked {

        public static final Identifier NAME = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "post_item");

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                PostBlock.MaterialType.CODEC.fieldOf("fallback").forGetter(Unbaked::materialType)
            ).apply(instance, Unbaked::new)
        );

        @Override
        public SpecialModelRenderer<?> bake(BakingContext context) {
            return new PostItemRenderer(this.materialType, context.materials());
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
