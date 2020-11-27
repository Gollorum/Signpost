package gollorum.signpost.minecraft.gui;

import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.events.WaystoneRenamedEvent;
import gollorum.signpost.minecraft.events.WaystoneUpdatedEvent;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.StringTextComponent;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class WaystoneGui extends Screen {

    private final WorldLocation location;
    private final Optional<WaystoneData> oldData;
    private ImageInputBox inputBox;

    private Optional<Set<String>> allWaystoneNames = Optional.empty();

    private static final TextureResource texture = TextureResource.waystoneNameField;
    private static final int inputBoxScale = 3;
    private static final int inputBoxYOffset = -46;

    private static final int buttonsYOffset = -inputBoxYOffset;
    private static final TextureSize buttonsSize = new TextureSize(98, 20);


    private Button doneButton;

    private final Consumer<WaystoneUpdatedEvent> waystoneUpdateListener = event ->
        allWaystoneNames.ifPresent(names -> {
            switch(event.getType()) {
                case Added:
                    names.add(event.name);
                    break;
                case Removed:
                    names.remove(event.name);
                    break;
                case Renamed:
                    names.remove(((WaystoneRenamedEvent)event).oldName);
                    names.add(event.name);
                    break;
            }
        });

    public WaystoneGui(WorldLocation location, Optional<WaystoneData> oldData) {
        super(new StringTextComponent("Waystone"));
        this.location = location;
        this.oldData = oldData;
    }

    private int getCenterX() { return this.width / 2; }
    private int getCenterY() { return this.height / 2; }

    @Override
    protected void init() {
        super.init();
        WaystoneLibrary.getInstance().requestAllWaystoneNames(names -> allWaystoneNames = Optional.of(new HashSet<>(names)));
        WaystoneLibrary.getInstance().updateEventDispatcher.addListener(waystoneUpdateListener);
        inputBox = new ImageInputBox(font,
            new Rect(
                new Point(getCenterX(), getCenterY() + inputBoxYOffset),
                new TextureSize((texture.size.width - 6) * inputBoxScale, font.FONT_HEIGHT),
                Rect.XAlignment.Center, Rect.YAlignment.Bottom),
            new Rect(
                Point.zero,
                texture.size.scale(inputBoxScale),
                Rect.XAlignment.Center, Rect.YAlignment.Center),
            Rect.XAlignment.Center, Rect.YAlignment.Center,
            texture,
            true);
        oldData.ifPresent(data -> inputBox.setText(data.name));
        doneButton = new Button(
            getCenterX() - buttonsSize.width / 2,
            getCenterY() - buttonsSize.height / 2 + buttonsYOffset,
            buttonsSize.width,
            buttonsSize.height,
            I18n.format(LangKeys.Done),
            b -> onClose()
        );
        addButton(doneButton);
        inputBox.setCanLoseFocus(false);
        inputBox.changeFocus(true);
        inputBox.setTextColor(Colors.valid);
        inputBox.setDisabledTextColour(Colors.validInactive);
        inputBox.setMaxStringLength(200);
        inputBox.setResponder(name -> {
            if(isValid(name)){
                inputBox.setTextColor(Colors.valid);
                inputBox.setDisabledTextColour(Colors.validInactive);
                doneButton.active = true;
            } else {
                inputBox.setTextColor(Colors.invalid);
                inputBox.setDisabledTextColour(Colors.invalidInactive);
                doneButton.active = false;
            }
        });
        addButton(inputBox);
        setFocusedDefault(inputBox);
    }

    private boolean isValid(String name) {
        return allWaystoneNames.map(names -> !names.contains(name)).orElse(true)
            || (oldData.isPresent() && oldData.get().name.equals(name));
    }

    @Override
    public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
        renderBackground();
        super.render(p_render_1_, p_render_2_, p_render_3_);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return inputBox.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
        if(!inputBox.getText().equals("") && isValid(inputBox.getText()))
            WaystoneLibrary.getInstance().update(inputBox.getText(), new WaystoneLocationData(location, Vector3.fromVec3d(getMinecraft().player.getPositionVec())));
        WaystoneLibrary.getInstance().updateEventDispatcher.removeListener(waystoneUpdateListener);
    }
}
