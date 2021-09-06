package gollorum.signpost.minecraft.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.blockpartdata.types.*;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.events.WaystoneRenamedEvent;
import gollorum.signpost.minecraft.events.WaystoneUpdatedEvent;
import gollorum.signpost.minecraft.gui.utils.*;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.widgets.*;
import gollorum.signpost.minecraft.rendering.FlippableModel;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.relations.ExternalWaystoneLibrary;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.Tuple;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.gui.widget.button.LockIconButton;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SignGui extends ExtendedScreen {

    private enum SignType {
        Wide, Short, Large
    }

    private static final TextureSize typeSelectionButtonsTextureSize = TextureResource.signTypeSelection.size;
    private static final TextureResource waystoneNameTexture = TextureResource.waystoneNameField;
    private static final TextureSize typeSelectionButtonsSize = new TextureSize(typeSelectionButtonsTextureSize.width * 2, typeSelectionButtonsTextureSize.height * 2);
    private static final int typeSelectionButtonsSpace = (int) (typeSelectionButtonsSize.width * 0.3f);

    private static final int typeSelectionButtonsY = 15;
    private static final float typeSelectionButtonsScale = 0.66f;
    private static final float overlayButtonsScale = 0.5f;

    private static final int centralAreaHeight = 110;
    private static final int centerGap = 15;

    private static final float waystoneBoxScale = 2.5f;
    final int inputSignsScale = 5;

    private ImageInputBox waystoneInputBox;
    private DropDownSelection<WaystoneEntry> waystoneDropdown;
    private LockIconButton lockButton;
    private DropDownSelection<AngleSelectionEntry> angleDropDown;

    private TextDisplay rotationLabel;
    private AngleInputBox rotationInputField;

    private final ItemStack itemToDropOnBreak;

    private final Consumer<WaystoneUpdatedEvent> waystoneUpdateListener = event -> {
        WaystoneEntry newEntry = new WaystoneEntry(event.name, event.name, event.handle, event.location.block.blockPos);
        switch(event.getType()) {
            case Added:
                waystoneDropdown.addEntry(newEntry);
                onWaystoneCountChanged();
                break;
            case Removed:
                waystoneDropdown.removeEntry(newEntry);
                onWaystoneCountChanged();
                break;
            case Renamed:
                String oldName = ((WaystoneRenamedEvent)event).oldName;
                WaystoneEntry oldEntry = new WaystoneEntry(oldName, oldName, event.handle, event.location.block.blockPos);
                waystoneDropdown.removeEntry(oldEntry);
                waystoneDropdown.addEntry(newEntry);
                break;
        }
    };

    private static final TextureSize buttonsSize = new TextureSize(98, 20);

    private SignType selectedType = null;
    private final PostTile tile;
    private final ItemStack itemStack;

    private final PostBlock.ModelType modelType;
    private final Vector3 localHitPos;

    private final Optional<SignBlockPart> oldSign;
    private final Optional<PostTile.TilePartInfo> oldTilePartInfo;

    private final List<Flippable> widgetsToFlip = new ArrayList<>();

    private InputBox wideSignInputBox;
    private InputBox shortSignInputBox;

    private List<InputBox> largeSignInputBoxes;

    private List<InputBox> allSignInputBoxes;

    private GuiModelRenderer wideSignRenderer;
    private GuiModelRenderer shortSignRenderer;
    private GuiModelRenderer largeSignRenderer;
    private GuiModelRenderer currentSignRenderer;

    @Nullable
    private InputBox currentSignInputBox;
    private ColorInputBox colorInputBox;

    private String lastWaystone = "";

    @Nullable
    private AngleSelectionEntry waystoneRotationEntry;

    private Optional<Overlay> selectedOverlay;
    private final List<ModelButton> overlaySelectionButtons = new ArrayList<>();

    private boolean hasBeenInitialized = false;

    private TextDisplay noWaystonesInfo;

    public static void display(PostTile tile, PostBlock.ModelType modelType, Vector3 localHitPos, ItemStack itemToDropOnBreak) {
        Minecraft.getInstance().setScreen(new SignGui(tile, modelType, localHitPos, itemToDropOnBreak));
    }

    public static void display(PostTile tile, SignBlockPart oldSign, Vector3 oldOffset, PostTile.TilePartInfo oldTilePartInfo) {
        if(oldSign.hasThePermissionToEdit(tile, Minecraft.getInstance().player))
            Minecraft.getInstance().setScreen(new SignGui(tile, oldSign, oldOffset, oldTilePartInfo));
    }

    public SignGui(PostTile tile, PostBlock.ModelType modelType, Vector3 localHitPos, ItemStack itemToDropOnBreak) {
        super(new TranslationTextComponent(LangKeys.signGuiTitle));
        this.tile = tile;
        this.modelType = modelType;
        this.localHitPos = localHitPos;
        this.itemToDropOnBreak = itemToDropOnBreak;
        oldSign = Optional.empty();
        oldTilePartInfo = Optional.empty();
        itemStack = new ItemStack(tile.getBlockState().getBlock().asItem());
    }

    public SignGui(PostTile tile, SignBlockPart oldSign, Vector3 oldOffset, PostTile.TilePartInfo oldTilePartInfo) {
        super(new TranslationTextComponent(LangKeys.signGuiTitle));
        this.tile = tile;
        this.modelType = oldSign.getModelType();
        this.localHitPos = oldOffset;
        this.itemToDropOnBreak = oldSign.getItemToDropOnBreak();
        this.oldSign = Optional.of(oldSign);
        this.oldTilePartInfo = Optional.of(oldTilePartInfo);
        itemStack = new ItemStack(tile.getBlockState().getBlock().asItem());
    }

    @Override
    protected void init() {
        SignType currentType;
        String currentWaystone;
        String[] currentText;
        boolean isFlipped;
        int currentColor;
        Angle currentAngle;
        if(hasBeenInitialized) {
            currentType = selectedType;
            currentWaystone = waystoneInputBox.getText();
            switch (currentType) {
                case Short:
                    currentText = new String[]{shortSignInputBox.getText()};
                    break;
                case Large:
                    currentText = largeSignInputBoxes.stream().map(InputBox::getText).toArray(String[]::new);
                    break;
                case Wide:
                default:
                    currentText = new String[]{wideSignInputBox.getText()};
                    break;
            }
            isFlipped = widgetsToFlip.get(0).isFlipped();
            currentColor = colorInputBox.getCurrentColor();
            currentAngle = rotationInputField.getCurrentAngle();
        } else {
            currentWaystone = "";
            if(oldSign.isPresent()) {
                if(oldSign.get() instanceof LargeSignBlockPart) {
                    currentType = SignType.Large;
                    LargeSignBlockPart sign = (LargeSignBlockPart) oldSign.get();
                    currentText = sign.getText();
                }
                else if(oldSign.get() instanceof SmallShortSignBlockPart) {
                    currentType = SignType.Short;
                    currentText = new String[]{((SmallShortSignBlockPart) oldSign.get()).getText()};
                }
                else {
                    currentType = SignType.Wide;
                    currentText = new String[]{((SmallWideSignBlockPart) oldSign.get()).getText()};
                }
                isFlipped = oldSign.get().isFlipped();
                currentColor = oldSign.get().getColor();
                currentAngle = oldSign.get().getAngle();
                selectedOverlay = oldSign.get().getOverlay();
            } else {
                currentType = SignType.Wide;
                currentText = new String[]{""};
                isFlipped = true;
                currentColor = 0;
                currentAngle = Angle.ZERO;
                selectedOverlay = Optional.empty();
            }
        }

        additionalRenderables.clear();
        selectedType = null;

        int signTypeSelectionTopY = typeSelectionButtonsY;
        int centerOffset = (typeSelectionButtonsSize.width + typeSelectionButtonsSpace) / 2;

        ResourceLocation postTexture = tile.getParts().stream()
            .filter(p -> p.blockPart instanceof PostBlockPart)
            .map(p -> ((PostBlockPart)p.blockPart).getTexture())
            .findFirst().orElse(tile.modelType.postTexture);
        ResourceLocation mainTexture = oldSign.map(SignBlockPart::getMainTexture).orElse(modelType.mainTexture);
        ResourceLocation secondaryTexture = oldSign.map(SignBlockPart::getSecondaryTexture).orElse(modelType.secondaryTexture);

        FlippableModel postModel = FlippableModel.loadSymmetrical(PostModel.postLocation, postTexture);
        FlippableModel wideModel = FlippableModel.loadFrom(
            PostModel.wideLocation, PostModel.wideFlippedLocation, mainTexture, secondaryTexture
        );
        FlippableModel shortModel = FlippableModel.loadFrom(
            PostModel.shortLocation, PostModel.shortFlippedLocation, mainTexture, secondaryTexture
        );
        FlippableModel largeModel = FlippableModel.loadFrom(
            PostModel.largeLocation, PostModel.largeFlippedLocation, mainTexture, secondaryTexture
        );

        addButton(
            new ModelButton(
                TextureResource.signTypeSelection,
                new Point(getCenterX() - centerOffset, signTypeSelectionTopY),
                typeSelectionButtonsScale,
                Rect.XAlignment.Center, Rect.YAlignment.Top,
                rect -> rect.withPoint(p -> p.add(-4, 0)).scaleCenter(0.75f),
                this::switchToWide,
                new ModelButton.ModelData(postModel, 0, -0.5f, itemStack, RenderType.solid()),
                new ModelButton.ModelData(wideModel, 0, 0.25f, itemStack, RenderType.solid())
            )
        );

        addButton(
            new ModelButton(
                TextureResource.signTypeSelection,
                new Point(getCenterX(), signTypeSelectionTopY),
                typeSelectionButtonsScale,
                Rect.XAlignment.Center, Rect.YAlignment.Top,
                rect -> rect.withPoint(p -> p.add(-11, 0)).scaleCenter(0.75f),
                this::switchToShort,
                new ModelButton.ModelData(postModel, 0, -0.5f, itemStack, RenderType.solid()),
                new ModelButton.ModelData(shortModel, 0, 0.25f, itemStack, RenderType.solid())
            )
        );

        addButton(
            new ModelButton(
                TextureResource.signTypeSelection,
                new Point(getCenterX() + centerOffset, signTypeSelectionTopY),
                typeSelectionButtonsScale,
                Rect.XAlignment.Center, Rect.YAlignment.Top,
                rect -> rect.withPoint(p -> p.add(-3, 0)).scaleCenter(0.75f),
                this::switchToLarge,
                new ModelButton.ModelData(postModel, 0, -0.5f, itemStack, RenderType.solid()),
                new ModelButton.ModelData(largeModel, 0, 0, itemStack, RenderType.solid())
            )
        );

        Rect doneRect = new Rect(new Point(getCenterX(), height - typeSelectionButtonsY), buttonsSize, Rect.XAlignment.Center, Rect.YAlignment.Bottom);
        Button doneButton;
        if(oldSign.isPresent()){
            int buttonsWidth = doneRect.width;
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

        lockButton = new LockIconButton(
            getCenterX() - 10,
            doneRect.point.y - 30,
            b -> lockButton.setLocked(!lockButton.isLocked())
        );
        lockButton.setLocked(oldSign.map(SignBlockPart::isLocked).orElse(false));
        addButton(lockButton);

        Collection<WaystoneEntry> waystoneDropdownEntry = hasBeenInitialized
            ? waystoneDropdown.getAllEntries()
            : new HashSet<>();
        waystoneDropdown = new DropDownSelection<>(font,
            new Point(getCenterX() - centerGap, getCenterY() - centralAreaHeight / 2 + 4 * inputSignsScale),
            Rect.XAlignment.Right,
            Rect.YAlignment.Center,
            (int)(waystoneNameTexture.size.width * waystoneBoxScale) + 3 + DropDownSelection.size.width,
            100,
            (int)((waystoneNameTexture.size.height * waystoneBoxScale) - DropDownSelection.size.height) / 2,
            e -> {
                children.add(e);
                hideStuffOccludedByWaystoneDropdown();
            },
            o -> {
                children.remove(o);
                showStuffOccludedByWaystoneDropdown();
            },
            entry -> {
                waystoneInputBox.setText(entry.displayName);
                waystoneDropdown.hideList();
            },
            false);
        waystoneDropdown.setEntries(waystoneDropdownEntry);
        Rect waystoneInputRect = new Rect(
            new Point(waystoneDropdown.x - 10, waystoneDropdown.y + waystoneDropdown.getHeight() / 2),
            new TextureSize((int)((waystoneNameTexture.size.width - 4) * waystoneBoxScale), (int)((waystoneNameTexture.size.height - 4) * waystoneBoxScale)),
            Rect.XAlignment.Right, Rect.YAlignment.Center);
        waystoneInputBox = new ImageInputBox(font,
            waystoneInputRect,
            new Rect(
                Point.zero,
                waystoneNameTexture.size.scale(waystoneBoxScale),
                Rect.XAlignment.Center, Rect.YAlignment.Center),
            Rect.XAlignment.Center, Rect.YAlignment.Center,
            waystoneNameTexture,
            true, 100);
        waystoneInputBox.setMaxLength(200);
        waystoneInputBox.setResponder(this::onWaystoneSelected);
        noWaystonesInfo = new TextDisplay(
            I18n.get(LangKeys.noWaystones),
            waystoneDropdown.rect.max(),
            Rect.XAlignment.Right, Rect.YAlignment.Bottom,
            font
        );

        int rotationLabelStringWidth = font.width(I18n.get(LangKeys.rotationLabel));
        int rotationLabelWidth = Math.min(rotationLabelStringWidth, waystoneInputBox.width() / 2);
        Rect rotationInputBoxRect = waystoneInputRect.offset(
            new Point(rotationLabelWidth + 10, waystoneInputRect.height + 20),
            new Point(0, waystoneInputRect.height + 20));
        rotationInputField = new AngleInputBox(font, rotationInputBoxRect, 0);
        addButton(rotationInputField);
        angleDropDown = new DropDownSelection<>(
            font,
            new Point(getCenterX() - centerGap, rotationInputBoxRect.center().y),
            Rect.XAlignment.Right,
            Rect.YAlignment.Center,
            (int)(waystoneNameTexture.size.width * waystoneBoxScale) + DropDownSelection.size.width,
            75,
            (int)((waystoneNameTexture.size.height * waystoneBoxScale) - DropDownSelection.size.height) / 2,
            e -> {
                children.add(e);
                removeButtons(overlaySelectionButtons);
            },
            o -> {
                children.remove(o);
                addButtons(overlaySelectionButtons);
            },
            entry -> {
                rotationInputField.setText(entry.angleToString());
                angleDropDown.hideList();
            },
            false
        );
        angleDropDown.setEntries(new HashSet<>());
        angleDropDown.addEntry(angleEntryForPlayer());
        addButton(angleDropDown);
        rotationLabel = new TextDisplay(
            I18n.get(LangKeys.rotationLabel),
            rotationInputBoxRect.at(Rect.XAlignment.Left, Rect.YAlignment.Center).add(-10, 0),
            Rect.XAlignment.Right, Rect.YAlignment.Center,
            font
        );
        additionalRenderables.add(rotationLabel);

        Rect modelRect = new Rect(
            new Point(getCenterX() + centerGap + 3 * inputSignsScale, getCenterY() - centralAreaHeight / 2),
            new TextureSize(22, 16).scale(inputSignsScale),
            Rect.XAlignment.Left,
            Rect.YAlignment.Top);
        GuiModelRenderer postRenderer = new GuiModelRenderer(
            modelRect, postModel,
            0, -0.5f,
            new ItemStack(PostBlock.OAK.block.asItem()),
            RenderType.solid()
        );
        additionalRenderables.add(postRenderer);
        Point modelRectTop = modelRect.at(Rect.XAlignment.Center, Rect.YAlignment.Top);

        Rect wideInputRect = new Rect(
            modelRectTop.add(-7 * inputSignsScale, 2 * inputSignsScale),
            modelRectTop.add(11 * inputSignsScale, 6 * inputSignsScale));
        wideSignInputBox = new InputBox(font,
            wideInputRect,
            false, false, 100);
        wideSignInputBox.setTextColor(Colors.black);
        widgetsToFlip.add(new FlippableAtPivot(wideSignInputBox, modelRectTop.x));

        wideSignRenderer = new GuiModelRenderer(
            modelRect, wideModel,
            0, 0.25f,
            itemStack,
            RenderType.solid()
        );
        widgetsToFlip.add(wideSignRenderer);

        Rect shortInputRect = new Rect(
            modelRectTop.add(3 * inputSignsScale, 2 * inputSignsScale),
            modelRectTop.add(14 * inputSignsScale, 6 * inputSignsScale));
        shortSignInputBox = new InputBox(
            font,
            shortInputRect,
            false, false, 100
        );
        shortSignInputBox.setTextColor(Colors.black);
        widgetsToFlip.add(new FlippableAtPivot(shortSignInputBox, modelRectTop.x));

        shortSignRenderer = new GuiModelRenderer(
            modelRect, shortModel,
            0, 0.25f,
            itemStack,
            RenderType.solid()
        );
        widgetsToFlip.add(shortSignRenderer);

        Rect largeInputRect = new Rect(
            modelRectTop.add(-7 * inputSignsScale, 3 * inputSignsScale),
            modelRectTop.add(9 * inputSignsScale, 14 * inputSignsScale))
            .withHeight(height -> height / 4 - 1);
        InputBox firstLarge = new InputBox(font, largeInputRect, false, false, 100);
        firstLarge.setTextColor(Colors.black);
        largeInputRect = largeInputRect.withPoint(p -> p.withY(Math.round(modelRectTop.y + (13 - 3 * 2.5f) * inputSignsScale)));
        InputBox secondLarge = new InputBox(font, largeInputRect, false, false, 100);
        secondLarge.setTextColor(Colors.black);
        largeInputRect = largeInputRect.withPoint(p -> p.withY(Math.round(modelRectTop.y + (13 - 2 * 2.5f) * inputSignsScale)));
        InputBox thirdLarge = new InputBox(font, largeInputRect, false, false, 100);
        thirdLarge.setTextColor(Colors.black);
        largeInputRect = largeInputRect.withPoint(p -> p.withY(Math.round(modelRectTop.y + (13 - 1 * 2.5f) * inputSignsScale)));
        InputBox fourthLarge = new InputBox(font, largeInputRect, false, false, 100);
        fourthLarge.setTextColor(Colors.black);
        firstLarge.addKeyCodeListener(KeyCodes.Down, () -> setInitialFocus(secondLarge));
        secondLarge.addKeyCodeListener(KeyCodes.Up, () -> setInitialFocus(firstLarge));
        secondLarge.addKeyCodeListener(KeyCodes.Down, () -> setInitialFocus(thirdLarge));
        thirdLarge.addKeyCodeListener(KeyCodes.Up, () -> setInitialFocus(secondLarge));
        thirdLarge.addKeyCodeListener(KeyCodes.Down, () -> setInitialFocus(fourthLarge));
        fourthLarge.addKeyCodeListener(KeyCodes.Up, () -> setInitialFocus(thirdLarge));
        widgetsToFlip.add(new FlippableAtPivot(firstLarge, modelRectTop.x));
        widgetsToFlip.add(new FlippableAtPivot(secondLarge, modelRectTop.x));
        widgetsToFlip.add(new FlippableAtPivot(thirdLarge, modelRectTop.x));
        widgetsToFlip.add(new FlippableAtPivot(fourthLarge, modelRectTop.x));

        largeSignRenderer = new GuiModelRenderer(
            modelRect, largeModel,
            0, 0,
            itemStack,
            RenderType.solid()
        );
        widgetsToFlip.add(largeSignRenderer);

        largeSignInputBoxes = ImmutableList.of(firstLarge, secondLarge, thirdLarge, fourthLarge);
        allSignInputBoxes = ImmutableList.of(wideSignInputBox, shortSignInputBox, firstLarge, secondLarge, thirdLarge, fourthLarge);

        Button switchDirectionButton = newImageButton(
            TextureResource.flipDirection,
            0,
            new Point(modelRect.point.x, modelRect.max().y + centerGap),
            1,
            Rect.XAlignment.Left, Rect.YAlignment.Top,
            this::flip
        );
        addButton(switchDirectionButton);

        colorInputBox = new ColorInputBox(font,
            new Rect(
                new Point(switchDirectionButton.x + switchDirectionButton.getWidth() + 20, switchDirectionButton.y + switchDirectionButton.getHeight() / 2),
                80, 20,
                Rect.XAlignment.Left, Rect.YAlignment.Center
            ), 0);
        colorInputBox.setColorResponder(color -> allSignInputBoxes.forEach(b -> b.setTextColor(color)));
        addButton(colorInputBox);

        overlaySelectionButtons.clear();
        int i = 0;
        for(Overlay overlay: Overlay.getAllOverlays()) {
            FlippableModel overlayModel = FlippableModel.loadFrom(
                PostModel.wideOverlayLocation, PostModel.wideOverlayFlippedLocation, overlay.textureFor(SmallWideSignBlockPart.class)
            ).withTintIndex(overlay.tintIndex);
            overlaySelectionButtons.add(new ModelButton(
                TextureResource.signTypeSelection, new Point(getCenterX() - centerGap - i * 37, rotationInputBoxRect.max().y + 15),
                overlayButtonsScale, Rect.XAlignment.Right, Rect.YAlignment.Top,
                rect -> rect.withPoint(p -> p.add(Math.round(-4 / typeSelectionButtonsScale * overlayButtonsScale), 0)).scaleCenter(0.75f),
                () -> switchOverlay(Optional.of(overlay)),
                new ModelButton.ModelData(postModel, 0, -0.5f, itemStack, RenderType.solid()),
                new ModelButton.ModelData(wideModel, 0, 0.25f, itemStack, RenderType.solid()),
                new ModelButton.ModelData(overlayModel, 0, 0.25f, itemStack, RenderType.cutout())
            ));
            i++;
        }
        if(i > 0)
            overlaySelectionButtons.add(new ModelButton(
                TextureResource.signTypeSelection, new Point(getCenterX() - centerGap - i * 37, rotationInputBoxRect.max().y + 15),
                overlayButtonsScale, Rect.XAlignment.Right, Rect.YAlignment.Top,
                rect -> rect.withPoint(p -> p.add(Math.round(-4 / typeSelectionButtonsScale * overlayButtonsScale), 0)).scaleCenter(0.75f),
                () -> switchOverlay(Optional.empty()),
                new ModelButton.ModelData(postModel, 0, -0.5f, itemStack, RenderType.solid()),
                new ModelButton.ModelData(wideModel, 0, 0.25f, itemStack, RenderType.solid())
            ));
        addButtons(overlaySelectionButtons);


        switchTo(currentType);
        switchOverlay(selectedOverlay);
        waystoneInputBox.setText(currentWaystone);
        switch (currentType) {
            case Wide:
                wideSignInputBox.setText(currentText[0]);
                break;
            case Short:
                shortSignInputBox.setText(currentText[0]);
                break;
            case Large:
                for(i = 0; i < largeSignInputBoxes.size(); i++) {
                    largeSignInputBoxes.get(i).setText(currentText[i]);
                }
                break;
        }
        if(isFlipped) flip();
        colorInputBox.setSelectedColor(currentColor);
        rotationInputField.setSelectedAngle(Angle.fromDegrees(Math.round(currentAngle.degrees())));


        if(hasBeenInitialized) {
            onWaystoneCountChanged();
        } else {
            String unknownWaystone = new TranslationTextComponent(LangKeys.unknownWaystone)
                .withStyle(style -> style.withColor(Color.fromRgb(Colors.darkGrey)))
                .getString();
            Optional<WaystoneEntry> oldWaystone = oldSign
                .<WaystoneHandle>flatMap(SignBlockPart::getDestination)
                .map(handle -> new WaystoneEntry(
                    unknownWaystone,
                    unknownWaystone,
                    handle,
                    tile.getBlockPos().offset(
                        new Vector3(100, 0, 0).rotateY(oldSign.get().getAngle()).toBlockPos()
                    )
                ));
            oldWaystone.ifPresent(text -> {
                waystoneDropdown.addEntry(text);
                waystoneInputBox.setText(text.entryName);
            });
            Consumer<Function<WaystoneHandle, Optional<Tuple<Tuple<String, String>, BlockPos>>>> setupFromSign = map -> {
                oldWaystone.ifPresent(oldWs -> {
                    Optional<Tuple<Tuple<String, String>, BlockPos>> name = map.apply(oldWs.handle);
                    if (name.isPresent()) {
                        oldWs.entryName = name.get()._1._1;
                        oldWs.displayName = name.get()._1._2;
                        oldWs.pos = name.get()._2;
                        waystoneInputBox.setText(oldWs.entryName);
                    }
                });
                onWaystoneCountChanged();
            };
            WaystoneLibrary.getInstance().requestAllWaystones(n -> {
                waystoneDropdown.addEntries(n.entrySet().stream().map(e -> new WaystoneEntry(
                    e.getValue()._1,
                    e.getValue()._1,
                    e.getKey(),
                    e.getValue()._2.block.blockPos
                )).filter(e -> oldWaystone.map(oldE -> !e.handle.equals(oldE.handle)).orElse(true))
                    .collect(Collectors.toList()));
                setupFromSign.accept(id ->
                    id instanceof WaystoneHandle.Vanilla
                        ? Optional.ofNullable(n.get(id))
                            .map(e -> Tuple.of(e._1, e._1, e._2.block.blockPos))
                        : Optional.empty());
            }, Optional.of(PlayerHandle.from(getMinecraft().player)), true);
            ExternalWaystoneLibrary.getInstance().requestKnownWaystones(n -> {
                Stream<WaystoneEntry> entries = n.stream().map(w -> new WaystoneEntry(
                    w.name() + " " + w.handle().modMark(),
                    w.name(),
                    w.handle(),
                    w.loc().blockPos
                ));
                waystoneDropdown.addEntries(entries.filter(e -> oldWaystone.map(oldE -> !e.handle.equals(oldE.handle)).orElse(true))
                    .collect(Collectors.toList()));
                setupFromSign.accept(id -> entries.filter(e -> e.handle.equals(id)).findFirst().map(e -> Tuple.of(e.entryName, e.displayName, e.pos)));
            });
            WaystoneLibrary.getInstance().updateEventDispatcher.addListener(waystoneUpdateListener);
        }

        final int newSignItemSize = 16;
        TextDisplay newSignHint = new TextDisplay(
            I18n.get(LangKeys.newSignHint),
            new Point(getCenterX() - newSignItemSize, (int) ((doneButton.y + doneButton.getHeight() + height) / 2f)),
            Rect.XAlignment.Center, Rect.YAlignment.Center,
            font
        );
        additionalRenderables.add(newSignHint);
        GuiItemRenderer ir = new GuiItemRenderer(
            new Rect(newSignHint.rect.at(Rect.XAlignment.Right, Rect.YAlignment.Center), newSignItemSize, newSignItemSize, Rect.XAlignment.Left, Rect.YAlignment.Center),
            itemToDropOnBreak
        );
        additionalRenderables.add(ir);
        AtomicReference<Runnable> cycleItem = new AtomicReference<>();
        AtomicInteger cycleItemIndex = new AtomicInteger(0);
        AtomicInteger cycleItemIngredientIndex = new AtomicInteger(0);
        AtomicLong nextCycleAt = new AtomicLong(System.currentTimeMillis());
        cycleItem.set(() -> {
            ItemStack[] options = PostBlock.AllVariants.get(cycleItemIndex.get()).type.addSignIngredient.get().getItems();
            ir.setItemStack(options[cycleItemIngredientIndex.get()]);
            if(cycleItemIngredientIndex.get() >= options.length - 1) {
                cycleItemIndex.set((cycleItemIndex.get() + 1) % PostBlock.AllVariants.size());
                cycleItemIngredientIndex.set(0);
            } else cycleItemIngredientIndex.incrementAndGet();
            nextCycleAt.set(nextCycleAt.get() + (options.length < 2 ? 1500 : (options.length == 2 ? 1000 : 500)));
            Delay.onClientUntil(
                () -> System.currentTimeMillis() >= nextCycleAt.get(),
                () -> cycleItem.get().run()
            );
        });
        cycleItem.get().run();

        hasBeenInitialized = true;
    }

    private void onWaystoneCountChanged() {
        if(waystoneDropdown.getAllEntries().isEmpty()){
            additionalRenderables.add(noWaystonesInfo);
        } else {
            addButton(waystoneDropdown);
            addButton(waystoneInputBox);
        }
    }

    private void flip() {
        AngleSelectionEntry playerAngleEntry = angleEntryForPlayer();
        boolean shouldPointAtPlayer = Math.round(Math.abs(playerAngleEntry.angleGetter.get().degrees()
            - rotationInputField.getCurrentAngle().degrees())) <= 1;
        widgetsToFlip.forEach(Flippable::flip);
        if(shouldPointAtPlayer)
            rotationInputField.setText(playerAngleEntry.angleToString());
    }

    private void onWaystoneSelected(String waystoneName) {
        boolean shouldOverrideRotation = rotationInputField.getCurrentAngle().equals(Angle.ZERO)
            || rotationInputField.getCurrentAngle().isNearly(angleEntryForPlayer().angleGetter.get(), Angle.fromDegrees(1));
        if(waystoneRotationEntry != null) {
            shouldOverrideRotation |= waystoneRotationEntry.angleGetter.get().isNearly(rotationInputField.getCurrentAngle(), Angle.fromDegrees(1));
            angleDropDown.removeEntry(waystoneRotationEntry);
        }
        Optional<WaystoneEntry> validWaystone = asValidWaystone(waystoneName);
        if(waystoneName.equals("") || validWaystone.isPresent()) {
            waystoneInputBox.setTextColor(Colors.valid);
            waystoneInputBox.setTextColorUneditable(Colors.validInactive);
            waystoneDropdown.setFilter(name -> true);
            if(currentSignInputBox != null && lastWaystone.equals(currentSignInputBox.getText()))
                currentSignInputBox.setText(waystoneName);
            if(!waystoneName.equals("")) {
                waystoneRotationEntry = angleEntryForWaystone(validWaystone.get());
                angleDropDown.addEntry(waystoneRotationEntry);
                if(shouldOverrideRotation)
                    rotationInputField.setSelectedAngle(waystoneRotationEntry.angleGetter.get());
            }
            lastWaystone = waystoneName;
        } else {
            waystoneInputBox.setTextColor(Colors.invalid);
            waystoneInputBox.setTextColorUneditable(Colors.invalidInactive);
            waystoneDropdown.setFilter(e -> e.entryName.toLowerCase().contains(waystoneName.toLowerCase()));
            if(currentSignInputBox != null && lastWaystone.equals(currentSignInputBox.getText()))
                currentSignInputBox.setText("");
        }
    }

    private Optional<WaystoneEntry> asValidWaystone(String name){
        Optional<WaystoneEntry> ret = waystoneDropdown.getAllEntries().stream().filter(e -> e.entryName.equals(name))
            .findFirst();
        if(ret.isPresent()) return ret;
        else return waystoneDropdown.getAllEntries().stream().filter(e -> e.displayName.equals(name))
            .findFirst();
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

    private int getCenterX() { return this.width / 2; }
    private int getCenterY() { return this.height / 2; }

    private final List<Widget> selectionDependentWidgets = Lists.newArrayList();

    private void switchTo(SignType type) {
        switch (type) {
            case Wide:
                switchToWide();
                break;
            case Short:
                switchToShort();
                break;
            case Large:
                switchToLarge();
                break;
            default:
                throw new RuntimeException("Sign type " + type + " is not supported");
        }
    }

    private void switchToWide(){
        if(selectedType == SignType.Wide) return;
        clearTypeDependentChildren();
        selectedType = SignType.Wide;

        switchSignInputBoxTo(wideSignInputBox);

        addTypeDependentChild(wideSignInputBox);
        additionalRenderables.add(wideSignRenderer);
        currentSignRenderer = wideSignRenderer;
        switchOverlay(selectedOverlay);
    }

    private void switchToShort(){
        if(selectedType == SignType.Short) return;
        clearTypeDependentChildren();
        selectedType = SignType.Short;

        switchSignInputBoxTo(shortSignInputBox);

        addTypeDependentChild(shortSignInputBox);
        additionalRenderables.add(shortSignRenderer);
        currentSignRenderer = shortSignRenderer;
        switchOverlay(selectedOverlay);
    }

    private void switchToLarge(){
        if(selectedType == SignType.Large) return;
        clearTypeDependentChildren();
        selectedType = SignType.Large;

        switchSignInputBoxTo(largeSignInputBoxes.get(0));

        addTypeDependentChildren(largeSignInputBoxes);
        additionalRenderables.add(largeSignRenderer);
        currentSignRenderer = largeSignRenderer;
        switchOverlay(selectedOverlay);
    }

    private GuiModelRenderer currentOverlay;

    private void switchOverlay(Optional<Overlay> overlay) {
        if(currentOverlay != null) {
            additionalRenderables.remove(currentOverlay);
            widgetsToFlip.remove(currentOverlay);
        }
        this.selectedOverlay = overlay;
        if(!overlay.isPresent()) return;
        Overlay o = overlay.get();
        switch(selectedType) {
            case Wide:
                currentOverlay = new GuiModelRenderer(
                    wideSignRenderer.rect,
                    FlippableModel.loadFrom(PostModel.wideOverlayLocation, PostModel.wideOverlayFlippedLocation, o.textureFor(SmallWideSignBlockPart.class))
                        .withTintIndex(o.tintIndex),
                    0, 0.25f, itemStack,
                    RenderType.cutout()
                );
                break;
            case Short:
                currentOverlay = new GuiModelRenderer(
                    shortSignRenderer.rect,
                    FlippableModel.loadFrom(PostModel.shortOverlayLocation, PostModel.shortOverlayFlippedLocation, o.textureFor(SmallShortSignBlockPart.class))
                        .withTintIndex(o.tintIndex),
                    0, 0.25f, itemStack,
                    RenderType.cutout()
                );
                break;
            case Large:
                currentOverlay = new GuiModelRenderer(
                    largeSignRenderer.rect,
                    FlippableModel.loadFrom(PostModel.largeOverlayLocation, PostModel.largeOverlayFlippedLocation, o.textureFor(LargeSignBlockPart.class))
                        .withTintIndex(o.tintIndex),
                    0, 0, itemStack,
                    RenderType.cutout()
                );
                break;
        }
        additionalRenderables.add(currentOverlay);
        if(currentSignRenderer.isFlipped()) currentOverlay.flip();
        widgetsToFlip.add(currentOverlay);
    }

    private void hideStuffOccludedByWaystoneDropdown() {
        additionalRenderables.remove(rotationLabel);
        removeButton(rotationInputField);
        angleDropDown.hideList();
        removeButton(angleDropDown);
        removeButtons(overlaySelectionButtons);
    }

    private void showStuffOccludedByWaystoneDropdown() {
        additionalRenderables.add(rotationLabel);
        addButton(rotationInputField);
        addButton(angleDropDown);
        addButtons(overlaySelectionButtons);
    }

    private void switchSignInputBoxTo(InputBox box) {
        if(currentSignInputBox != null)
            box.setText(currentSignInputBox.getText());
        currentSignInputBox = box;
    }

    private void clearTypeDependentChildren(){
        removeButtons(selectionDependentWidgets);
        additionalRenderables.remove(currentSignRenderer);
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
        getMinecraft().setScreen(null);
    }

    private void done() {
        apply(asValidWaystone(waystoneInputBox.getText()).map(w -> w.handle));
        getMinecraft().setScreen(null);
    }

    private void apply(Optional<WaystoneHandle> destinationId) {
        PostTile.TilePartInfo tilePartInfo = oldTilePartInfo.orElseGet(() ->
            new PostTile.TilePartInfo(tile.getLevel().dimension().location(), tile.getBlockPos(), UUID.randomUUID()));
        CompoundNBT data;
        boolean isLocked = lockButton.isLocked();
        ResourceLocation mainTex = oldSign.map(SignBlockPart::getMainTexture).orElse(modelType.mainTexture);
        ResourceLocation secondaryTex = oldSign.map(SignBlockPart::getSecondaryTexture).orElse(modelType.secondaryTexture);
        switch (selectedType) {
            case Wide:
                data = SmallWideSignBlockPart.METADATA.write(
                    new SmallWideSignBlockPart(
                        rotationInputField.getCurrentAngle(),
                        wideSignInputBox.getText(),
                        wideSignRenderer.isFlipped(),
                        mainTex,
                        secondaryTex,
                        selectedOverlay,
                        colorInputBox.getCurrentColor(),
                        destinationId,
                        itemToDropOnBreak,
                        modelType,
                        isLocked
                    )
                );
                if(oldSign.isPresent()) {
                    PacketHandler.sendToServer(new PostTile.PartMutatedEvent.Packet(
                        tilePartInfo, data,
                        SmallWideSignBlockPart.METADATA.identifier,
                        new Vector3(0, localHitPos.y > 0.5f ? 0.75f : 0.25f, 0)
                    ));
                } else {
                    PacketHandler.sendToServer(new PostTile.PartAddedEvent.Packet(
                        tilePartInfo, data,
                        SmallWideSignBlockPart.METADATA.identifier,
                        new Vector3(0, localHitPos.y > 0.5f ? 0.75f : 0.25f, 0), itemToDropOnBreak, PlayerHandle.from(getMinecraft().player)
                    ));
                }
                break;
            case Short:
                data = SmallShortSignBlockPart.METADATA.write(
                    new SmallShortSignBlockPart(
                        rotationInputField.getCurrentAngle(),
                        shortSignInputBox.getText(),
                        shortSignRenderer.isFlipped(),
                        mainTex,
                        secondaryTex,
                        selectedOverlay,
                        colorInputBox.getCurrentColor(),
                        destinationId,
                        itemToDropOnBreak,
                        modelType,
                        isLocked
                    )
                );
                if(oldSign.isPresent()) {
                    PacketHandler.sendToServer(new PostTile.PartMutatedEvent.Packet(
                        tilePartInfo, data,
                        SmallShortSignBlockPart.METADATA.identifier,
                        new Vector3(0, localHitPos.y > 0.5f ? 0.75f : 0.25f, 0)
                    ));
                } else {
                    PacketHandler.sendToServer(new PostTile.PartAddedEvent.Packet(
                        tilePartInfo, data,
                        SmallShortSignBlockPart.METADATA.identifier,
                        new Vector3(0, localHitPos.y > 0.5f ? 0.75f : 0.25f, 0), itemToDropOnBreak, PlayerHandle.from(getMinecraft().player)
                    ));
                }
                break;
            case Large:
                data = LargeSignBlockPart.METADATA.write(
                    new LargeSignBlockPart(
                        rotationInputField.getCurrentAngle(),
                        new String[] {
                            largeSignInputBoxes.get(0).getText(),
                            largeSignInputBoxes.get(1).getText(),
                            largeSignInputBoxes.get(2).getText(),
                            largeSignInputBoxes.get(3).getText(),
                        },
                        currentSignRenderer.isFlipped(),
                        mainTex,
                        secondaryTex,
                        selectedOverlay,
                        colorInputBox.getCurrentColor(),
                        destinationId,
                        itemToDropOnBreak,
                        modelType,
                        isLocked
                    )
                );
                if(oldSign.isPresent()) {
                    PacketHandler.sendToServer(new PostTile.PartMutatedEvent.Packet(
                        tilePartInfo, data,
                        LargeSignBlockPart.METADATA.identifier,
                        new Vector3(0, localHitPos.y >= 0.5f ? 0.501f : 0.499f, 0)
                    ));
                } else {
                    PacketHandler.sendToServer(new PostTile.PartAddedEvent.Packet(
                        tilePartInfo, data,
                        LargeSignBlockPart.METADATA.identifier,
                        new Vector3(0, 0.5f, 0), itemToDropOnBreak, PlayerHandle.from(getMinecraft().player)
                    ));
                }
                break;
        }
    }

    private AngleSelectionEntry angleEntryForWaystone(WaystoneEntry waystone) {
        AtomicReference<Angle> angle = new AtomicReference<>(Angle.fromDegrees(404));
        angle.set(SignBlockPart.pointingAt(tile.getBlockPos(), waystone.pos));
        return new AngleSelectionEntry(LangKeys.rotationWaystone, angle::get);
    }

    private AngleSelectionEntry angleEntryForPlayer() {
        AtomicReference<Angle> angleWhenFlipped = new AtomicReference<>(Angle.fromDegrees(404));
        AtomicReference<Angle> angleWhenNotFlipped = new AtomicReference<>(Angle.fromDegrees(404));
        Delay.onClientUntil(() -> getMinecraft() != null && getMinecraft().player != null, () -> {
            angleWhenFlipped.set(Angle.fromDegrees(-getMinecraft().player.yRot).normalized());
            angleWhenNotFlipped.set(angleWhenFlipped.get().add(Angle.fromRadians((float) Math.PI)).normalized());

            if(!oldSign.isPresent() && rotationInputField.getCurrentAngle().equals(Angle.ZERO)) {
                Delay.onClientUntil(
                    () -> widgetsToFlip.size() > 0,
                    () -> rotationInputField.setSelectedAngle((widgetsToFlip.get(0).isFlipped() ? angleWhenFlipped : angleWhenNotFlipped).get()),
                    100,
                    Optional.empty()
                );
            }
        });
        return new AngleSelectionEntry(LangKeys.rotationPlayer,
            () -> (widgetsToFlip.get(0).isFlipped() ? angleWhenFlipped : angleWhenNotFlipped).get());
    }

    private static class AngleSelectionEntry {

        private final String langKey;
        public final Supplier<Angle> angleGetter;

        private AngleSelectionEntry(String langKey, Supplier<Angle> angleGetter) {
            this.langKey = langKey;
            this.angleGetter = angleGetter;
        }

        public String angleToString() {
            return Math.round(angleGetter.get().degrees()) + AngleInputBox.degreeSign;
        }

        @Override
        public String toString() {
            return I18n.get(langKey, angleToString());
        }
    }

}
