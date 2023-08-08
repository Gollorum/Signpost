package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.minecraft.rendering.FlippableModel;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ModelButton extends ImageButton {

    private final List<GuiModelRenderer> modelRenderers;

    public ModelButton(
        TextureResource background,
        Point point,
        float scale,
        Rect.XAlignment xAlignment,
        Rect.YAlignment yAlignment,
        Function<Rect, Rect> rectBuilder,
        Runnable onPress,
        ModelData... modelData
    ) {
        this(
            background,
            new Rect(point, background.size.scale(scale), xAlignment, yAlignment),
            scale, rectBuilder, b -> onPress.run(), modelData
        );
    }

    private ModelButton(
        TextureResource background,
        Rect rect,
        float scale,
        Function<Rect, Rect> rectBuilder,
        Button.OnPress onPress,
        ModelData... modelData
    ){
        super(
            rect.point.x, rect.point.y,
            rect.width, rect.height,
            0, 0, (int) (background.size.height * scale),
            background.location,
            (int) (background.fileSize.width * scale), (int) (background.fileSize.height * scale),
            onPress
        );
        modelRenderers = new ArrayList<>();
        for(ModelData model: modelData) {
            modelRenderers.add(new GuiModelRenderer(
                rectBuilder.apply(rect),
                model.model,
                model.modelSpaceXOffset,
                model.modelSpaceYOffset,
                model.renderType,
                model.color
            ));
        }
    }

    @Override
    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(matrixStack, mouseX, mouseY, partialTicks);
        for(GuiModelRenderer model : modelRenderers) {
            model.render(matrixStack, mouseX, mouseY, partialTicks);
        }
    }

    public static class ModelData {

        public final FlippableModel model;
        public final float modelSpaceXOffset;
        public final float modelSpaceYOffset;
        public final ItemStack itemStack;
        public final RenderType renderType;
        public final int color;

        public ModelData(FlippableModel model, float modelSpaceXOffset, float modelSpaceYOffset, ItemStack itemStack, RenderType renderType) {
            this(model, modelSpaceXOffset, modelSpaceYOffset, itemStack, renderType, 0xffffff);
        }

        public ModelData(FlippableModel model, float modelSpaceXOffset, float modelSpaceYOffset, ItemStack itemStack, RenderType renderType, int color) {
            this.model = model;
            this.modelSpaceXOffset = modelSpaceXOffset;
            this.modelSpaceYOffset = modelSpaceYOffset;
            this.itemStack = itemStack;
            this.renderType = renderType;
            this.color = color;
        }

    }

}
