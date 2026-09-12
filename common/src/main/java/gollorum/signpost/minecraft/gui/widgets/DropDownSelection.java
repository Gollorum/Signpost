package gollorum.signpost.minecraft.gui.widgets;

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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;

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
        Consumer<EntryType> onSelectionChanged
    ) { this(
        fontRenderer,
        new Rect(position, size, xAlignment, yAlignment),
        width, height,
        yOffset,
        onShow, onHide, onSelectionChanged
    ); }

    private DropDownSelection(
        Font fontRenderer,
        Rect rect, int width, int height, int yOffset,
        Consumer<List> onShow, Consumer<List> onHide, Consumer<EntryType> onSelectionChanged
    ){
        super(rect.point.x, rect.point.y, rect.width, rect.height, new WidgetSprites(texture.location, texture.location), b -> ((DropDownSelection)b).toggle());
        this.rect = rect;
        this.fontRenderer = fontRenderer;
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
//        graphics.pose().translate(0, 0, 100);
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);
        int yTexStart = this.isHovered ? texture.size.height : 0;
        int xTexStart = this.isListVisible ? texture.size.width : 0;

        graphics.blit(
            texture.location,
            getX(), getY(),
            (float) xTexStart, (float) yTexStart,
            this.width, this.height,
            texture.fileSize.height, texture.fileSize.width
        );
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
//            headerHeight = 14;
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

//        @Override
//        protected void renderListItems(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
//            int itemCount = this.getItemCount();
//            for(int i = 0; i < itemCount; ++i) {
//                int rowTop = this.getRowTop(i);
//                int rowBottom = rowTop + fontRenderer.lineHeight;
//                var y0 = getY() + rimHeight;
//                var y1 = getY() + height - rimHeight;
//                if (rowBottom >= y0 && rowTop <= y1) {
//                    int height = this.itemHeight - 4;
//                    Entry e = this.getEntry(i);
//                    int width = this.getRowWidth();
//                    int left = this.getRowLeft();
//                    e.render(graphics, i, rowTop, left, width, height, mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && Objects.equals(this.getEntryAtPosition(mouseX, mouseY), e), partialTick);
//                }
//            }
//
//        }

//        @Override
//        protected int getScrollbarPosition() {
//            return getX() + width - 6;
//        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {

            private final EntryType content;

            public Entry(EntryType content) {
                this.content = content;
            }

            @Override
            public void render(
                GuiGraphics graphics, int index, int top, int left, int entryWidth, int entryHeight,
                int mouseX, int mouseY, boolean isHovering, float partialTick
            ) {
                int brightness = 255;
//                if(this.isMouseOver(mouseX, mouseY))
//                    brightness = (int) (brightness * 0.8f);
                if(isHovering)
                    brightness = (int) (brightness * 0.6f);
                RenderingUtil.drawString(
                    graphics,
                    fontRenderer,
                    content.toString(),
                    new Point(left, top),
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
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                DropDownSelection.this.selectedIndex = allEntries.indexOf(this.content);
                onSelectionChanged.accept(this.content);
                return true;
            }

        }

    }

}
