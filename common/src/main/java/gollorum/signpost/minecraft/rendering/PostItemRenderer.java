package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.data.PostData;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.Either;
import gollorum.signpost.utils.NameProvider;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.AngleProvider;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class PostItemRenderer implements SpecialModelRenderer<PostData> {

    private final PostBlock.ModelType fallbackType;

    public PostItemRenderer(PostBlock.ModelType fallbackType) {
        this.fallbackType = fallbackType;
    }

    @Nullable
    public PostData extractArgument(ItemStack itemStack) {
        return itemStack.get(PostData.TYPE);
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

            for (BlockPartInstance now: parts) {
                RenderingUtil.wrapInMatrixEntry(poseStack, () ->
                    BlockPartRenderer.renderGuiDynamic(
                        now.blockPart(), poseStack, now.offset(), bufferSource, packedLight, packedOverlay
                    ));
            }

            if(bufferSource instanceof MultiBufferSource.BufferSource) ((MultiBufferSource.BufferSource) bufferSource).endBatch();
        });
    }

    public record Unbaked(PostBlock.ModelType fallbackType) implements SpecialModelRenderer.Unbaked {

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
