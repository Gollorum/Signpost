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
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.signtypes.LargeSign;
import gollorum.signpost.signtypes.Sign;
import gollorum.signpost.signtypes.SmallShortSign;
import gollorum.signpost.signtypes.SmallWideSign;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.renderer.model.IBakedModel;
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

    private final List<Flippable> widgetsToFlip = new ArrayList<>();
    private final Set<IRenderable> additionallyRenderables = new HashSet<>();

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

        IBakedModel postModel = RenderingUtil.loadModel(RenderingUtil.ModelPost, tile.getParts().stream().filter(p -> p.blockPart instanceof gollorum.signpost.signtypes.Post).map(p -> ((gollorum.signpost.signtypes.Post)p.blockPart).getTexture()).findFirst().orElse(tile.modelType.postTexture)).get();
        IBakedModel wideModel = RenderingUtil.loadModel(RenderingUtil.ModelWideSign, modelType.mainTexture, modelType.secondaryTexture).get();
        IBakedModel shortModel = RenderingUtil.loadModel(RenderingUtil.ModelShortSign, modelType.mainTexture, modelType.secondaryTexture).get();
        IBakedModel largeModel = RenderingUtil.loadModel(RenderingUtil.ModelLargeSign, modelType.mainTexture, modelType.secondaryTexture).get();

        ItemStack itemStack = new ItemStack(tile.getBlockState().getBlock().asItem());

        ImageButton wideSelectionButton = newImageButton(
            TextureResource.signTypeSelection,
            0,
            new Point(getCenterX() - centerOffset, signTypeSelectionTopY),
            typeSelectionButtonsScale,
            Rect.XAlignment.Center, Rect.YAlignment.Top,
            this::switchToWide
        );
        addButton(wideSelectionButton);
        Rect wideSelectionRect = new Rect(new Point(wideSelectionButton.x - 4, wideSelectionButton.y), wideSelectionButton.getWidth(), wideSelectionButton.getHeightRealms())
            .scaleCenter(0.75f);
        additionallyRenderables.add(new GuiModelRenderer(
            wideSelectionRect,
            postModel,
            0, -0.5f,
            itemStack
        ));
        additionallyRenderables.add(new GuiModelRenderer(
            wideSelectionRect,
            wideModel,
            0, 0.25f,
            itemStack
        ));

        ImageButton shortSelectionButton = newImageButton(
            TextureResource.signTypeSelection,
            1,
            new Point(getCenterX(), signTypeSelectionTopY),
            typeSelectionButtonsScale,
            Rect.XAlignment.Center, Rect.YAlignment.Top,
            this::switchToShort
        );
        addButton(shortSelectionButton);
        Rect shortSelectionRect = new Rect(new Point(shortSelectionButton.x - 11, shortSelectionButton.y), shortSelectionButton.getWidth(), shortSelectionButton.getHeightRealms())
            .scaleCenter(0.75f);
        additionallyRenderables.add(new GuiModelRenderer(
            shortSelectionRect,
            postModel,
            0, -0.5f,
            itemStack
        ));
        additionallyRenderables.add(new GuiModelRenderer(
            shortSelectionRect,
            shortModel,
            0, 0.25f,
            itemStack
        ));

        ImageButton largeSelectionButton = newImageButton(
            TextureResource.signTypeSelection,
            2,
            new Point(getCenterX() + centerOffset, signTypeSelectionTopY),
            typeSelectionButtonsScale,
            Rect.XAlignment.Center, Rect.YAlignment.Top,
            this::switchToLarge
        );
        addButton(largeSelectionButton);
        Rect largeSelectionRect = new Rect(new Point(largeSelectionButton.x - 3, largeSelectionButton.y), largeSelectionButton.getWidth(), largeSelectionButton.getHeightRealms())
            .scaleCenter(0.75f);
        additionallyRenderables.add(new GuiModelRenderer(
            largeSelectionRect,
            postModel,
            0, -0.5f,
            itemStack
        ));
        additionallyRenderables.add(new GuiModelRenderer(
            largeSelectionRect,
            largeModel,
            0, 0,
            itemStack
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
            true, 100);
        waystoneInputBox.setMaxStringLength(200);
        waystoneInputBox.setResponder(this::onWaystoneSelected);
        addButton(waystoneInputBox);

        Rect modelRect = new Rect(
            new Point(getCenterX() + centerGap + 3 * inputSignsScale, getCenterY() - centralAreaHeight / 2),
            new TextureSize(22, 16).scale(inputSignsScale),
            Rect.XAlignment.Left,
            Rect.YAlignment.Top);
        GuiModelRenderer postRenderer = new GuiModelRenderer(
            modelRect,
            postModel,
            0, -0.5f,
            new ItemStack(Post.OAK.post.asItem()));
        additionallyRenderables.add(postRenderer);
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
            modelRect,
            wideModel,
            0, 0.25f,
            itemStack);
        widgetsToFlip.add(wideSignRenderer);

        Rect shortInputRect = new Rect(
            modelRectTop.add(3 * inputSignsScale, 2 * inputSignsScale),
            modelRectTop.add(14 * inputSignsScale, 6 * inputSignsScale));
        shortSignInputBox = new InputBox(font,
            shortInputRect,
            false, false, 100);
        shortSignInputBox.setTextColor(Colors.black);
        widgetsToFlip.add(new FlippableAtPivot(shortSignInputBox, modelRectTop.x));

        shortSignRenderer = new GuiModelRenderer(
            modelRect,
            shortModel,
            0, 0.25f,
            itemStack);
        widgetsToFlip.add(shortSignRenderer);

        Rect largeInputRect = new Rect(
            modelRectTop.add(-7 * inputSignsScale, 3 * inputSignsScale),
            modelRectTop.add(11 * inputSignsScale, 14 * inputSignsScale))
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
        widgetsToFlip.add(new FlippableAtPivot(firstLarge, modelRectTop.x));
        widgetsToFlip.add(new FlippableAtPivot(secondLarge, modelRectTop.x));
        widgetsToFlip.add(new FlippableAtPivot(thirdLarge, modelRectTop.x));
        widgetsToFlip.add(new FlippableAtPivot(fourthLarge, modelRectTop.x));

        largeSignRenderer = new GuiModelRenderer(
            modelRect,
            largeModel,
            0, 0,
            itemStack);
        widgetsToFlip.add(largeSignRenderer);



        largeSignInputBoxes = ImmutableList.of(firstLarge, secondLarge, thirdLarge, fourthLarge);
        allSignInputBoxes = ImmutableList.of(wideSignInputBox, shortSignInputBox, firstLarge, secondLarge, thirdLarge, fourthLarge);

        colorInputBox = new ColorInputBox(font,
            new Rect(new Point(modelRect.point.x + modelRect.width / 2, modelRect.point.y + modelRect.height + centerGap), 80, 20, Rect.XAlignment.Center, Rect.YAlignment.Top), 0);
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
            if(oldSign.get().isFlipped())
                flip();
            colorInputBox.setSelectedColor(oldSign.get().getColor());
        } else {
            switchToWide();
            flip();
        }


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
        widgetsToFlip.forEach(Flippable::flip);
    }

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
        for(IRenderable toRender : additionallyRenderables) {
            toRender.render(matrixStack, mouseX, mouseY, partialTicks);
        }
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
        additionallyRenderables.add(wideSignRenderer);
        currentSignRenderer = wideSignRenderer;
    }

    private void switchToShort(){
        if(selectedType == SignType.Short) return;
        clearTypeDependentChildren();
        selectedType = SignType.Short;

        switchSignInputBoxTo(shortSignInputBox);

        addTypeDependentChild(shortSignInputBox);
        additionallyRenderables.add(shortSignRenderer);
        currentSignRenderer = shortSignRenderer;
    }

    private void switchToLarge(){
        if(selectedType == SignType.Large) return;
        clearTypeDependentChildren();
        selectedType = SignType.Large;

        switchSignInputBoxTo(largeSignInputBoxes.get(0));

        addTypeDependentChildren(largeSignInputBoxes);
        additionallyRenderables.add(largeSignRenderer);
        currentSignRenderer = largeSignRenderer;
    }

    private void switchSignInputBoxTo(InputBox box) {
        if(currentSignInputBox != null)
            box.setText(currentSignInputBox.getText());
        currentSignInputBox = box;
    }

    private void clearTypeDependentChildren(){
        removeButtons(selectionDependentWidgets);
        additionallyRenderables.remove(currentSignRenderer);
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
                        wideSignRenderer.isFlipped(),
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
                        tilePartInfo, data,
                        SmallWideSign.METADATA.identifier,
                        new Vector3(0, localHitPos.y > 0.5f ? 0.75f : 0.25f, 0))
                    );
                }
                break;
            case Short:
                data = SmallShortSign.METADATA.write(
                    new SmallShortSign(
                        Angle.fromDegrees(0),
                        shortSignInputBox.getText(),
                        shortSignRenderer.isFlipped(),
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
                        tilePartInfo, data,
                        SmallShortSign.METADATA.identifier,
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
                        currentSignRenderer.isFlipped(),
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
                        tilePartInfo, data,
                        LargeSign.METADATA.identifier,
                        new Vector3(0, 0.5f, 0))
                    );
                }
                break;
        }
    }
}
