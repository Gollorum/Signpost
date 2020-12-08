package gollorum.signpost.minecraft.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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

public class DropDownSelection extends ImageButton {

    private static final TextureResource texture = TextureResource.expandContract;
    public static final TextureSize size = new TextureSize(11, 11);
    private final Consumer<List> onShow;
    private final Consumer<List> onHide;
    private final Consumer<String> onSelectionChanged;
    private final FontRenderer fontRenderer;

    private boolean isListVisible;
    private final List list;

    private final java.util.List<String> allEntries = new ArrayList<>();
    private Predicate<String> filter = b -> true;
    private int selectedIndex;
    private final boolean shouldHighlightSelected;

    public Collection<String> getAllEntries() { return allEntries; }

    public Optional<String> getSelectedEntry() {
        List.Entry selectedEntry = list.getSelected();
        if(selectedEntry == null)
            return Optional.empty();
        else return Optional.of(selectedEntry.text);
    }

    public void setFilter(Predicate<String> filter) {
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
        Consumer<String> onSelectionChanged,
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
        Consumer<List> onShow, Consumer<List> onHide, Consumer<String> onSelectionChanged,
        boolean shouldHighlightSelected
    ){
        super(rect.point.x, rect.point.y, rect.width, rect.height, 0, 0, texture.size.height, texture.location, texture.fileSize.width, texture.fileSize.height, b -> ((DropDownSelection)b).toggle());
        this.fontRenderer = fontRenderer;
        this.shouldHighlightSelected = shouldHighlightSelected;
        list = new List(Minecraft.getInstance(), new Point(rect.point.x + size.width, rect.point.y + size.height + yOffset), width, height);
        this.onSelectionChanged = onSelectionChanged;
        this.onShow = onShow;
        this.onHide = onHide;
    }

    public void addEntry(String text) {
        if(!allEntries.contains(text)) {
            allEntries.add(text);
            list.updateContent();
        }
    }

    public void removeEntry(String text) {
        if(allEntries.contains(text)) {
            allEntries.remove(text);
            list.updateContent();
        }
    }

    public void setEntries(Collection<String> entries) {
        allEntries.clear();
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

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        if(isListVisible) list.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // Copied from super with varying xTexStart.
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().bindTexture(texture.location);
        RenderSystem.disableDepthTest();
        int yTexStart = this.isHovered() ? texture.size.height : 0;
        int xTexStart = this.isListVisible ? texture.size.width : 0;

        blit(matrixStack, this.x, this.y, xTexStart, yTexStart, this.width, this.height, texture.fileSize.width, texture.fileSize.height);
        RenderSystem.enableDepthTest();
    }

    public class List extends ExtendedList<List.Entry> {

        private final int rimHeight;

