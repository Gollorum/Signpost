package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import gollorum.signpost.minecraft.gui.utils.*;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;

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
    private final FontRenderer fontRenderer;

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
        FontRenderer fontRenderer,
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
        FontRenderer fontRenderer,
        Rect rect, int width, int height, int yOffset,
        Consumer<List> onShow, Consumer<List> onHide, Consumer<EntryType> onSelectionChanged,
        boolean shouldHighlightSelected
    ){
        super(rect.point.x, rect.point.y, rect.width, rect.height, 0, 0, texture.size.height, texture.location, texture.fileSize.width, texture.fileSize.height, b -> ((DropDownSelection)b).toggle());
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
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
        if(isListVisible) list.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderButton(int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().bind(texture.location);
        RenderSystem.disableDepthTest();
        int yTexStart = this.isHovered() ? texture.size.height : 0;
        int xTexStart = this.isListVisible ? texture.size.width : 0;

        blit(this.x, this.y, 100, xTexStart, yTexStart, this.width, this.height, texture.fileSize.height, texture.fileSize.width);
        RenderSystem.enableDepthTest();
    }

    public class List extends ExtendedList<List.Entry> {

        private final int rimHeight;

        public List(Minecraft minecraft, Point topRight, int width, int height) {
            this(minecraft, topRight, width, height, minecraft.font.lineHeight);
        }

        public List(Minecraft minecraft, Point topRight, int width, int height, int rimHeight) {
            super(minecraft, width, height, topRight.y + rimHeight, topRight.y + height - rimHeight, 14);
            x0 = topRight.x - width;
            x1 = topRight.x;
            this.setRenderHeader(false, 0);
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
        public void render(int mouseX, int mouseY, float partialTicks) {
            this.renderBackground();
            int i = this.getScrollbarPosition();
            int j = i + 6;
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuilder();
            this.minecraft.getTextureManager().bind(TextureResource.background.location);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            int backgroundBrightness = 170;
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.vertex(this.x0, this.y1, 0.0D).uv((float)this.x0 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255).endVertex();
            bufferbuilder.vertex(this.x1, this.y1, 0.0D).uv((float)this.x1 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255).endVertex();
            bufferbuilder.vertex(this.x1, this.y0, 0.0D).uv((float)this.x1 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255).endVertex();
            bufferbuilder.vertex(this.x0, this.y0, 0.0D).uv((float)this.x0 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255).endVertex();
            tessellator.end();
            int k = this.getRowLeft();
            int l = this.y0 + 4 - (int)this.getScrollAmount();

            this.renderList( k, l, mouseX, mouseY, partialTicks);
            RenderSystem.disableDepthTest();
            this.renderStripe(new Point(x0 - 2, y0 - rimHeight), new Point(x0, y1 + rimHeight));
            this.renderStripe(new Point(x0, y0 - rimHeight), new Point(x1, y0));
            this.renderStripe(new Point(x0, y1), new Point(x1, y1 + rimHeight));
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            RenderSystem.disableAlphaTest();
            RenderSystem.shadeModel(7425);
            RenderSystem.disableTexture();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.vertex(this.x0, this.y0 + 4, 0.0D).uv(0.0F, 1.0F).color(0, 0, 0, 0).endVertex();
            bufferbuilder.vertex(this.x1, this.y0 + 4, 0.0D).uv(1.0F, 1.0F).color(0, 0, 0, 0).endVertex();
            bufferbuilder.vertex(this.x1, this.y0, 0.0D).uv(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
            bufferbuilder.vertex(this.x0, this.y0, 0.0D).uv(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
            tessellator.end();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.vertex(this.x0, this.y1, 0.0D).uv(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
            bufferbuilder.vertex(this.x1, this.y1, 0.0D).uv(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
            bufferbuilder.vertex(this.x1, this.y1 - 4, 0.0D).uv(1.0F, 0.0F).color(0, 0, 0, 0).endVertex();
            bufferbuilder.vertex(this.x0, this.y1 - 4, 0.0D).uv(0.0F, 0.0F).color(0, 0, 0, 0).endVertex();
            tessellator.end();
            int j1 = Math.max(0, this.getMaxPosition() - (this.y1 - this.y0 - 4));
            if (j1 > 0) {
                int k1 = (int)((float)((this.y1 - this.y0) * (this.y1 - this.y0)) / (float)this.getMaxPosition());
                k1 = MathHelper.clamp(k1, 32, this.y1 - this.y0 - 8);
                int l1 = (int)this.getScrollAmount() * (this.y1 - this.y0 - k1) / j1 + this.y0;
                if (l1 < this.y0) {
                    l1 = this.y0;
                }

                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.vertex(i, this.y1, 0.0D).uv(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.vertex(j, this.y1, 0.0D).uv(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.vertex(j, this.y0, 0.0D).uv(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.vertex(i, this.y0, 0.0D).uv(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
                tessellator.end();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.vertex(i, l1 + k1, 0.0D).uv(0.0F, 1.0F).color(128, 128, 128, 255).endVertex();
                bufferbuilder.vertex(j, l1 + k1, 0.0D).uv(1.0F, 1.0F).color(128, 128, 128, 255).endVertex();
                bufferbuilder.vertex(j, l1, 0.0D).uv(1.0F, 0.0F).color(128, 128, 128, 255).endVertex();
                bufferbuilder.vertex(i, l1, 0.0D).uv(0.0F, 0.0F).color(128, 128, 128, 255).endVertex();
                tessellator.end();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.vertex(i, l1 + k1 - 1, 0.0D).uv(0.0F, 1.0F).color(192, 192, 192, 255).endVertex();
                bufferbuilder.vertex(j - 1, l1 + k1 - 1, 0.0D).uv(1.0F, 1.0F).color(192, 192, 192, 255).endVertex();
                bufferbuilder.vertex(j - 1, l1, 0.0D).uv(1.0F, 0.0F).color(192, 192, 192, 255).endVertex();
                bufferbuilder.vertex(i, l1, 0.0D).uv(0.0F, 0.0F).color(192, 192, 192, 255).endVertex();
                tessellator.end();
            }

            this.renderDecorations(mouseX, mouseY);
            RenderSystem.enableTexture();
            RenderSystem.shadeModel(7424);
            RenderSystem.enableAlphaTest();
            RenderSystem.disableBlend();
        }

        protected void renderStripe(Point min, Point max) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuilder();
            this.minecraft.getTextureManager().bind(TextureResource.background.location);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.vertex(min.x, max.y, 0.0D).uv(min.x / 32f, max.y / 32.0f).color(255, 255, 255, 255).endVertex();
            bufferbuilder.vertex(max.x, max.y, 0.0D).uv(max.x / 32f, max.y / 32.0f).color(255, 255, 255, 255).endVertex();
            bufferbuilder.vertex(max.x, min.y, 0.0D).uv(max.x / 32f, min.y / 32.0f).color(255, 255, 255, 255).endVertex();
            bufferbuilder.vertex(min.x, min.y, 0.0D).uv(min.x / 32f, min.y / 32.0f).color(255, 255, 255, 255).endVertex();
            tessellator.end();
        }

        @Override
        protected void renderList(int p_renderList_1_, int p_renderList_2_, int mouseX, int mouseY, float p_renderList_5_) {
            int itemCount = this.getItemCount();
            for(int i = 0; i < itemCount; ++i) {
                int rowTop = this.getRowTop(i);
                int rowBottom = rowTop + fontRenderer.lineHeight;
                if (rowBottom >= this.y0 && rowTop <= this.y1) {
                    int height = this.itemHeight - 4;
                    Entry e = this.getEntry(i);
                    int width = this.getRowWidth();
                    int left = this.getRowLeft();
                    e.render(i, rowTop, left, width, height, mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && Objects.equals(this.getEntryAtPosition(mouseX, mouseY), e), p_renderList_5_);
                }
            }

        }

        @Override
        protected int getScrollbarPosition() {
            return x1 - 6;
        }

        public class Entry extends ExtendedList.AbstractListEntry<Entry> {

            private final EntryType content;

            public Entry(EntryType content) {
                this.content = content;
            }

            @Override
            public void render(int i, int p_render_2_, int p_render_3_, int p_render_4_, int p_render_5_, int mouseX, int mouseY, boolean p_render_8_, float p_render_9_) {
                int brightness = 255;
                if(this.isMouseOver(mouseX, mouseY))
                    brightness = (int) (brightness * 0.8f);
                if(shouldHighlightSelected && allEntries.indexOf(List.this.getEntry(i).content) == selectedIndex)
                    brightness = (int) (brightness * 0.6f);
                RenderSystem.enableAlphaTest();
                RenderingUtil.drawString(
                    fontRenderer,
                    content.toString(),
                    new Point(List.this.x0, p_render_2_ + 1),
                    Rect.XAlignment.Center, Rect.YAlignment.Top,
                    Colors.from(brightness, brightness, brightness),
                    width - 6,
                    true
                );
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
