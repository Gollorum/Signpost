package gollorum.signpost.minecraft.gui.widget;

import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.utils.math.Angle;
import net.minecraft.client.gui.FontRenderer;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class AngleInputBox extends InputBox {

    public static final String degreeSign = Character.toString('\u00b0');

    @Nullable
    private Consumer<Angle> responder;

    private float currentResult;

    public AngleInputBox(FontRenderer fontRenderer, Rect inputFieldRect, double zOffset) {
        super(fontRenderer, new Rect(
            new Point(inputFieldRect.point.x + inputFieldRect.height, inputFieldRect.point.y),
            inputFieldRect.width - inputFieldRect.height, inputFieldRect.height
        ), true, true, zOffset);
        setValidator(text -> text.endsWith(degreeSign) ? canParse(text.substring(0, text.length() - 1)) : canParse(text));
        setText("0" + degreeSign);
    }

    private static boolean canParse(String text) {
        if(text.equals("")) return true;
        try {
            Integer.parseInt(text);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    private float getResult() {
        String text = getText().endsWith(degreeSign)
            ? getText().substring(0, getText().length() - 1)
            : getText();
        return text.equals("") ? 0 : Integer.parseInt(text);
    }

    public Angle getCurrentAngle() { return Angle.fromDegrees(currentResult); }

    @Override
    protected void onTextChanged() {
        currentResult = getResult();
        if(responder != null) {
            responder.accept(Angle.fromDegrees(currentResult));
        }
        super.onTextChanged();
    }

    public void setAngleResponder(@Nullable Consumer<Angle> responder) {
        this.responder = responder;
    }

    public void setSelectedAngle(Angle angle) {
        setText(Math.round(angle.degrees()) + degreeSign);
    }
}
