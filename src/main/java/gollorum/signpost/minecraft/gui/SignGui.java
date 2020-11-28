package gollorum.signpost.minecraft.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.events.WaystoneRenamedEvent;
import gollorum.signpost.minecraft.events.WaystoneUpdatedEvent;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.signtypes.LargeSign;
import gollorum.signpost.signtypes.SmallShortSign;
import gollorum.signpost.signtypes.SmallWideSign;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.StringTextComponent;

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
    private final Vector3 localHitPos;

    public SignGui(PostTile tile, Vector3 localHitPos) {
        super(new StringTextComponent("Sign"));
        this.tile = tile;
        this.localHitPos = localHitPos;
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
        doneButton = new Button(
            doneRect.point.x, doneRect.point.y, doneRect.width, doneRect.height,
            I18n.format(LangKeys.Done),
            b -> done()
        );
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
            }
        );
        waystoneDropdown.setEntries(new HashSet<>());
        addButton(waystoneDropdown);
        waystoneInputBox = new ImageInputBox(font,
            new Rect(
                new Point(waystoneDropdown.x - 10, waystoneDropdown.y + waystoneDropdown.getHeight() / 2),
                new TextureSize((int)((waystoneNameTexture.size.width - 6) * waystoneBoxScale), font.FONT_HEIGHT),
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
        children.add(waystoneInputBox);

        Rect wideRect = new Rect(
            new Point(getCenterX() + centerGap + 2 * inputSignsScale, getCenterY() - centralAreaHeight / 2),
            TextureResource.smallWideSign.size.scale(inputSignsScale),
            Rect.XAlignment.Left,
            Rect.YAlignment.Top);
        wideSignInputBox = new ImageInputBox(font,
            wideRect.offset(new Point(5, 3).mul(inputSignsScale), new Point(-1, -11).mul(inputSignsScale)),
            wideRect.withPoint(new Point(-5, -3).mul(inputSignsScale)),
            Rect.XAlignment.Left, Rect.YAlignment.Top,
            TextureResource.smallWideSign,
            false);
        Rect shortRect = new Rect(
            wideRect.min().withX(x -> x - 2 * inputSignsScale),
            TextureResource.smallShortSign.size.scale(inputSignsScale),
            Rect.XAlignment.Left,
            Rect.YAlignment.Top);
        shortSignInputBox = new ImageInputBox(font,
            shortRect.offset(new Point(5, 3).mul(inputSignsScale), new Point(-4, -11).mul(inputSignsScale)),
            shortRect.withPoint(new Point(-5, -3).mul(inputSignsScale)),
            Rect.XAlignment.Left, Rect.YAlignment.Top,
            TextureResource.smallShortSign,
            false);
        Rect largeRect = new Rect(
            wideRect.min().withX(x -> x + 3 * inputSignsScale),
            TextureResource.largeSign.size.scale(inputSignsScale),
            Rect.XAlignment.Left,
            Rect.YAlignment.Top);
        Rect largeInputRect = largeRect.withPoint(p -> p.add(
            new Point(4 * inputSignsScale, (int) (2.75f * inputSignsScale)))
        ).withSize(
                width -> width - 6 * inputSignsScale,
                height -> font.FONT_HEIGHT
        );
        InputBox firstLarge = new ImageInputBox(font,
            largeInputRect,
            largeRect.withPoint(largeRect.point.subtract(largeInputRect.point)),
            Rect.XAlignment.Left, Rect.YAlignment.Top,
            TextureResource.largeSign,
            false
        );
        final int largeYSpace = (int) (2.5f * inputSignsScale);
        largeInputRect = largeInputRect.withPoint(p -> p.withY(y -> y + largeYSpace));
        InputBox secondLarge = new InputBox(font, largeInputRect, false, false);
        secondLarge.setTextColor(Colors.black);
        largeInputRect = largeInputRect.withPoint(p -> p.withY(y -> y + largeYSpace));
        InputBox thirdLarge = new InputBox(font, largeInputRect, false, false);
        thirdLarge.setTextColor(Colors.black);
        largeInputRect = largeInputRect.withPoint(p -> p.withY(y -> y + largeYSpace));
        InputBox fourthLarge = new InputBox(font, largeInputRect, false, false);
        fourthLarge.setTextColor(Colors.black);

        largeSignInputBoxes = ImmutableList.of(firstLarge, secondLarge, thirdLarge, fourthLarge);
        allSignInputBoxes = ImmutableList.of(wideSignInputBox, shortSignInputBox, firstLarge, secondLarge, thirdLarge, fourthLarge);

        colorInputBox = new ColorInputBox(font,
            new Rect(new Point(wideRect.point.x + wideRect.width / 2, wideRect.point.y + wideRect.height + centerGap), 80, 20, Rect.XAlignment.Center, Rect.YAlignment.Top));
        colorInputBox.setColorResponder(color -> allSignInputBoxes.forEach(b -> b.setTextColor(color)));
        children.add(colorInputBox);
        switchToWide();


        WaystoneLibrary.getInstance().requestAllWaystoneNames(waystoneDropdown::setEntries);
        WaystoneLibrary.getInstance().updateEventDispatcher.addListener(waystoneUpdateListener);
    }

    private ImageInputBox wideSignInputBox;
    private ImageInputBox shortSignInputBox;

    private List<InputBox> largeSignInputBoxes;

    private List<InputBox> allSignInputBoxes;

    @Nullable
    private InputBox currentSignInputBox;
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
    public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
        renderBackground();
        waystoneInputBox.render(p_render_1_, p_render_2_, p_render_3_);
        for (Widget widget: selectionDependentWidgets) {
            widget.render(p_render_1_, p_render_2_, p_render_3_);
        }
        colorInputBox.render(p_render_1_, p_render_2_, p_render_3_);
        super.render(p_render_1_, p_render_2_, p_render_3_);
    }

    private int getCenterX() { return this.width / 2; }
    private int getCenterY() { return this.height / 2; }

    private List<Widget> selectionDependentWidgets = Lists.newArrayList();

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

        switchSignInputBoxTo(largeSignInputBoxes.get(0));

        addTypeDependentChildren(largeSignInputBoxes);
        updateColorBoxPosition(((ImageInputBox) largeSignInputBoxes.get(0)).bounds.max().y);
    }

    private void switchSignInputBoxTo(InputBox box) {
        if(currentSignInputBox != null)
            box.setText(currentSignInputBox.getText());
        currentSignInputBox = box;
    }

    private void updateColorBoxPosition(int otherStuffBottom) {
        colorInputBox.setY(otherStuffBottom + centerGap);
    }

    private void clearTypeDependentChildren(){
        children.removeAll(selectionDependentWidgets);
        selectionDependentWidgets.clear();
    }

    private void addTypeDependentChildren(Collection<? extends Widget> widgets){
        selectionDependentWidgets.addAll(widgets);
        children.addAll(widgets);
    }

    private void addTypeDependentChild(Widget widget){
        selectionDependentWidgets.add(widget);
        children.add(widget);
    }

    @Override
    public void onClose() {
        super.onClose();
        done();
        WaystoneLibrary.getInstance().updateEventDispatcher.removeListener(waystoneUpdateListener);
    }

    private void done() {
        if(isValidWaystone(waystoneInputBox.getText()))
            WaystoneLibrary.getInstance().requestIdFor(waystoneInputBox.getText(), this::apply);
        else apply(Optional.empty());
        getMinecraft().displayGuiScreen(null);
    }

    private void apply(Optional<UUID> destinationId) {
        PostTile.TilePartInfo tilePartInfo = new PostTile.TilePartInfo(tile.getWorld().dimension.getType().getId(), tile.getPos(), UUID.randomUUID());
        switch (selectedType) {
            case Wide:
                PacketHandler.sendToServer(new PostTile.PartAddedEvent.Packet(
                    tilePartInfo,
                    SmallWideSign.METADATA.identifier,
                    SmallWideSign.METADATA.write(
                        new SmallWideSign(
                            Angle.fromDegrees(0),
                            wideSignInputBox.getText(),
                            false,
                            tile.modelType.signTextureLocation,
                            tile.modelType.darkSignTextureLocation,
                            colorInputBox.getCurrentColor(),
                            destinationId
                        )
                    ),
                    new Vector3(0, localHitPos.y > 0.5f ? 0.75f : 0.25f, 0))
                );
                break;
            case Short:
                PacketHandler.sendToServer(new PostTile.PartAddedEvent.Packet(
                    tilePartInfo,
                    SmallShortSign.METADATA.identifier,
                    SmallShortSign.METADATA.write(
                        new SmallShortSign(
                            Angle.fromDegrees(0),
                            shortSignInputBox.getText(),
                            false,
                            tile.modelType.signTextureLocation,
                            colorInputBox.getCurrentColor(),
                            destinationId
                        )
                    ),
                    new Vector3(0, localHitPos.y > 0.5f ? 0.75f : 0.25f, 0))
                );
                break;
            case Large:
                PacketHandler.sendToServer(new PostTile.PartAddedEvent.Packet(
                    tilePartInfo,
                    LargeSign.METADATA.identifier,
                    LargeSign.METADATA.write(
                        new LargeSign(
                            Angle.fromDegrees(0),
                            new String[] {
                                largeSignInputBoxes.get(0).getText(),
                                largeSignInputBoxes.get(1).getText(),
                                largeSignInputBoxes.get(2).getText(),
                                largeSignInputBoxes.get(3).getText(),
                            },
                            false,
                            tile.modelType.signTextureLocation,
                            tile.modelType.darkSignTextureLocation,
                            colorInputBox.getCurrentColor(),
                            destinationId
                        )
                    ),
                    new Vector3(0, 0.5f, 0))
                );
                break;
        }
    }
}
