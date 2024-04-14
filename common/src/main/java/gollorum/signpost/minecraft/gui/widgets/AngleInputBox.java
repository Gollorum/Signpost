package gollorum.signpost.minecraft.gui.widgets;

import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.utils.math.Angle;
import net.minecraft.client.gui.Font;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AngleInputBox extends InputBox {

    public static final String degreeSign = Character.toString('\u00b0');

    private float currentResult;

    public AngleInputBox(Font fontRenderer, Rect inputFieldRect, double zOffset) {
        super(
            fontRenderer,
            new Rect(
                new Point(inputFieldRect.point.x + inputFieldRect.height, inputFieldRect.point.y),
                inputFieldRect.width - inputFieldRect.height, inputFieldRect.height
            ),
            true, zOffset
        );
        setFilter(null);
        setValue("0" + degreeSign);
        setResponder(null);
    }

    private static boolean isValidValue(String text) {
        return text.endsWith(degreeSign) ? canParse(text.substring(0, text.length() - 1)) : canParse(text);
    }

    private static boolean canParse(String text) {
        if(text.equals("")) return true;
        else if(text.equals("-")) return true;
        try {
            Integer.parseInt(text);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    private float getResult() {
        String text = getValue().endsWith(degreeSign)
            ? getValue().substring(0, getValue().length() - 1)
            : getValue();
        return text.equals("") || text.equals("-") ? 0 : Integer.parseInt(text);
    }

    public Angle getCurrentAngle() { return Angle.fromDegrees(currentResult); }

    @Override
    public void setResponder(@Nullable Consumer<String> responder) {
        super.setResponder(value -> {
            currentResult = getResult();
            if(responder != null) {
                responder.accept(value);
            }
        });
    }

    @Override
    public void setFilter(@Nullable Predicate<String> filter) {
        super.setFilter(value -> isValidValue(value) && (filter == null || filter.test(value)));
    }

    public void setAngleResponder(@Nullable Consumer<Angle> responder) {
        setResponder(responder == null ? null : value -> responder.accept(Angle.fromDegrees(currentResult)));
    }

    public void setSelectedAngle(Angle angle) {
        setValue(Math.round(angle.degrees()) + degreeSign);
    }
}
