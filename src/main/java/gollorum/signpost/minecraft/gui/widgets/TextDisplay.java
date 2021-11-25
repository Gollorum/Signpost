package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.utils.Tuple;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.*;
import java.util.stream.Collectors;

public class TextDisplay implements IRenderable {

    private final List<Tuple<String, Integer>> texts;
    private final List<Integer> widths;
    public final Rect rect;
    private final FontRenderer fontRenderer;

    public TextDisplay(Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, FontRenderer fontRenderer, String translationKey, Tuple<String, Integer>... args){
        this(getTextsFor(translationKey, args), point, xAlignment, yAlignment, fontRenderer);
    }

    public TextDisplay(String text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, FontRenderer fontRenderer) {
        this(Collections.singletonList(new Tuple<>(text, Colors.white)), point, xAlignment, yAlignment, fontRenderer);
    }

    private static List<Tuple<String, Integer>> getTextsFor(String translationKey, Tuple<String, Integer>... args) {
        List<Tuple<String, Integer>> texts = new ArrayList<>();
        new TranslationTextComponent(
            translationKey,
            Arrays.stream(args).map(Tuple::getLeft).toArray(Object[]::new)
        ).flatStream().forEachOrdered(tt -> {
            String t = tt.getString();
            texts.add(new Tuple<>(
                t,
                Arrays.stream(args)
                    .filter(p -> p._1.equals(t))
                    .map(Tuple::getRight)
                    .findFirst()
                    .orElse(Colors.white)
            ));
        });
        return texts;
    }

    public TextDisplay(List<Tuple<String, Integer>> texts, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, FontRenderer fontRenderer) {
        this.texts = texts;
        widths = texts.stream().map(tuple -> fontRenderer.width(tuple._1)).collect(Collectors.toList());
        this.rect = new Rect(
            point,
            widths.stream().reduce(0, Integer::sum), fontRenderer.lineHeight,
            xAlignment, yAlignment
        );
        this.fontRenderer = fontRenderer;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        int x = rect.point.x;
        for(int i = 0; i < texts.size(); i++) {
            Tuple<String, Integer> textAndColor = texts.get(i);
            fontRenderer.drawShadow(textAndColor._1, x, rect.point.y, textAndColor._2);
            x += widths.get(i);
        }
    }

}
