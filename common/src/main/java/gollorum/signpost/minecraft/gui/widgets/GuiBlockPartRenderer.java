package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.rendering.GuiFakeCollector;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.mixin.GuiGraphicsMixin;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.math.Angle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;

public class GuiBlockPartRenderer extends AbstractWidget {

    private static final double randomOffset = 0.001;

    private final Collection<BlockPartInstance> partsToRender;
    private final Point center;
    private Angle yaw;
    private Angle pitch;
    private float scale;
    private final Font font;

    public GuiBlockPartRenderer(Collection<BlockPartInstance> partsToRender, Point center, Angle yaw, Angle pitch, float scale, Font font) {
        super(center.x - widthFor(scale) / 2, center.y - heightFor(scale) / 2, widthFor(scale), heightFor(scale), Component.literal(""));
        this.partsToRender = partsToRender;
        this.center = center;
        this.yaw = yaw;
        this.pitch = pitch;
        this.scale = scale;
        this.font = font;
    }

    private static int widthFor(float scale) { return (int)(scale * 1.5f); }
    private static int heightFor(float scale) { return (int)(scale * 1.5f); }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if(isHovered)
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x20ffffff);

        long randomSeed = this.hashCode();
        RandomSource random = RandomSource.create(randomSeed);
        PoseStack ms = new PoseStack();
        var renderState = ((GuiGraphicsMixin)graphics).getGuiRenderState();
        var rect = new ScreenRectangle(getX(), getY(), width, height);
        RenderingUtil.wrapInMatrixEntry(ms, () -> {
            ms.translate(0, 0, 100);
            ms.translate(center.x, center.y, 0);
            ms.scale(scale, -scale, scale);
            ms.mulPose(new Quaternionf(new AxisAngle4f(pitch.radians(), new Vector3f(1, 0, 0))));
            ms.mulPose(new Quaternionf(new AxisAngle4f(yaw.radians(), new Vector3f(0, 1, 0))));
            ms.translate(0, -0.5, 0);
            for(BlockPartInstance bpi : partsToRender) {
                RenderingUtil.wrapInMatrixEntry(ms, () -> {
                    ms.translate(
                        bpi.offset().x() + randomOffset * random.nextDouble(),
                        bpi.offset().y() + randomOffset * random.nextDouble(),
                        bpi.offset().z() + randomOffset * random.nextDouble());
                    BlockPartRenderer.renderDynamic(
                        bpi.blockPart(),
                        Minecraft.getInstance().level,
                        Minecraft.getInstance().player.blockPosition(),
                        ms,
                        new GuiFakeCollector(rect, renderState, font),
                        Minecraft.getInstance().getAtlasManager(),
                        LightCoordsUtil.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        t -> RenderTypes.cutoutMovingBlock(),
                        null
                    );
                });
            }
        });
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double mouseX, double mouseY) {
        yaw = yaw.add(Angle.fromDegrees((float) (mouseX * 3)));
        pitch = pitch.add(Angle.fromDegrees((float) (mouseY * 3)));
        pitch = Angle.fromDegrees(Math.clamp(pitch.degrees(), -90f, 90f));
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_169152_) {

    }
}
