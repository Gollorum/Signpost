package gollorum.signpost.minecraft.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.*;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.events.WaystoneRenamedEvent;
import gollorum.signpost.minecraft.events.WaystoneUpdatedEvent;
import gollorum.signpost.minecraft.gui.utils.*;
import gollorum.signpost.minecraft.gui.widgets.*;
import gollorum.signpost.minecraft.rendering.FlippableModel;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.utils.*;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import org.openjdk.nashorn.internal.runtime.options.Option;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    private Optional<WaystoneEntry> lastWaystone = Optional.empty();

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
        super(new TranslatableComponent(LangKeys.signGuiTitle));
        this.tile = tile;
        this.modelType = modelType;
        this.localHitPos = localHitPos;
        this.itemToDropOnBreak = itemToDropOnBreak;
        oldSign = Optional.empty();
        oldTilePartInfo = Optional.empty();
        itemStack = new ItemStack(tile.getBlockState().getBlock().asItem());
    }

    public SignGui(PostTile tile, SignBlockPart oldSign, Vector3 oldOffset, PostTile.TilePartInfo oldTilePartInfo) {
        super(new TranslatableComponent(LangKeys.signGuiTitle));
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
        NameProvider[] currentText;
        boolean isFlipped;
        int currentColor;
        Angle currentAngle;
        if(hasBeenInitialized) {
            currentType = selectedType;
            currentWaystone = waystoneInputBox.getValue();
            currentText = switch (currentType) {
                case Short -> new NameProvider[]{asNameProvider(shortSignInputBox.getValue())};
                case Large -> largeSignInputBoxes.stream().map(InputBox::getValue).map(this::asNameProvider).toArray(NameProvider[]::new);
                default -> new NameProvider[]{asNameProvider(wideSignInputBox.getValue())};
            };
            isFlipped = widgetsToFlip.get(0).isFlipped();
            widgetsToFlip.clear();
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
                    currentText = new NameProvider[]{((SmallShortSignBlockPart) oldSign.get()).getText()};
                }
                else {
                    currentType = SignType.Wide;
                    currentText = new NameProvider[]{((SmallWideSignBlockPart) oldSign.get()).getText()};
                }
                isFlipped = oldSign.get().isFlipped();
                currentColor = oldSign.get().getColor();
                currentAngle = oldSign.get().getAngle().get();
                selectedOverlay = oldSign.get().getOverlay();
            } else {
                currentType = SignType.Wide;
                currentText = new NameProvider[]{new NameProvider.Literal("")};
                isFlipped = true;
                currentColor = 0;
                currentAngle = Angle.ZERO;
                selectedOverlay = Optional.empty();
            }
        }
        super.init();
        selectedType = null;

        int signTypeSelectionTopY = typeSelectionButtonsY;
        int centerOffset = (typeSelectionButtonsSize.width + typeSelectionButtonsSpace) / 2;

        var postTexture = tile.getParts().stream()
            .filter(p -> p.blockPart instanceof PostBlockPart)
            .map(p -> ((PostBlockPart)p.blockPart).getTexture())
            .findFirst().orElse(tile.modelType.postTexture);
        var mainTexture = oldSign.map(SignBlockPart::getMainTexture).orElse(modelType.mainTexture);
        var secondaryTexture = oldSign.map(SignBlockPart::getSecondaryTexture).orElse(modelType.secondaryTexture);

        FlippableModel postModel = FlippableModel.loadSymmetrical(PostModel.postLocation, postTexture.location());
        FlippableModel wideModel = FlippableModel.loadFrom(
            PostModel.wideLocation, PostModel.wideFlippedLocation, mainTexture.location(), secondaryTexture.location()
        );
        FlippableModel shortModel = FlippableModel.loadFrom(
            PostModel.shortLocation, PostModel.shortFlippedLocation, mainTexture.location(), secondaryTexture.location()
        );
        FlippableModel largeModel = FlippableModel.loadFrom(
            PostModel.largeLocation, PostModel.largeFlippedLocation, mainTexture.location(), secondaryTexture.location()
        );

        addRenderableWidget(
            new ModelButton(
                TextureResource.signTypeSelection,
                new Point(getCenterX() - centerOffset, signTypeSelectionTopY),
                typeSelectionButtonsScale,
                Rect.XAlignment.Center, Rect.YAlignment.Top,
                rect -> rect.withPoint(p -> p.add(-4, 0)).scaleCenter(0.75f),
                this::switchToWide,
                new ModelButton.ModelData(postModel, 0, -0.5f, itemStack, RenderType.solid(), new int[]{colorFrom(postTexture.tint())}),
                new ModelButton.ModelData(wideModel, 0, 0.25f, itemStack, RenderType.solid(), new int[]{colorFrom(mainTexture.tint()), colorFrom(secondaryTexture.tint())})
            )
        );

        addRenderableWidget(
            new ModelButton(
                TextureResource.signTypeSelection,
                new Point(getCenterX(), signTypeSelectionTopY),
                typeSelectionButtonsScale,
                Rect.XAlignment.Center, Rect.YAlignment.Top,
                rect -> rect.withPoint(p -> p.add(-11, 0)).scaleCenter(0.75f),
                this::switchToShort,
                new ModelButton.ModelData(postModel, 0, -0.5f, itemStack, RenderType.solid(), new int[]{colorFrom(postTexture.tint())}),
                new ModelButton.ModelData(shortModel, 0, 0.25f, itemStack, RenderType.solid(), new int[]{colorFrom(mainTexture.tint()), colorFrom(secondaryTexture.tint())})
            )
        );

        addRenderableWidget(
            new ModelButton(
                TextureResource.signTypeSelection,
                new Point(getCenterX() + centerOffset, signTypeSelectionTopY),
                typeSelectionButtonsScale,
                Rect.XAlignment.Center, Rect.YAlignment.Top,
                rect -> rect.withPoint(p -> p.add(-3, 0)).scaleCenter(0.75f),
                this::switchToLarge,
                new ModelButton.ModelData(postModel, 0, -0.5f, itemStack, RenderType.solid(), new int[]{colorFrom(postTexture.tint())}),
                new ModelButton.ModelData(largeModel, 0, 0, itemStack, RenderType.solid(), new int[]{colorFrom(mainTexture.tint()), colorFrom(secondaryTexture.tint())})
            )
        );

        Rect doneRect = new Rect(new Point(getCenterX(), height - typeSelectionButtonsY), buttonsSize, Rect.XAlignment.Center, Rect.YAlignment.Bottom);
        Button doneButton;
        if(oldSign.isPresent()){
            int buttonsWidth = doneRect.width;
            doneButton = new Button(
                getCenterX() + centerGap / 2, doneRect.point.y, buttonsWidth, doneRect.height,
                new TranslatableComponent(LangKeys.done),
                b -> done()
            );
            Button removeSignButton = new Button(
                getCenterX() - centerGap / 2 - buttonsWidth, doneRect.point.y, buttonsWidth, doneRect.height,
                new TranslatableComponent(LangKeys.removeSign),
                b -> removeSign()
            );
            removeSignButton.setFGColor(Colors.invalid);
            addRenderableWidget(removeSignButton);
        } else {
            doneButton = new Button(
                doneRect.point.x, doneRect.point.y, doneRect.width, doneRect.height,
                new TranslatableComponent(LangKeys.done),
                b -> done()
            );
        }
        addRenderableWidget(doneButton);

        lockButton = new LockIconButton(
            getCenterX() - 10,
            doneRect.point.y - 30,
            b -> lockButton.setLocked(!lockButton.isLocked())
        );
        lockButton.setLocked(oldSign.map(SignBlockPart::isLocked).orElse(false));
        addRenderableWidget(lockButton);

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
                addWidget(e);
                hideStuffOccludedByWaystoneDropdown();
            },
            o -> {
                removeWidget(o);
                showStuffOccludedByWaystoneDropdown();
            },
            entry -> {
                waystoneInputBox.setValue(entry.entryName);
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
                new Point(-4, 0),
                waystoneNameTexture.size.scale(waystoneBoxScale),
                Rect.XAlignment.Center, Rect.YAlignment.Center),
            Rect.XAlignment.Center, Rect.YAlignment.Center,
            waystoneNameTexture,
            true, 100);
        waystoneInputBox.setBlitOffset(100);
        waystoneInputBox.setMaxLength(200);
        waystoneInputBox.setResponder(this::onWaystoneSelected);
        noWaystonesInfo = new TextDisplay(
            new TranslatableComponent(LangKeys.noWaystones),
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
        addRenderableWidget(rotationInputField);
        angleDropDown = new DropDownSelection<>(
            font,
            new Point(getCenterX() - centerGap, rotationInputBoxRect.center().y),
            Rect.XAlignment.Right,
            Rect.YAlignment.Center,
            (int)(waystoneNameTexture.size.width * waystoneBoxScale) + DropDownSelection.size.width,
            75,
            (int)((waystoneNameTexture.size.height * waystoneBoxScale) - DropDownSelection.size.height) / 2,
            e -> {
                addWidget(e);
                for(AbstractWidget b : overlaySelectionButtons)
                    removeWidget(b);
            },
            o -> {
                removeWidget(o);
                for(AbstractWidget b : overlaySelectionButtons)
                    addRenderableWidget(b);
            },
            entry -> {
                rotationInputField.setValue(entry.angleToString());
                angleDropDown.hideList();
            },
            false
        );
        angleDropDown.setEntries(new HashSet<>());
        angleDropDown.addEntry(angleEntryForPlayer());
        addRenderableWidget(angleDropDown);
        rotationLabel = new TextDisplay(
            new TranslatableComponent(LangKeys.rotationLabel),
            rotationInputBoxRect.at(Rect.XAlignment.Left, Rect.YAlignment.Center).add(-10, 0),
            Rect.XAlignment.Right, Rect.YAlignment.Center,
            font
        );
        addRenderableOnly(rotationLabel);

        Rect modelRect = new Rect(
            new Point(getCenterX() + centerGap + 3 * inputSignsScale, getCenterY() - centralAreaHeight / 2),
            new TextureSize(22, 16).scale(inputSignsScale),
            Rect.XAlignment.Left,
            Rect.YAlignment.Top);
        GuiModelRenderer postRenderer = new GuiModelRenderer(
            modelRect, postModel,
            0, -0.5f,
            RenderType.solid(),
            new int[]{colorFrom(postTexture.tint())});
        addRenderableOnly(postRenderer);
        Point modelRectTop = modelRect.at(Rect.XAlignment.Center, Rect.YAlignment.Top);

        final int inputBoxesZOffset = 100;
        Rect wideInputRect = new Rect(
            modelRectTop.add(-7 * inputSignsScale, 2 * inputSignsScale),
            modelRectTop.add(11 * inputSignsScale, 6 * inputSignsScale)
        );
        wideSignInputBox = new InputBox(font, wideInputRect, false, inputBoxesZOffset);
        wideSignInputBox.setBordered(false);
        wideSignInputBox.setTextColor(Colors.black);
        widgetsToFlip.add(new FlippableAtPivot(wideSignInputBox, modelRectTop.x));

        wideSignRenderer = new GuiModelRenderer(
            modelRect, wideModel,
            0, 0.24f,
            RenderType.solid(),
            new int[]{colorFrom(mainTexture.tint()), colorFrom(secondaryTexture.tint())});
        widgetsToFlip.add(wideSignRenderer);

        Rect shortInputRect = new Rect(
            modelRectTop.add(3 * inputSignsScale, 2 * inputSignsScale),
            modelRectTop.add(14 * inputSignsScale, 6 * inputSignsScale)
        );
        shortSignInputBox = new InputBox(font, shortInputRect, false, inputBoxesZOffset);
        shortSignInputBox.setBordered(false);
        shortSignInputBox.setTextColor(Colors.black);
        widgetsToFlip.add(new FlippableAtPivot(shortSignInputBox, modelRectTop.x));

        shortSignRenderer = new GuiModelRenderer(
            modelRect, shortModel,
            0, 0.24f,
            RenderType.solid(),
            new int[]{colorFrom(mainTexture.tint()), colorFrom(secondaryTexture.tint())});
        widgetsToFlip.add(shortSignRenderer);

        Rect largeInputRect = new Rect(
            modelRectTop.add(-7 * inputSignsScale, 3 * inputSignsScale),
            modelRectTop.add(9 * inputSignsScale, 14 * inputSignsScale))
            .withHeight(height -> height / 4 - 1);
        InputBox firstLarge = new InputBox(font, largeInputRect, false, inputBoxesZOffset);
        firstLarge.setBordered(false);
        firstLarge.setTextColor(Colors.black);
        largeInputRect = largeInputRect.withPoint(p -> p.withY(Math.round(modelRectTop.y + (13 - 3 * 2.5f) * inputSignsScale)));
        InputBox secondLarge = new InputBox(font, largeInputRect, false, inputBoxesZOffset);
        secondLarge.setBordered(false);
        secondLarge.setTextColor(Colors.black);
        largeInputRect = largeInputRect.withPoint(p -> p.withY(Math.round(modelRectTop.y + (13 - 2 * 2.5f) * inputSignsScale)));
        InputBox thirdLarge = new InputBox(font, largeInputRect, false, inputBoxesZOffset);
        thirdLarge.setBordered(false);
        thirdLarge.setTextColor(Colors.black);
        largeInputRect = largeInputRect.withPoint(p -> p.withY(Math.round(modelRectTop.y + (13 - 1 * 2.5f) * inputSignsScale)));
        InputBox fourthLarge = new InputBox(font, largeInputRect, false, inputBoxesZOffset);
        fourthLarge.setBordered(false);
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
            0, -0.01f,
            RenderType.solid(),
            new int[]{colorFrom(mainTexture.tint()), colorFrom(secondaryTexture.tint())});
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
        addRenderableWidget(switchDirectionButton);

        colorInputBox = new ColorInputBox(font,
            new Rect(
                new Point(switchDirectionButton.x + switchDirectionButton.getWidth() + 20, switchDirectionButton.y + switchDirectionButton.getHeight() / 2),
                80, 20,
                Rect.XAlignment.Left, Rect.YAlignment.Center
            ), 0);
        colorInputBox.setColorResponder(color -> allSignInputBoxes.forEach(b -> b.setTextColor(color)));
        addRenderableWidget(colorInputBox);

        overlaySelectionButtons.clear();
        int i = 0;
        for(Overlay overlay: Overlay.getAllOverlays()) {
            FlippableModel overlayModel = FlippableModel.loadFrom(
                PostModel.wideOverlayLocation, PostModel.wideOverlayFlippedLocation, overlay.textureFor(SmallWideSignBlockPart.class)
            );
            overlaySelectionButtons.add(new ModelButton(
                TextureResource.signTypeSelection, new Point(getCenterX() - centerGap - i * 37, rotationInputBoxRect.max().y + 15),
                overlayButtonsScale, Rect.XAlignment.Right, Rect.YAlignment.Top,
                rect -> rect.withPoint(p -> p.add(Math.round(-4 / typeSelectionButtonsScale * overlayButtonsScale), 0)).scaleCenter(0.75f),
                () -> switchOverlay(Optional.of(overlay)),
                new ModelButton.ModelData(postModel, 0, -0.5f, itemStack, RenderType.solid(), new int[]{colorFrom(postTexture.tint())}),
                new ModelButton.ModelData(wideModel, 0, 0.25f, itemStack, RenderType.solid(), new int[]{colorFrom(mainTexture.tint()), colorFrom(secondaryTexture.tint())}),
                new ModelButton.ModelData(overlayModel, 0, 0.25f, itemStack, RenderType.cutout(), new int[]{colorFrom(overlay.tint)})
            ));
            i++;
        }
        if(i > 0)
            overlaySelectionButtons.add(new ModelButton(
                TextureResource.signTypeSelection, new Point(getCenterX() - centerGap - i * 37, rotationInputBoxRect.max().y + 15),
                overlayButtonsScale, Rect.XAlignment.Right, Rect.YAlignment.Top,
                rect -> rect.withPoint(p -> p.add(Math.round(-4 / typeSelectionButtonsScale * overlayButtonsScale), 0)).scaleCenter(0.75f),
                () -> switchOverlay(Optional.empty()),
                new ModelButton.ModelData(postModel, 0, -0.5f, itemStack, RenderType.solid(), new int[]{colorFrom(postTexture.tint())}),
                new ModelButton.ModelData(wideModel, 0, 0.25f, itemStack, RenderType.solid(), new int[]{colorFrom(mainTexture.tint()), colorFrom(secondaryTexture.tint())})
            ));
        for(Button button : overlaySelectionButtons) addRenderableWidget(button);


        switchTo(currentType);
        switchOverlay(selectedOverlay);
        waystoneInputBox.setValue(currentWaystone);
        switch (currentType) {
            case Wide:
                wideSignInputBox.setValue(currentText[0].get());
                break;
            case Short:
                shortSignInputBox.setValue(currentText[0].get());
                break;
            case Large:
                for(i = 0; i < largeSignInputBoxes.size(); i++) {
                    largeSignInputBoxes.get(i).setValue(currentText[i].get());
                }
                break;
        }
        if(isFlipped ^ widgetsToFlip.get(0).isFlipped()) flip();
        colorInputBox.setSelectedColor(currentColor);
        rotationInputField.setSelectedAngle(Angle.fromDegrees(Math.round(currentAngle.degrees())));


        if(hasBeenInitialized) {
            onWaystoneCountChanged();
        } else {
            String unknownWaystone = new TranslatableComponent(LangKeys.unknownWaystone)
                .withStyle(style -> style.withColor(TextColor.fromRgb(Colors.darkGrey)))
                .getString();
            Optional<WaystoneEntry> oldWaystone = oldSign
                .<WaystoneHandle>flatMap(SignBlockPart::getDestination)
                .map(handle -> new WaystoneEntry(
                    unknownWaystone,
                    unknownWaystone,
                    (WaystoneHandle) handle,
                    tile.getBlockPos().offset(
                        new Vector3(100, 0, 0).rotateY(oldSign.get().getAngle().get()).toBlockPos()
                    )
                ));
            oldWaystone.ifPresent(text -> {
                waystoneDropdown.addEntry(text);
                waystoneInputBox.setResponder(x -> {});
                waystoneInputBox.setValue(text.entryName);
                waystoneInputBox.setResponder(this::onWaystoneSelected);
            });
            Consumer<Function<WaystoneHandle, Optional<Tuple<Tuple<String, String>, BlockPos>>>> setupFromSign = map -> {
                oldWaystone.ifPresent(oldWs -> {
                    Optional<Tuple<Tuple<String, String>, BlockPos>> name = map.apply(oldWs.handle);
                    if (name.isPresent()) {
                        oldWs.entryName = name.get()._1._1;
                        oldWs.displayName = name.get()._1._2;
                        oldWs.pos = name.get()._2;
                        waystoneInputBox.setValue(oldWs.entryName);
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
                List<WaystoneEntry> entries = n.stream().map(w -> new WaystoneEntry(
                    w.name() + " " + w.handle().modMark(),
                    w.name(),
                    w.handle(),
                    w.loc().block.blockPos
                )).collect(Collectors.toList());
                waystoneDropdown.addEntries(entries.stream().filter(e -> oldWaystone.map(oldE -> !e.handle.equals(oldE.handle)).orElse(true))
                    .collect(Collectors.toList()));
                setupFromSign.accept(id -> entries.stream().filter(e -> e.handle.equals(id)).findFirst().map(e -> Tuple.of(e.entryName, e.displayName, e.pos)));
            });
            WaystoneLibrary.getInstance().updateEventDispatcher.addListener(waystoneUpdateListener);
        }

        final int newSignItemSize = 16;
        TextDisplay newSignHint = new TextDisplay(
            new TranslatableComponent(LangKeys.newSignHint),
            new Point(getCenterX() - newSignItemSize, (int) ((doneButton.y + doneButton.getHeight() + height) / 2f)),
            Rect.XAlignment.Center, Rect.YAlignment.Center,
            font
        );
        addRenderableOnly(newSignHint);
        GuiItemRenderer ir = new GuiItemRenderer(
            new Rect(newSignHint.rect.at(Rect.XAlignment.Right, Rect.YAlignment.Center), newSignItemSize, newSignItemSize, Rect.XAlignment.Left, Rect.YAlignment.Center),
            itemToDropOnBreak
        );
        addRenderableOnly(ir);
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
        setFocused(currentSignInputBox);
        currentSignInputBox.setFocus(true);
    }

    private void onWaystoneCountChanged() {
        if(waystoneDropdown.getAllEntries().isEmpty()){
            if(!renderables.contains(noWaystonesInfo))
                addRenderableOnly(noWaystonesInfo);
            removeWidget(waystoneDropdown);
            removeWidget(waystoneInputBox);
        } else {
            if(!renderables.contains(waystoneDropdown))
                addRenderableWidget(waystoneDropdown);
            if(!renderables.contains(waystoneInputBox))
                addRenderableWidget(waystoneInputBox);
            renderables.remove(noWaystonesInfo);
        }
    }

    private void flip() {
        AngleSelectionEntry playerAngleEntry = angleEntryForPlayer();
        boolean shouldPointAtPlayer = Math.round(Math.abs(playerAngleEntry.angleGetter.get().degrees()
            - rotationInputField.getCurrentAngle().degrees())) <= 1;
        widgetsToFlip.forEach(Flippable::flip);
        if(shouldPointAtPlayer)
            rotationInputField.setValue(playerAngleEntry.angleToString());
    }

    private void onWaystoneSelected(String waystoneName) {
        boolean shouldOverrideRotation = isCurrentAnglePointingAtWaystone() || isCurrentAnglePointingAtPlayer();
        if(waystoneRotationEntry != null) {
            shouldOverrideRotation |= waystoneRotationEntry.angleGetter.get().isNearly(rotationInputField.getCurrentAngle(), Angle.fromDegrees(1));
            angleDropDown.removeEntry(waystoneRotationEntry);
        }
        Optional<WaystoneEntry> validWaystone = asValidWaystone(waystoneName);
        if(waystoneName.equals("") || validWaystone.isPresent()) {
            waystoneInputBox.setTextColor(Colors.valid);
            waystoneInputBox.setTextColorUneditable(Colors.validInactive);
            waystoneDropdown.setFilter(name -> true);
            if(currentSignInputBox != null
                && lastWaystone.map(lw ->
                    lw.displayName.equals(currentSignInputBox.getValue()))
                        .orElse(currentSignInputBox.getValue().equals("")))
                currentSignInputBox.setValue(validWaystone.map(e -> e.displayName).orElse(waystoneName));
            if(!waystoneName.equals("")) {
                waystoneRotationEntry = angleEntryForWaystone(validWaystone.get());
                angleDropDown.addEntry(waystoneRotationEntry);
                if(shouldOverrideRotation)
                    rotationInputField.setSelectedAngle(waystoneRotationEntry.angleGetter.get());
            }
            lastWaystone = validWaystone;
        } else {
            waystoneInputBox.setTextColor(Colors.invalid);
            waystoneInputBox.setTextColorUneditable(Colors.invalidInactive);
            waystoneDropdown.setFilter(e -> e.entryName.toLowerCase().contains(waystoneName.toLowerCase()));
            if(currentSignInputBox != null
                && lastWaystone.map(lw ->
                    lw.displayName.equals(currentSignInputBox.getValue()))
                        .orElse(currentSignInputBox.getValue().equals("")))
                currentSignInputBox.setValue("");
        }
    }

    private int colorFrom(Optional<Tint> tint) {
        return tint.map(t -> t.getColorAt(minecraft.level, minecraft.player.blockPosition())).orElse(Colors.white);
    }

    private boolean isCurrentAnglePointingAtWaystone() {
        return (!oldSign.isPresent() && (rotationInputField.getCurrentAngle().equals(Angle.ZERO))
            || lastWaystone.map(lw -> rotationInputField.getCurrentAngle().isNearly(angleEntryForWaystone(lw).angleGetter.get(), Angle.fromDegrees(1)))
                .orElse(false));
    }

    private boolean isCurrentAnglePointingAtPlayer() {
        return rotationInputField.getCurrentAngle().isNearly(angleEntryForPlayer().angleGetter.get(), Angle.fromDegrees(1));
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

    private final List<AbstractWidget> selectionDependentWidgets = Lists.newArrayList();

    private void switchTo(SignType type) {
        switch (type) {
            case Wide -> switchToWide();
            case Short -> switchToShort();
            case Large -> switchToLarge();
            default -> throw new RuntimeException("Sign type " + type + " is not supported");
        }
    }

    private void switchToWide(){
        if(selectedType == SignType.Wide) return;
        clearTypeDependentChildren();
        selectedType = SignType.Wide;

        switchSignInputBoxTo(wideSignInputBox);

        addRenderableOnly(wideSignRenderer);
        addTypeDependentChild(wideSignInputBox);
        currentSignRenderer = wideSignRenderer;
        switchOverlay(selectedOverlay);
    }

    private void switchToShort(){
        if(selectedType == SignType.Short) return;
        clearTypeDependentChildren();
        selectedType = SignType.Short;

        switchSignInputBoxTo(shortSignInputBox);

        addRenderableOnly(shortSignRenderer);
        addTypeDependentChild(shortSignInputBox);
        currentSignRenderer = shortSignRenderer;
        switchOverlay(selectedOverlay);
    }

    private void switchToLarge(){
        if(selectedType == SignType.Large) return;
        clearTypeDependentChildren();
        selectedType = SignType.Large;

        switchSignInputBoxTo(largeSignInputBoxes.get(0));

        addRenderableOnly(largeSignRenderer);
        addTypeDependentChildren(largeSignInputBoxes);
        currentSignRenderer = largeSignRenderer;
        switchOverlay(selectedOverlay);
    }

    private GuiModelRenderer currentOverlay;

    private void switchOverlay(Optional<Overlay> overlay) {
        if(currentOverlay != null) {
            renderables.remove(currentOverlay);
            widgetsToFlip.remove(currentOverlay);
        }
        this.selectedOverlay = overlay;
        if(!overlay.isPresent()) return;
        Overlay o = overlay.get();
        switch(selectedType) {
            case Wide:
                currentOverlay = new GuiModelRenderer(
                    wideSignRenderer.rect,
                    FlippableModel.loadFrom(PostModel.wideOverlayLocation, PostModel.wideOverlayFlippedLocation, o.textureFor(SmallWideSignBlockPart.class)),
                    0, 0.25f,
                    RenderType.cutout(),
                    new int[]{colorFrom(o.tint)});
                break;
            case Short:
                currentOverlay = new GuiModelRenderer(
                    shortSignRenderer.rect,
                    FlippableModel.loadFrom(PostModel.shortOverlayLocation, PostModel.shortOverlayFlippedLocation, o.textureFor(SmallShortSignBlockPart.class)),
                    0, 0.25f,
                    RenderType.cutout(),
                    new int[]{colorFrom(o.tint)});
                break;
            case Large:
                currentOverlay = new GuiModelRenderer(
                    largeSignRenderer.rect,
                    FlippableModel.loadFrom(PostModel.largeOverlayLocation, PostModel.largeOverlayFlippedLocation, o.textureFor(LargeSignBlockPart.class)),
                    0, 0,
                    RenderType.cutout(),
                    new int[]{colorFrom(o.tint)});
                break;
        }
        addRenderableOnly(currentOverlay);
        if(currentSignRenderer.isFlipped()) currentOverlay.flip();
        widgetsToFlip.add(currentOverlay);
    }

    private void hideStuffOccludedByWaystoneDropdown() {
        renderables.remove(rotationLabel);
        removeWidget(rotationInputField);
        angleDropDown.hideList();
        removeWidget(angleDropDown);
        for(Button b : overlaySelectionButtons) removeWidget(b);
    }

    private void showStuffOccludedByWaystoneDropdown() {
        addRenderableOnly(rotationLabel);
        addRenderableWidget(rotationInputField);
        addRenderableWidget(angleDropDown);
        for(Button b : overlaySelectionButtons) addRenderableWidget(b);
    }

    private void switchSignInputBoxTo(InputBox box) {
        if(currentSignInputBox != null)
            box.setValue(currentSignInputBox.getValue());
        currentSignInputBox = box;
    }

    private void clearTypeDependentChildren(){
        for(AbstractWidget b : selectionDependentWidgets) removeWidget(b);
        renderables.remove(currentSignRenderer);
        selectionDependentWidgets.clear();
    }

    private void addTypeDependentChildren(Collection<? extends AbstractWidget> widgets){
        selectionDependentWidgets.addAll(widgets);
        for(AbstractWidget w : widgets) addRenderableWidget(w);
    }

    private void addTypeDependentChild(AbstractWidget widget){
        selectionDependentWidgets.add(widget);
        addRenderableWidget(widget);
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
        apply(asValidWaystone(waystoneInputBox.getValue()).map(w -> w.handle));
        getMinecraft().setScreen(null);
    }

    private void apply(Optional<WaystoneHandle> destinationId) {
        PostTile.TilePartInfo tilePartInfo = oldTilePartInfo.orElseGet(() ->
            new PostTile.TilePartInfo(tile.getLevel().dimension().location(), tile.getBlockPos(), UUID.randomUUID()));
        CompoundTag data;
        boolean isLocked = lockButton.isLocked();
        var mainTex = oldSign.map(SignBlockPart::getMainTexture).orElse(modelType.mainTexture);
        var secondaryTex = oldSign.map(SignBlockPart::getSecondaryTexture).orElse(modelType.secondaryTexture);
        AngleProvider angle = destinationId.flatMap(destination -> isCurrentAnglePointingAtWaystone()
            ? Optional.<AngleProvider>of(new AngleProvider.WaystoneTarget(rotationInputField.getCurrentAngle()))
            : Optional.empty()
        ).orElseGet(() -> new AngleProvider.Literal(rotationInputField.getCurrentAngle()));
        switch (selectedType) {
            case Wide -> {
                data = SmallWideSignBlockPart.METADATA.write(
                    new SmallWideSignBlockPart(
                        angle,
                        asNameProvider(wideSignInputBox.getValue()),
                        wideSignRenderer.isFlipped(),
                        mainTex,
                        secondaryTex,
                        selectedOverlay,
                        colorInputBox.getCurrentColor(),
                        destinationId,
                        itemToDropOnBreak,
                        modelType,
                        isLocked,
                        oldSign.map(SignBlockPart::isMarkedForGeneration).orElse(false)
                    )
                );
                if (oldSign.isPresent()) {
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
            }
            case Short -> {
                data = SmallShortSignBlockPart.METADATA.write(
                    new SmallShortSignBlockPart(
                        angle,
                        asNameProvider(shortSignInputBox.getValue()),
                        shortSignRenderer.isFlipped(),
                        mainTex,
                        secondaryTex,
                        selectedOverlay,
                        colorInputBox.getCurrentColor(),
                        destinationId,
                        itemToDropOnBreak,
                        modelType,
                        isLocked,
                        oldSign.map(SignBlockPart::isMarkedForGeneration).orElse(false)
                    )
                );
                if (oldSign.isPresent()) {
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
            }
            case Large -> {
                data = LargeSignBlockPart.METADATA.write(
                    new LargeSignBlockPart(
                        angle,
                        new NameProvider[]{
                            asNameProvider(largeSignInputBoxes.get(0).getValue()),
                            asNameProvider(largeSignInputBoxes.get(1).getValue()),
                            asNameProvider(largeSignInputBoxes.get(2).getValue()),
                            asNameProvider(largeSignInputBoxes.get(3).getValue()),
                        },
                        currentSignRenderer.isFlipped(),
                        mainTex,
                        secondaryTex,
                        selectedOverlay,
                        colorInputBox.getCurrentColor(),
                        destinationId,
                        itemToDropOnBreak,
                        modelType,
                        isLocked,
                        oldSign.map(SignBlockPart::isMarkedForGeneration).orElse(false)
                    )
                );
                if (oldSign.isPresent()) {
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
            }
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
            angleWhenFlipped.set(Angle.fromDegrees(-getMinecraft().player.getYRot()).normalized());
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

    private NameProvider asNameProvider(String name) {
        return lastWaystone.map(lw -> lw.entryName.equals(name)).orElse(false)
            ? new NameProvider.WaystoneTarget(name)
            : new NameProvider.Literal(name);
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
