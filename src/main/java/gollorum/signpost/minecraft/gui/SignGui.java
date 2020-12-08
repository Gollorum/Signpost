package gollorum.signpost.minecraft.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.events.WaystoneRenamedEvent;
import gollorum.signpost.minecraft.events.WaystoneUpdatedEvent;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.signtypes.LargeSign;
import gollorum.signpost.signtypes.Sign;
import gollorum.signpost.signtypes.SmallShortSign;
import gollorum.signpost.signtypes.SmallWideSign;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class SignGui extends Screen {


    private enum SignType {
        Wide, Short, Large
    }

    private static final TextureSize typeSelectionButtonsTextureSize = TextureResource.signTypeSelection.size;
    private static final TextureResource waystoneNameTexture = TextureResource.waystoneNameField;
    private static final TextureSize typeSelectionButtonsSize = new TextureSize(typeSelectionButtonsTextureSize.width * 2, typeSelectionButtonsTextureSize.height * 2);
    private static final int typeSelectionButtonsSpace = (int) (typeSelectionButtonsSize.width * 0.3f);

    private static final int typeSelectionButtonsY = 15;
    private static final float typeSelectionButtonsScale = 0.66f;

    private static final int centralAreaHeight = 110;
    private static final int centerGap = 15;

    private static final float waystoneBoxScale = 2.5f;
    final int inputSignsScale = 5;

    private ImageInputBox waystoneInputBox;
    private DropDownSelection waystoneDropdown;

    private final Optional<ItemStack> itemToDropOnBreak;

    private final Consumer<WaystoneUpdatedEvent> waystoneUpdateListener = event -> {
        switch(event.getType()) {
            case Added:
                waystoneDropdown.addEntry(event.name);
                break;
            case Removed:
                waystoneDropdown.removeEntry(event.name);
                break;
            case Renamed:
                waystoneDropdown.removeEntry(((WaystoneRenamedEvent)event).oldName);
                waystoneDropdown.addEntry(event.name);
                break;
        }
    };

    private static final TextureSize buttonsSize = new TextureSize(98, 20);
    private Button doneButton;

    private SignType selectedType = null;
    private final PostTile tile;
    private final Post.ModelType modelType;
    private final Vector3 localHitPos;

    private final Optional<Sign> oldSign;
    private final Optional<PostTile.TilePartInfo> oldTilePartInfo;

    public SignGui(PostTile tile, Post.ModelType modelType, Vector3 localHitPos, Optional<ItemStack> itemToDropOnBreak) {
        super(new StringTextComponent("Sign"));
        this.tile = tile;
        this.modelType = modelType;
        this.localHitPos = localHitPos;
        this.itemToDropOnBreak = itemToDropOnBreak;
        oldSign = Optional.empty();
        oldTilePartInfo = Optional.empty();
    }

    public SignGui(PostTile tile, Post.ModelType modelType, Sign oldSign, PostTile.TilePartInfo oldTilePartInfo) {
        super(new StringTextComponent("Sign"));
        this.tile = tile;
        this.modelType = modelType;
        this.localHitPos = Vector3.ZERO;
        this.itemToDropOnBreak = oldSign.itemToDropOnBreak;
        this.oldSign = Optional.of(oldSign);
        this.oldTilePartInfo = Optional.of(oldTilePartInfo);
    }

    @Override
    protected void init() {
        super.init();
        int signTypeSelectionTopY = typeSelectionButtonsY;
        int centerOffset = (typeSelectionButtonsSize.width + typeSelectionButtonsSpace) / 2;
        addButton(newImageButton(
            TextureResource.signTypeSelection,
            0,
            new Point(getCenterX() - centerOffset, signTypeSelectionTopY),
            typeSelectionButtonsScale,
            Rect.XAlignment.Center, Rect.YAlignment.Top,
            this::switchToWide
        ));
        addButton(newImageButton(
            TextureResource.signTypeSelection,
            1,
            new Point(getCenterX(), signTypeSelectionTopY),
            typeSelectionButtonsScale,
            Rect.XAlignment.Center, Rect.YAlignment.Top,
            this::switchToShort
        ));
        addButton(newImageButton(
            TextureResource.signTypeSelection,
            2,
            new Point(getCenterX() + centerOffset, signTypeSelectionTopY),
            typeSelectionButtonsScale,
            Rect.XAlignment.Center, Rect.YAlignment.Top,
            this::switchToLarge
        ));
        Rect doneRect = new Rect(new Point(getCenterX(), height - typeSelectionButtonsY), buttonsSize, Rect.XAlignment.Center, Rect.YAlignment.Bottom);
        if(oldSign.isPresent()){
            int buttonsWidth = (int) (doneRect.width * 0.75);
            doneButton = new Button(
                getCenterX() + centerGap / 2, doneRect.point.y, buttonsWidth, doneRect.height,
                new TranslationTextComponent(LangKeys.done),
                b -> done()
            );
            Button removeSignButton = new Button(
                getCenterX() - centerGap / 2 - buttonsWidth, doneRect.point.y, buttonsWidth, doneRect.height,
                new TranslationTextComponent(LangKeys.removeSign),
                b -> removeSign()
            );
            removeSignButton.setFGColor(Colors.invalid);
            addButton(removeSignButton);
        } else {
            doneButton = new Button(
                doneRect.point.x, doneRect.point.y, doneRect.width, doneRect.height,
                new TranslationTextComponent(LangKeys.done),
                b -> done()
            );
        }
        addButton(doneButton);
        waystoneDropdown = new DropDownSelection(font,
            new Point(getCenterX() - centerGap, getCenterY() - centralAreaHeight / 2 + 4 * inputSignsScale),
            Rect.XAlignment.Right,
            Rect.YAlignment.Center,
            (int)(waystoneNameTexture.size.width * waystoneBoxScale) + DropDownSelection.size.width,
            100,
            (int)((waystoneNameTexture.size.height * waystoneBoxScale) - DropDownSelection.size.height) / 2,
            children::add,
            children::remove,
            textIn -> {
                waystoneInputBox.setText(textIn);
                waystoneDropdown.toggle();
            },
            false);
        waystoneDropdown.setEntries(new HashSet<>());
        addButton(waystoneDropdown);
        waystoneInputBox = new ImageInputBox(font,
            new Rect(
                new Point(waystoneDropdown.x - 10, waystoneDropdown.y + waystoneDropdown.getHeightRealms() / 2),
                new TextureSize((int)((waystoneNameTexture.size.width - 4) * waystoneBoxScale), (int)((waystoneNameTexture.size.height - 4) * waystoneBoxScale)),
                Rect.XAlignment.Right, Rect.YAlignment.Center),
            new Rect(
                Point.zero,
                waystoneNameTexture.size.scale(waystoneBoxScale),
                Rect.XAlignment.Center, Rect.YAlignment.Center),
            Rect.XAlignment.Center, Rect.YAlignment.Center,
            waystoneNameTexture,
            true);
        waystoneInputBox.setMaxStringLength(200);
        waystoneInputBox.setResponder(this::onWaystoneSelected);
        addButton(waystoneInputBox);

        Rect wideRect = new Rect(
            new Point(getCenterX() + centerGap + 2 * inputSignsScale, getCenterY() - centralAreaHeight / 2),
            modelType.wideGuiTexture.size.scale(inputSignsScale),
            Rect.XAlignment.Left,
            Rect.YAlignment.Top);
        wideSignInputBox = new ImageInputBox(font,
            wideRect.offset(new Point(5, 2).mul(inputSignsScale), new Point(-2, -10).mul(inputSignsScale)),
            wideRect.withPoint(new Point(-5, -2).mul(inputSignsScale)),
            Rect.XAlignment.Left, Rect.YAlignment.Top,
            modelType.wideGuiTexture,
            false);
        Rect shortRect = new Rect(
            wideRect.min().withX(x -> x - 2 * inputSignsScale),
            modelType.shortGuiTexture.size.scale(inputSignsScale),
            Rect.XAlignment.Left,
            Rect.YAlignment.Top);
        shortSignInputBox = new ImageInputBox(font,
            shortRect.offset(new Point(5, 2).mul(inputSignsScale), new Point(-5, -10).mul(inputSignsScale)),
            shortRect.withPoint(new Point(-5, -2).mul(inputSignsScale)),
            Rect.XAlignment.Left, Rect.YAlignment.Top,
            modelType.shortGuiTexture,
            false);
        Rect largeRect = new Rect(
            wideRect.min().withX(x -> x + 3 * inputSignsScale),
            modelType.largeGuiTexture.size.scale(inputSignsScale),
            Rect.XAlignment.Left,
            Rect.YAlignment.Top);
        Rect largeInputRect = largeRect.withPoint(p -> p.add(
            new Point(4 * inputSignsScale, 2 * inputSignsScale))
        ).withSize(
                width -> width - 6 * inputSignsScale,
                height -> (int)(2.5f * inputSignsScale)
        );
        ImageInputBox firstLarge = new ImageInputBox(font,
            largeInputRect,
            largeRect.withPoint(largeRect.point.subtract(largeInputRect.point)),
            Rect.XAlignment.Left, Rect.YAlignment.Top,
            modelType.largeGuiTexture,
            false
        );
        largeInputRect = largeInputRect.withPoint(p -> p.withY(Math.round(largeRect.point.y + (12 - 3 * 2.5f) * inputSignsScale)));
        InputBox secondLarge = new InputBox(font, largeInputRect, false, false);
        secondLarge.setTextColor(Colors.black);
        largeInputRect = largeInputRect.withPoint(p -> p.withY(Math.round(largeRect.point.y + (12 - 2 * 2.5f) * inputSignsScale)));
        InputBox thirdLarge = new InputBox(font, largeInputRect, false, false);
        thirdLarge.setTextColor(Colors.black);
        largeInputRect = largeInputRect.withPoint(p -> p.withY(Math.round(largeRect.point.y + (12 - 1 * 2.5f) * inputSignsScale)));
        InputBox fourthLarge = new InputBox(font, largeInputRect, false, false);
        fourthLarge.setTextColor(Colors.black);

        largeSignInputBoxes = ImmutableList.of(firstLarge, secondLarge, thirdLarge, fourthLarge);
        allSignInputBoxes = ImmutableList.of(wideSignInputBox, shortSignInputBox, firstLarge, secondLarge, thirdLarge, fourthLarge);
        allImageInputBoxes = ImmutableList.of(wideSignInputBox, shortSignInputBox, firstLarge);

        colorInputBox = new ColorInputBox(font,
            new Rect(new Point(wideRect.point.x + wideRect.width / 2, wideRect.point.y + wideRect.height + centerGap), 80, 20, Rect.XAlignment.Center, Rect.YAlignment.Top));
        colorInputBox.setColorResponder(color -> allSignInputBoxes.forEach(b -> b.setTextColor(color)));
        addButton(colorInputBox);

        Button switchDirectionButton = newImageButton(
            TextureResource.flipDirection,
            0,
            new Point(colorInputBox.x + colorInputBox.getWidth() + 10, colorInputBox.y + colorInputBox.getHeight() / 2),
            1,
            Rect.XAlignment.Left, Rect.YAlignment.Center,
            this::flip
        );
        addButton(switchDirectionButton);

        if(oldSign.isPresent()) {
            if(oldSign.get() instanceof LargeSign) {
                switchToLarge();
                LargeSign sign = (LargeSign) oldSign.get();
                for(int i = 0; i < largeSignInputBoxes.size(); i++) {
                    largeSignInputBoxes.get(i).setText(sign.getText()[i]);
                }
            }
            else if(oldSign.get() instanceof SmallShortSign) {
                switchToShort();
                shortSignInputBox.setText(((SmallShortSign) oldSign.get()).getText());
            }
            else {
                switchToWide();
                wideSignInputBox.setText(((SmallWideSign) oldSign.get()).getText());
            }
            if(!oldSign.get().isFlipped())
                flip();
            colorInputBox.setSelectedColor(oldSign.get().getColor());
        } else switchToWide();


        WaystoneLibrary.getInstance().requestAllWaystoneNames(n -> {
            waystoneDropdown.setEntries(n.values());
            oldSign.flatMap(s -> (Optional<WaystoneHandle>) s.getDestination()).ifPresent(id -> {
                String name = n.get(id);
                if(name != null && !name.equals(""))
                    waystoneInputBox.setText(name);
            });
        });
        WaystoneLibrary.getInstance().updateEventDispatcher.addListener(waystoneUpdateListener);
    }

    private void flip() {
        allImageInputBoxes.forEach(ImageInputBox::flip);
    }

    private ImageInputBox wideSignInputBox;
    private ImageInputBox shortSignInputBox;

    private List<InputBox> largeSignInputBoxes;

    private List<InputBox> allSignInputBoxes;
    private List<ImageInputBox> allImageInputBoxes;

    @Nullable
    private ImageInputBox currentSignInputBox;
    private ColorInputBox colorInputBox;

    private String lastWaystone = "";

    private void onWaystoneSelected(String waystoneName) {
        if(isValidWaystone(waystoneName)) {
            waystoneInputBox.setTextColor(Colors.valid);
            waystoneInputBox.setDisabledTextColour(Colors.validInactive);
            waystoneDropdown.setFilter(name -> true);
            if(currentSignInputBox != null && lastWaystone.equals(currentSignInputBox.getText()))
                currentSignInputBox.setText(waystoneName);
            lastWaystone = waystoneName;
        } else {
            waystoneInputBox.setTextColor(Colors.invalid);
            waystoneInputBox.setDisabledTextColour(Colors.invalidInactive);
            waystoneDropdown.setFilter(name -> name.toLowerCase().contains(waystoneName.toLowerCase()));
            if(currentSignInputBox != null && lastWaystone.equals(currentSignInputBox.getText()))
                currentSignInputBox.setText("");
        }
    }

    private boolean isValidWaystone(String name){
        return waystoneDropdown.getAllEntries().contains(name) || name.equals("");
    }

    // Texture must include highlight below main
    private static ImageButton newImageButton(
        TextureResource texture,
        int index,
        Point referencePoint,
        float scale,
        Rect.XAlignment xAlignment,
        Rect.YAlignment yAlignment,
        Runnable onClick
    ){
        Rect rect = new Rect(referencePoint, texture.size.scale(scale), xAlignment, yAlignment);
        return new ImageButton(
            rect.point.x, rect.point.y,
            rect.width, rect.height,
            (int) (index * texture.size.width * scale), 0, (int) (texture.size.height * scale),
            texture.location,
            (int) (texture.fileSize.width * scale), (int) (texture.fileSize.height * scale),
            b -> onClick.run()
        );
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);
        String hint = I18n.format(LangKeys.newSignHint);
        font.drawStringWithShadow(matrixStack, hint, (width - font.getStringWidth(hint)) / 2f, (doneButton.y + doneButton.getHeightRealms() + height - font.FONT_HEIGHT) / 2f, Colors.white);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private int getCenterX() { return this.width / 2; }
    private int getCenterY() { return this.height / 2; }

    private final List<Widget> selectionDependentWidgets = Lists.newArrayList();

    private void switchToWide(){
        if(selectedType == SignType.Wide) return;
        clearTypeDependentChildren();
        selectedType = SignType.Wide;

        switchSignInputBoxTo(wideSignInputBox);

        addTypeDependentChild(wideSignInputBox);
        updateColorBoxPosition(wideSignInputBox.bounds.max().y);
    }

    private void switchToShort(){
        if(selectedType == SignType.Short) return;
        clearTypeDependentChildren();
        selectedType = SignType.Short;

        switchSignInputBoxTo(shortSignInputBox);

        addTypeDependentChild(shortSignInputBox);
        updateColorBoxPosition(shortSignInputBox.bounds.max().y);
    }

    private void switchToLarge(){
        if(selectedType == SignType.Large) return;
        clearTypeDependentChildren();
        selectedType = SignType.Large;

        switchSignInputBoxTo((ImageInputBox) largeSignInputBoxes.get(0));

        addTypeDependentChildren(largeSignInputBoxes);
        updateColorBoxPosition(((ImageInputBox) largeSignInputBoxes.get(0)).bounds.max().y);
    }

    private void switchSignInputBoxTo(ImageInputBox box) {
        if(currentSignInputBox != null)
            box.setText(currentSignInputBox.getText());
        currentSignInputBox = box;
    }

    private void updateColorBoxPosition(int otherStuffBottom) {
        colorInputBox.setY(otherStuffBottom + centerGap);
    }

    private void clearTypeDependentChildren(){
        removeButtons(selectionDependentWidgets);
        selectionDependentWidgets.clear();
    }

    private void addTypeDependentChildren(Collection<? extends Widget> widgets){
        selectionDependentWidgets.addAll(widgets);
        addButtons(widgets);
    }

    private void addTypeDependentChild(Widget widget){
        selectionDependentWidgets.add(widget);
        addButton(widget);
    }

    private void addButtons(Collection<? extends Widget> widgets) {
        this.buttons.addAll(widgets);
        this.children.addAll(widgets);
    }

    private void removeButtons(Collection<? extends Widget> widgets) {
        this.buttons.removeAll(widgets);
        this.children.removeAll(widgets);
    }

    private void removeButton(Widget widget) {
        this.buttons.remove(widget);
        this.children.remove(widget);
    }

    @Override
    public void onClose() {
        super.onClose();
        WaystoneLibrary.getInstance().updateEventDispatcher.removeListener(waystoneUpdateListener);
    }

    private void removeSign() {
        if(oldSign.isPresent())
            PacketHandler.sendToServer(new PostTile.PartRemovedEvent.Packet(
                oldTilePartInfo.get(), true
            ));
        else Signpost.LOGGER.error("Tried to remove a sign, but the necessary information was missing.");
        getMinecraft().displayGuiScreen(null);
    }

    private void done() {
        if(isValidWaystone(waystoneInputBox.getText()))
            WaystoneLibrary.getInstance().requestIdFor(waystoneInputBox.getText(), this::apply);
        else apply(Optional.empty());
        getMinecraft().displayGuiScreen(null);
    }

    private void apply(Optional<WaystoneHandle> destinationId) {
        PostTile.TilePartInfo tilePartInfo = oldTilePartInfo.orElseGet(() ->
            new PostTile.TilePartInfo(tile.getWorld().getDimensionKey().getLocation(), tile.getPos(), UUID.randomUUID()));
        CompoundNBT data;
        switch (selectedType) {
            case Wide:
                data = SmallWideSign.METADATA.write(
                    new SmallWideSign(
                        Angle.fromDegrees(0),
                        wideSignInputBox.getText(),
                        !wideSignInputBox.isFlipped(),
                        modelType.mainTexture,
                        modelType.secondaryTexture,
                        colorInputBox.getCurrentColor(),
                        destinationId,
                        itemToDropOnBreak,
                        modelType
                    )
                );
                if(oldSign.isPresent()) {
                    PacketHandler.sendToServer(new PostTile.PartMutatedEvent.Packet(
                        tilePartInfo, data,
                        SmallWideSign.METADATA.identifier));
                } else {
                    PacketHandler.sendToServer(new PostTile.PartAddedEvent.Packet(
                        tilePartInfo,
                        SmallWideSign.METADATA.identifier,
                        data,
                        new Vector3(0, localHitPos.y > 0.5f ? 0.75f : 0.25f, 0))
                    );
                }
                break;
            case Short:
                data = SmallShortSign.METADATA.write(
                    new SmallShortSign(
                        Angle.fromDegrees(0),
                        shortSignInputBox.getText(),
                        !shortSignInputBox.isFlipped(),
                        modelType.mainTexture,
                        modelType.secondaryTexture,
                        colorInputBox.getCurrentColor(),
                        destinationId,
                        itemToDropOnBreak,
                        modelType
                    )
                );
                if(oldSign.isPresent()) {
                    PacketHandler.sendToServer(new PostTile.PartMutatedEvent.Packet(
                        tilePartInfo, data,
                        SmallShortSign.METADATA.identifier));
                } else {
                    PacketHandler.sendToServer(new PostTile.PartAddedEvent.Packet(
                        tilePartInfo,
                        SmallShortSign.METADATA.identifier,
                        data,
                        new Vector3(0, localHitPos.y > 0.5f ? 0.75f : 0.25f, 0))
                    );
                }
                break;
            case Large:
                data = LargeSign.METADATA.write(
                    new LargeSign(
                        Angle.fromDegrees(0),
                        new String[] {
                            largeSignInputBoxes.get(0).getText(),
                            largeSignInputBoxes.get(1).getText(),
                            largeSignInputBoxes.get(2).getText(),
                            largeSignInputBoxes.get(3).getText(),
                        },
                        !currentSignInputBox.isFlipped(),
                        modelType.mainTexture,
                        modelType.secondaryTexture,
                        colorInputBox.getCurrentColor(),
                        destinationId,
                        itemToDropOnBreak,
                        modelType)
                );
                if(oldSign.isPresent()) {
                    PacketHandler.sendToServer(new PostTile.PartMutatedEvent.Packet(
                        tilePartInfo, data,
                        LargeSign.METADATA.identifier));
                } else {
                    PacketHandler.sendToServer(new PostTile.PartAddedEvent.Packet(
                        tilePartInfo,
                        LargeSign.METADATA.identifier,
                        data,
                        new Vector3(0, 0.5f, 0))
                    );
                }
                break;
        }
    }
}
