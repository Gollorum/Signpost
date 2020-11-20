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
    private WaystoneInputBox inputBox;

    private Optional<Set<String>> allWaystoneNames = Optional.empty();

    private static final int inputBoxWidth = 137;
    private static final int inputBoxYOffset = -46;

    private static final int buttonsYOffset = -inputBoxYOffset;
    private static final int buttonsXOffset = 15;
    private static final int buttonsWidth = 98;
    private static final int buttonsHeight = 20;

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
        WaystoneLibrary.getInstance().addListener(waystoneUpdateListener);
        inputBox = new WaystoneInputBox(
            getCenterX() - (inputBoxWidth) / 2,
            getCenterY() - (int)(inputBoxWidth / WaystoneInputBox.widthHeightRatio) / 2 + inputBoxYOffset,
            inputBoxWidth
        );
        oldData.ifPresent(data -> inputBox.setText(data.name));
        doneButton = new Button(
            getCenterX() - buttonsWidth - buttonsXOffset,
            getCenterY() - buttonsHeight / 2 + buttonsYOffset,
            buttonsWidth,
            buttonsHeight,
            I18n.format(LangKeys.Done),
            this::done
        );
        addButton(doneButton);
        addButton(new Button(
            getCenterX() + buttonsXOffset,
            getCenterY() - buttonsHeight / 2 + buttonsYOffset,
            buttonsWidth,
            buttonsHeight,
            I18n.format(LangKeys.Cancel),
            this::cancel
        ));
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
        children.add(inputBox);
        setFocusedDefault(inputBox);
    }

    private boolean isValid(String name) {
        return allWaystoneNames.map(names -> !names.contains(name)).orElse(true);
    }

    @Override
    public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
        renderBackground();
        super.render(p_render_1_, p_render_2_, p_render_3_);
        inputBox.render(p_render_1_, p_render_2_, p_render_3_);
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        return inputBox.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return inputBox.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
        WaystoneLibrary.getInstance().removeListener(waystoneUpdateListener);
    }

    private void cancel(Button button) {
        getMinecraft().displayGuiScreen(null);
    }

    private void done(Button button) {
        WaystoneLibrary.getInstance().update(inputBox.getText(), new WaystoneLocationData(location, Vector3.fromVec3d(getMinecraft().player.getPositionVec())));
        getMinecraft().displayGuiScreen(null);
    }
}
