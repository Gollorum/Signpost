package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import gollorum.signpost.minecraft.gui.utils.*;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DropDownSelection<EntryType> extends ImageButton {

    public final Rect rect;

    private static final TextureResource texture = TextureResource.expandContract;
    public static final TextureSize size = new TextureSize(11, 11);
    private final Consumer<List> onShow;
    private final Consumer<List> onHide;
    private final Consumer<EntryType> onSelectionChanged;
    private final Font fontRenderer;

    private boolean isListVisible;
    private final List list;

    private final java.util.List<EntryType> allEntries = new ArrayList<>();
    private Predicate<EntryType> filter = b -> true;
    private int selectedIndex;
    private final boolean shouldHighlightSelected;

    public Collection<EntryType> getAllEntries() { return allEntries; }

    public Optional<EntryType> getSelectedEntry() {
        List.Entry selectedEntry = list.getSelected();
        if(selectedEntry == null)
            return Optional.empty();
        else return Optional.of(selectedEntry.content);
    }

    public void setFilter(Predicate<EntryType> filter) {
        this.filter = filter;
        list.updateContent();
    }

    public DropDownSelection(
        Font fontRenderer,
        Point position,
        Rect.XAlignment xAlignment,
        Rect.YAlignment yAlignment,
        int width, int height,
        int yOffset,
        Consumer<List> onShow,
        Consumer<List> onHide,
        Consumer<EntryType> onSelectionChanged,
        boolean shouldHighlightSelected
    ) { this(
        fontRenderer,
        new Rect(position, size, xAlignment, yAlignment),
        width, height,
        yOffset,
        onShow, onHide, onSelectionChanged,
        shouldHighlightSelected
    ); }

    private DropDownSelection(
        Font fontRenderer,
        Rect rect, int width, int height, int yOffset,
        Consumer<List> onShow, Consumer<List> onHide, Consumer<EntryType> onSelectionChanged,
        boolean shouldHighlightSelected
    ){
        super(rect.point.x, rect.point.y, rect.width, rect.height, new WidgetSprites(texture.location, texture.location), b -> ((DropDownSelection)b).toggle());
        this.rect = rect;
        this.fontRenderer = fontRenderer;
        this.shouldHighlightSelected = shouldHighlightSelected;
        list = new List(Minecraft.getInstance(), new Point(rect.point.x + size.width, rect.point.y + size.height + yOffset), width, height);
        this.onSelectionChanged = onSelectionChanged;
        this.onShow = onShow;
        this.onHide = onHide;
    }

    public void addEntry(EntryType text) {
        if(!allEntries.contains(text)) {
            allEntries.add(text);
            list.updateContent();
        }
    }

    public void removeEntry(EntryType text) {
        if(allEntries.contains(text)) {
            allEntries.remove(text);
            list.updateContent();
        }
    }

    public void setEntries(Collection<EntryType> entries) {
        allEntries.clear();
        addEntries(entries);
    }

    public void addEntries(Collection<EntryType> entries) {
        allEntries.addAll(entries);
        list.updateContent();
    }

    public void toggle(){
        if(isListVisible) {
            isListVisible = false;
            onHide.accept(list);
        } else {
            isListVisible = true;
            onShow.accept(list);
        }
    }

    public void showList(){
        if(!isListVisible) {
            isListVisible = true;
            onShow.accept(list);
        }
    }

    public void hideList(){
        if(isListVisible) {
            isListVisible = false;
            onHide.accept(list);
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);
        RenderSystem.disableDepthTest();
        int yTexStart = this.isHovered ? texture.size.height : 0;
        int xTexStart = this.isListVisible ? texture.size.width : 0;

        graphics.blit(RenderType::guiTextured,
            texture.location,
            getX(), getY(),
            xTexStart, yTexStart,
            this.width, this.height,
            texture.fileSize.height, texture.fileSize.width
        );
        RenderSystem.enableDepthTest();
        if(isListVisible) list.render(graphics, mouseX, mouseY, partialTicks);
        graphics.pose().popPose();
    }

    public class List extends ObjectSelectionList<List.Entry> {

        private final int rimHeight;

        public List(Minecraft minecraft, Point topRight, int width, int height) {
            this(minecraft, topRight, width, height, minecraft.font.lineHeight);
        }

        public List(Minecraft minecraft, Point topRight, int width, int height, int rimHeight) {
            super(minecraft, width, height, topRight.y + rimHeight, 14);
            this.setX(topRight.x - width);
//            super(minecraft, width, height, topRight.x - width, topRight.y + rimHeight);
            this.setRenderHeader(false, 0);
            headerHeight = 14;
            this.rimHeight = rimHeight;
            updateContent();
        }

        public void updateContent() {
            this.replaceEntries(allEntries.stream().filter(filter).map(Entry::new).collect(Collectors.toList()));
            setScrollAmount(getScrollAmount());
        }

        @Override
        public int getRowWidth() {
            return width;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
//            super.renderWidget();
//            this.renderBackground(graphics);
            int i = this.getScrollbarPosition();
            int j = i + 6;
            Tesselator tesselator = Tesselator.getInstance();
            RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, TextureResource.background.location);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int backgroundBrightness = 170;
            BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            var x0 = getX();
            var x1 = x0 + width;
            var y0 = getY() + rimHeight;
            var y1 = getY() + height - rimHeight;
            bufferbuilder.addVertex(x0, y1, 0.0f).setUv((float)x0 / 32.0F, (float)(y1 + (int)this.getScrollAmount()) / 32.0F).setColor(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255);
            bufferbuilder.addVertex(x1, y1, 0.0f).setUv((float)x1 / 32.0F, (float)(y1 + (int)this.getScrollAmount()) / 32.0F).setColor(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255);
            bufferbuilder.addVertex(x1, y0, 0.0f).setUv((float)x1 / 32.0F, (float)(y0 + (int)this.getScrollAmount()) / 32.0F).setColor(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255);
            bufferbuilder.addVertex(x0, y0, 0.0f).setUv((float)x0 / 32.0F, (float)(y0 + (int)this.getScrollAmount()) / 32.0F).setColor(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255);
            BufferUploader.drawWithShader(bufferbuilder.build());

            this.renderListItems(graphics, mouseX, mouseY, partialTicks);
            RenderSystem.disableDepthTest();
            this.renderStripe(new Point(x0 - 2, y0 - rimHeight), new Point(x0, y1 + rimHeight));
            this.renderStripe(new Point(x0, y0 - rimHeight), new Point(x1, y0));
            this.renderStripe(new Point(x0, y1), new Point(x1, y1 + rimHeight));

            RenderSystem.depthFunc(515);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
//            RenderSystem.disableTexture();
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bufferbuilder.addVertex(x0, y0 + 4, 0).setUv(0.0F, 1.0F).setColor(0, 0, 0, 0);
            bufferbuilder.addVertex(x1, y0 + 4, 0).setUv(1.0F, 1.0F).setColor(0, 0, 0, 0);
            bufferbuilder.addVertex(x1, y0, 0).setUv(1.0F, 0.0F).setColor(0, 0, 0, 255);
            bufferbuilder.addVertex(x0, y0, 0).setUv(0.0F, 0.0F).setColor(0, 0, 0, 255);
            BufferUploader.drawWithShader(bufferbuilder.build());
            bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bufferbuilder.addVertex(x0, y1, 0).setUv(0.0F, 1.0F).setColor(0, 0, 0, 255);
            bufferbuilder.addVertex(x1, y1, 0).setUv(1.0F, 1.0F).setColor(0, 0, 0, 255);
            bufferbuilder.addVertex(x1, y1 - 4, 0).setUv(1.0F, 0.0F).setColor(0, 0, 0, 0);
            bufferbuilder.addVertex(x0, y1 - 4, 0).setUv(0.0F, 0.0F).setColor(0, 0, 0, 0);
            BufferUploader.drawWithShader(bufferbuilder.build());
            int j1 = this.getMaxScroll();
            if (j1 > 0) {
//                RenderSystem.disableTexture();
                RenderSystem.setShader(CoreShaders.POSITION_COLOR);
                int k1 = (int)((float)((y1 - y0) * (y1 - y0)) / (float)this.getMaxPosition());
                k1 = Mth.clamp(k1, 32, y1 - y0 - 8);
                int l1 = (int)this.getScrollAmount() * (y1 - y0 - k1) / j1 + y0;
                if (l1 < y0) {
                    l1 = y0;
                }

                bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                bufferbuilder.addVertex(i, y1, 0).setUv(0.0F, 1.0F).setColor(0, 0, 0, 255);
                bufferbuilder.addVertex(j, y1, 0).setUv(1.0F, 1.0F).setColor(0, 0, 0, 255);
                bufferbuilder.addVertex(j, y0, 0).setUv(1.0F, 0.0F).setColor(0, 0, 0, 255);
                bufferbuilder.addVertex(i, y0, 0).setUv(0.0F, 0.0F).setColor(0, 0, 0, 255);
                BufferUploader.drawWithShader(bufferbuilder.build());
                bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                bufferbuilder.addVertex(i, l1 + k1, 0).setUv(0.0F, 1.0F).setColor(128, 128, 128, 255);
                bufferbuilder.addVertex(j, l1 + k1, 0).setUv(1.0F, 1.0F).setColor(128, 128, 128, 255);
                bufferbuilder.addVertex(j, l1, 0).setUv(1.0F, 0.0F).setColor(128, 128, 128, 255);
                bufferbuilder.addVertex(i, l1, 0).setUv(0.0F, 0.0F).setColor(128, 128, 128, 255);
                BufferUploader.drawWithShader(bufferbuilder.build());
                bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                bufferbuilder.addVertex(i, l1 + k1 - 1, 0).setUv(0.0F, 1.0F).setColor(192, 192, 192, 255);
                bufferbuilder.addVertex(j - 1, l1 + k1 - 1, 0).setUv(1.0F, 1.0F).setColor(192, 192, 192, 255);
                bufferbuilder.addVertex(j - 1, l1, 0).setUv(1.0F, 0.0F).setColor(192, 192, 192, 255);
                bufferbuilder.addVertex(i, l1, 0).setUv(0.0F, 0.0F).setColor(192, 192, 192, 255);
                BufferUploader.drawWithShader(bufferbuilder.build());
            }

            this.renderDecorations(graphics, mouseX, mouseY);
//            RenderSystem.enableTexture();
            RenderSystem.disableBlend();
        }

        protected void renderStripe(Point min, Point max) {
            Tesselator tesselator = Tesselator.getInstance();
            RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, TextureResource.background.location);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            var bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            bufferbuilder.addVertex(min.x, max.y, 0).setUv(min.x / 32f, max.y / 32.0f).setColor(255, 255, 255, 255);
            bufferbuilder.addVertex(max.x, max.y, 0).setUv(max.x / 32f, max.y / 32.0f).setColor(255, 255, 255, 255);
            bufferbuilder.addVertex(max.x, min.y, 0).setUv(max.x / 32f, min.y / 32.0f).setColor(255, 255, 255, 255);
            bufferbuilder.addVertex(min.x, min.y, 0).setUv(min.x / 32f, min.y / 32.0f).setColor(255, 255, 255, 255);
            BufferUploader.drawWithShader(bufferbuilder.build());
        }

        @Override
        protected void renderListItems(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int itemCount = this.getItemCount();
            for(int i = 0; i < itemCount; ++i) {
                int rowTop = this.getRowTop(i);
                int rowBottom = rowTop + fontRenderer.lineHeight;
                var y0 = getY() + rimHeight;
                var y1 = getY() + height - rimHeight;
                if (rowBottom >= y0 && rowTop <= y1) {
                    int height = this.itemHeight - 4;
                    Entry e = this.getEntry(i);
                    int width = this.getRowWidth();
                    int left = this.getRowLeft();
                    e.render(graphics, i, rowTop, left, width, height, mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && Objects.equals(this.getEntryAtPosition(mouseX, mouseY), e), partialTick);
                }
            }

        }

        @Override
        protected int getScrollbarPosition() {
            return getX() + width - 6;
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {

            private final EntryType content;

            public Entry(EntryType content) {
                this.content = content;
            }

            @Override
            public void render(GuiGraphics graphics, int i, int p_render_2_, int p_render_3_, int p_render_4_, int p_render_5_, int mouseX, int mouseY, boolean p_render_8_, float p_render_9_) {
                int brightness = 255;
                if(this.isMouseOver(mouseX, mouseY))
                    brightness = (int) (brightness * 0.8f);
                if(shouldHighlightSelected && allEntries.indexOf(List.this.getEntry(i).content) == selectedIndex)
                    brightness = (int) (brightness * 0.6f);
                RenderingUtil.drawString(
                    graphics,
                    fontRenderer,
                    content.toString(),
                    new Point(List.this.getX(), p_render_2_ + 1),
                    Rect.XAlignment.Center, Rect.YAlignment.Top,
                    Colors.from(brightness, brightness, brightness),
                    width - 6,
                    true
                );
            }

            @Override
            public Component getNarration() {
                return Component.literal("");
            }

            @Override
            public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
                DropDownSelection.this.selectedIndex = allEntries.indexOf(this.content);
                onSelectionChanged.accept(this.content);
                return true;
            }

        }

    }

}
