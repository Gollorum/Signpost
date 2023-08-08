package gollorum.signpost.minecraft.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.events.WaystoneRenamedEvent;
import gollorum.signpost.events.WaystoneUpdatedEvent;
import gollorum.signpost.minecraft.gui.utils.*;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.widgets.ImageInputBox;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class WaystoneGui extends ExtendedScreen {

    private final WorldLocation location;
    private final Optional<WaystoneData> oldData;
    private ImageInputBox inputBox;

    private Optional<Set<String>> allWaystoneNames = Optional.empty();

    private static final TextureResource texture = TextureResource.waystoneNameField;
    private static final int inputBoxScale = 3;
    private static final int inputBoxYOffset = -46;

    private static final int buttonsYOffset = -inputBoxYOffset;
    private static final TextureSize buttonsSize = new TextureSize(98, 20);

    private LockIconButton lockButton;

    private Button doneButton;

    private final Consumer<WaystoneUpdatedEvent> waystoneUpdateListener = event ->
        allWaystoneNames.ifPresent(names -> {
            switch (event.getType()) {
                case Added -> names.add(event.name);
                case Removed -> names.remove(event.name);
                case Renamed -> {
                    names.remove(((WaystoneRenamedEvent) event).oldName);
                    names.add(event.name);
                }
            }
        });

    public WaystoneGui(WorldLocation location, Optional<WaystoneData> oldData) {
        super(Component.literal("Waystone"));
        this.location = location;
        this.oldData = oldData;
    }

	public static void display(WorldLocation location, Optional<WaystoneData> oldData) {
        Minecraft.getInstance().setScreen(new WaystoneGui(location, oldData));
	}

	private int getCenterX() { return this.width / 2; }
    private int getCenterY() { return this.height / 2; }

    @Override
    protected void init() {
        super.init();
        WaystoneLibrary.getInstance()
            .requestAllWaystoneNames(names -> allWaystoneNames = Optional.of(
                new HashSet<>(names.values())), Optional.empty(), true);
        WaystoneLibrary.getInstance().updateEventDispatcher.addListener(waystoneUpdateListener);
        inputBox = new ImageInputBox(font,
            new Rect(
                new Point(getCenterX(), getCenterY() + inputBoxYOffset),
                new TextureSize((texture.size.width - 6) * inputBoxScale, font.lineHeight),
                Rect.XAlignment.Center, Rect.YAlignment.Bottom),
            new Rect(
                Point.zero,
                texture.size.scale(inputBoxScale),
                Rect.XAlignment.Center, Rect.YAlignment.Center),
            Rect.XAlignment.Center, Rect.YAlignment.Center,
            texture,
            true, 0
        );
        lockButton = new LockIconButton(
            inputBox.getX() + inputBox.width() + 10,
            inputBox.getY() + inputBox.getHeight() / 2 - 10,
            b -> lockButton.setLocked(!lockButton.isLocked())
        );
        addRenderableWidget(lockButton);
        oldData.ifPresent(data -> {
            inputBox.setValue(data.name);
            lockButton.setLocked(data.isLocked);
        });
        doneButton = Button.builder(
            Component.translatable(LangKeys.done),
            b -> done()
        ).bounds(
            getCenterX() - buttonsSize.width / 2,
            getCenterY() - buttonsSize.height / 2 + buttonsYOffset,
            buttonsSize.width,
            buttonsSize.height
        ).build();
        addRenderableWidget(inputBox);
        addRenderableWidget(doneButton);
        inputBox.setTextColor(Colors.valid);
        inputBox.setTextColorUneditable(Colors.validInactive);
        inputBox.setMaxLength(200);
        inputBox.setResponder(name -> {
            if(isValid(name)){
                inputBox.setTextColor(Colors.valid);
                inputBox.setTextColorUneditable(Colors.validInactive);
                doneButton.active = true;
            } else {
                inputBox.setTextColor(Colors.invalid);
                inputBox.setTextColorUneditable(Colors.invalidInactive);
                doneButton.active = false;
            }
        });
        setInitialFocus(inputBox);
    }

    private boolean isValid(String name) {
        return allWaystoneNames.map(names -> !names.contains(name)).orElse(true)
            || (oldData.isPresent() && oldData.get().name.equals(name));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        super.onClose();
        WaystoneLibrary.getInstance().updateEventDispatcher.removeListener(waystoneUpdateListener);
    }

    private void done() {
        if(inputBox != null && !inputBox.getValue().equals("") && isValid(inputBox.getValue()))
            WaystoneLibrary.getInstance().requestUpdate(
                inputBox.getValue(),
                new WaystoneLocationData(location, Vector3.fromVec3d(getMinecraft().player.position())),
                lockButton.isLocked()
            );
        onClose();
    }
}
