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
import gollorum.signpost.minecraft.data.PostData;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.NameProvider;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.AngleProvider;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

public class PostItemRenderer implements SpecialModelRenderer<PostData> {

    private final PostBlock.ModelType fallbackType;

    public PostItemRenderer(PostBlock.ModelType fallbackType) {
        this.fallbackType = fallbackType;
    }

    @Nullable
    public PostData extractArgument(ItemStack itemStack) {
        var data = itemStack.get(PostData.TYPE);
        if (data == null) {
            var parts = new HashMap<UUID, BlockPartInstance>();
            parts.put(UUID.randomUUID(), new BlockPartInstance(new PostBlockPart(fallbackType.postTexture), Vector3.ZERO));
            parts.put(UUID.randomUUID(), new BlockPartInstance(new SmallWideSignBlockPart(
                new AngleProvider.Literal(Angle.fromDegrees(180)), new NameProvider.Literal(""), true, fallbackType.mainTexture, fallbackType.secondaryTexture,
                Optional.empty(), Colors.white, Optional.empty(), ItemStack.EMPTY, fallbackType, false, false
                ),
                new Vector3(0, 0.75f, 0)));
            data = new PostData(parts);
        }
        return data;
    }

    @Override
    public void render(@Nullable PostData data, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean hasFoilType) {
        List<BlockPartInstance> parts;
        if (data != null) {
            parts = new ArrayList<>(data.parts().values());
        } else {
            parts = new ArrayList<>();
            parts.add(new BlockPartInstance(new PostBlockPart(fallbackType.postTexture), Vector3.ZERO));
            parts.add(new BlockPartInstance(new SmallWideSignBlockPart(
                new AngleProvider.Literal(Angle.fromDegrees(180)), new NameProvider.Literal(""), true, fallbackType.mainTexture, fallbackType.secondaryTexture,
                    Optional.empty(), Colors.white, Optional.empty(), ItemStack.EMPTY, fallbackType, false, false
                ),
                new Vector3(0, 0.75f, 0)));
        }

        RenderingUtil.wrapInMatrixEntry(poseStack, () -> {
            poseStack.translate(0.5, 0, 0.5);
//            switch (displayContext) {
//                case GUI -> {
//                    poseStack.translate(0.5, 0.125, 0.5);
//                    poseStack.mulPose(Axis.XP.rotationDegrees(30));
//                    poseStack.mulPose(Axis.YP.rotationDegrees(45));
//                    poseStack.scale(0.7f, 0.7f, 0.7f);
//                }
//                case FIRST_PERSON_RIGHT_HAND -> {
//                    poseStack.translate(0.5, 0.25, 0.5);
//                    poseStack.mulPose(Axis.YP.rotationDegrees(315));
//                    poseStack.scale(0.45f, 0.45f, 0.45f);
//                }
//                case FIRST_PERSON_LEFT_HAND -> {
//                    poseStack.translate(0.5, 0.25, 0.5);
//                    poseStack.mulPose(Axis.YP.rotationDegrees(35));
//                    poseStack.scale(0.45f, 0.45f, 0.45f);
//                }
//                case THIRD_PERSON_RIGHT_HAND -> {
//                    poseStack.translate(0.5, 0.6, 0.40);
//                    poseStack.mulPose(Axis.XP.rotationDegrees(75));
//                    poseStack.mulPose(Axis.YP.rotationDegrees(315));
//                    poseStack.scale(0.45f, 0.45f, 0.45f);
//                }
//                case THIRD_PERSON_LEFT_HAND -> {
//                    poseStack.translate(0.5, 0.6, 0.40);
//                    poseStack.mulPose(Axis.XP.rotationDegrees(75));
//                    poseStack.mulPose(Axis.YP.rotationDegrees(35));
//                    poseStack.scale(0.45f, 0.45f, 0.45f);
//                }
//                case GROUND -> {
//                    poseStack.translate(0.5, 0.0, 0.5);
//                    poseStack.scale(0.25f, 0.25f, 0.25f);
//                }
//                case FIXED -> {
//                    poseStack.translate(0.5, 0.25, 0.5);
//                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
//                    poseStack.scale(0.5f, 0.5f, 0.5f);
//                }
//            }
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
                        bufferSource,
                        packedLight,
                        packedOverlay,
                        false
                    );
                });
            }

            if(bufferSource instanceof MultiBufferSource.BufferSource) ((MultiBufferSource.BufferSource) bufferSource).endBatch();
        });
    }

    public record Unbaked(PostBlock.ModelType fallbackType) implements SpecialModelRenderer.Unbaked {

        public static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "post_item");

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                PostBlock.ModelType.CODEC.fieldOf("fallback").forGetter(Unbaked::fallbackType)
            ).apply(instance, Unbaked::new)
        );

        @Override
        public SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
            return new PostItemRenderer(this.fallbackType);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