        public List(Minecraft minecraft, Point topRight, int width, int height) {
            this(minecraft, topRight, width, height, minecraft.fontRenderer.FONT_HEIGHT);
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
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            // Copied from super with different texture and hole and an additional left vertical stripe.
            this.renderBackground(matrixStack);
            int i = this.getScrollbarPosition();
            int j = i + 6;
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            this.minecraft.getTextureManager().bindTexture(TextureResource.background.location);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            int backgroundBrightness = 170;
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos(this.x0, this.y1, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255).endVertex();
            bufferbuilder.pos(this.x1, this.y1, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255).endVertex();
            bufferbuilder.pos(this.x1, this.y0, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255).endVertex();
            bufferbuilder.pos(this.x0, this.y0, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(backgroundBrightness, backgroundBrightness, backgroundBrightness, 255).endVertex();
            tessellator.draw();
            int k = this.getRowLeft();
            int l = this.y0 + 4 - (int)this.getScrollAmount();

            this.renderList(matrixStack, k, l, mouseX, mouseY, partialTicks);
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
            bufferbuilder.pos(this.x0, this.y0 + 4, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 0).endVertex();
            bufferbuilder.pos(this.x1, this.y0 + 4, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 0).endVertex();
            bufferbuilder.pos(this.x1, this.y0, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos(this.x0, this.y0, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
            tessellator.draw();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos(this.x0, this.y1, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos(this.x1, this.y1, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos(this.x1, this.y1 - 4, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 0).endVertex();
            bufferbuilder.pos(this.x0, this.y1 - 4, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 0).endVertex();
            tessellator.draw();
            int j1 = Math.max(0, this.getMaxPosition() - (this.y1 - this.y0 - 4));
            if (j1 > 0) {
                int k1 = (int)((float)((this.y1 - this.y0) * (this.y1 - this.y0)) / (float)this.getMaxPosition());
                k1 = MathHelper.clamp(k1, 32, this.y1 - this.y0 - 8);
                int l1 = (int)this.getScrollAmount() * (this.y1 - this.y0 - k1) / j1 + this.y0;
                if (l1 < this.y0) {
                    l1 = this.y0;
                }

                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos(i, this.y1, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(j, this.y1, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(j, this.y0, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(i, this.y0, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
                tessellator.draw();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos(i, l1 + k1, 0.0D).tex(0.0F, 1.0F).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos(j, l1 + k1, 0.0D).tex(1.0F, 1.0F).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos(j, l1, 0.0D).tex(1.0F, 0.0F).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos(i, l1, 0.0D).tex(0.0F, 0.0F).color(128, 128, 128, 255).endVertex();
                tessellator.draw();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos(i, l1 + k1 - 1, 0.0D).tex(0.0F, 1.0F).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos(j - 1, l1 + k1 - 1, 0.0D).tex(1.0F, 1.0F).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos(j - 1, l1, 0.0D).tex(1.0F, 0.0F).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos(i, l1, 0.0D).tex(0.0F, 0.0F).color(192, 192, 192, 255).endVertex();
                tessellator.draw();
            }

            this.renderDecorations(matrixStack, mouseX, mouseY);
            RenderSystem.enableTexture();
            RenderSystem.shadeModel(7424);
            RenderSystem.enableAlphaTest();
            RenderSystem.disableBlend();
        }

        protected void renderStripe(Point min, Point max) {
            // Copied from super with different texture.
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            this.minecraft.getTextureManager().bindTexture(TextureResource.background.location);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            int brightness = 255;
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos(min.x, max.y, 0.0D).tex(min.x / 32f, max.y / 32.0f).color(brightness, brightness, brightness, 255).endVertex();
            bufferbuilder.pos(max.x, max.y, 0.0D).tex(max.x / 32f, max.y / 32.0f).color(brightness, brightness, brightness, 255).endVertex();
            bufferbuilder.pos(max.x, min.y, 0.0D).tex(max.x / 32f, min.y / 32.0f).color(brightness, brightness, brightness, 255).endVertex();
            bufferbuilder.pos(min.x, min.y, 0.0D).tex(min.x / 32f, min.y / 32.0f).color(brightness, brightness, brightness, 255).endVertex();
            tessellator.draw();
        }

        @Override
        protected void renderList(MatrixStack matrixStack, int p_renderList_1_, int p_renderList_2_, int mouseX, int mouseY, float p_renderList_5_) {
            // Copied from super with better font culling.
            int i = this.getItemCount();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();

            for(int j = 0; j < i; ++j) {
                int k = this.getRowTop(j);
                int l = k + fontRenderer.FONT_HEIGHT;
                if (l >= this.y0 && k <= this.y1) {
                    int i1 = p_renderList_2_ + j * this.itemHeight + this.headerHeight;
                    int j1 = this.itemHeight - 4;
                    Entry e = this.getEntry(j);
                    int k1 = this.getRowWidth();
//                    if (this.renderSelection && this.isSelectedItem(j)) {
//                        int l1 = this.x0 + this.width / 2 - k1 / 2;
//                        int i2 = this.x0 + this.width / 2 + k1 / 2;
//                        RenderSystem.disableTexture();
//                        float f = this.isFocused() ? 1.0F : 0.5F;
//                        RenderSystem.color4f(f, f, f, 1.0F);
//                        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
//                        bufferbuilder.pos(l1, i1 + j1 + 2, 0.0D).endVertex();
//                        bufferbuilder.pos(i2, i1 + j1 + 2, 0.0D).endVertex();
//                        bufferbuilder.pos(i2, i1 - 2, 0.0D).endVertex();
//                        bufferbuilder.pos(l1, i1 - 2, 0.0D).endVertex();
//                        tessellator.draw();
//                        RenderSystem.color4f(0.0F, 0.0F, 0.0F, 1.0F);
//                        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
//                        bufferbuilder.pos(l1 + 1, i1 + j1 + 1, 0.0D).endVertex();
//                        bufferbuilder.pos(i2 - 1, i1 + j1 + 1, 0.0D).endVertex();
//                        bufferbuilder.pos(i2 - 1, i1 - 1, 0.0D).endVertex();
//                        bufferbuilder.pos(l1 + 1, i1 - 1, 0.0D).endVertex();
//                        tessellator.draw();
//                        RenderSystem.enableTexture();
//                    }

                    int j2 = this.getRowLeft();
                    e.render(matrixStack, j, k, j2, k1, j1, mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && Objects.equals(this.getEntryAtPosition(mouseX, mouseY), e), p_renderList_5_);
                }
            }

        }

        @Override
        protected int getScrollbarPosition() {
            return x1 - 6;
        }

        public class Entry extends ExtendedList.AbstractListEntry<Entry> {

            private final String text;

            public Entry(String text) {
                this.text = text;
            }

            @Override
            public void render(MatrixStack matrixStack, int i, int p_render_2_, int p_render_3_, int p_render_4_, int p_render_5_, int mouseX, int mouseY, boolean p_render_8_, float p_render_9_) {
                int brightness = 255;
                if(this.isMouseOver(mouseX, mouseY))
                    brightness = (int) (brightness * 0.8f);
                if(shouldHighlightSelected && allEntries.indexOf(List.this.getEntry(i).text) == selectedIndex)
                    brightness = (int) (brightness * 0.6f);
                RenderSystem.enableAlphaTest();
                RenderingUtil.drawString(
                    fontRenderer,
                    text,
                    new Point(List.this.x0, p_render_2_ + 1),
                    Rect.XAlignment.Center, Rect.YAlignment.Top,
                    Colors.from(brightness, brightness, brightness),
                    width - 6,
                    true
                );
            }

            @Override
            public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
                DropDownSelection.this.selectedIndex = allEntries.indexOf(this.text);
                onSelectionChanged.accept(this.text);
                return true;
            }

        }

    }

}
