package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import javafx.util.Pair;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.*;
import java.util.stream.Collectors;

public class TextDisplay implements IRenderable {

    private final List<Pair<String, Integer>> texts;
    private final List<Integer> widths;
    public final Rect rect;
    private final FontRenderer fontRenderer;

    public TextDisplay(Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, FontRenderer fontRenderer, String translationKey, Pair<String, Integer>... args){
        this(getTextsFor(translationKey, args), point, xAlignment, yAlignment, fontRenderer);
    }

    public TextDisplay(String text, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, FontRenderer fontRenderer) {
        this(Collections.singletonList(new Pair<>(text, Colors.white)), point, xAlignment, yAlignment, fontRenderer);
    }

    private static List<Pair<String, Integer>> getTextsFor(String translationKey, Pair<String, Integer>... args) {
        List<Pair<String, Integer>> texts = new ArrayList<>();
        new TranslationTextComponent(
            translationKey,
            Arrays.stream(args).map(Pair::getKey).toArray(Object[]::new)
        ).getComponent(t -> {
            texts.add(new Pair<>(t, Arrays.stream(args).filter(p -> p.getKey().equals(t)).map(Pair::getValue).findFirst().orElse(Colors.white)));
            return Optional.empty();
        });
        return texts;
    }

    public TextDisplay(List<Pair<String, Integer>> texts, Point point, Rect.XAlignment xAlignment, Rect.YAlignment yAlignment, FontRenderer fontRenderer) {
        this.texts = texts;
        widths = texts.stream().map(pair -> fontRenderer.getStringWidth(pair.getKey())).collect(Collectors.toList());
        this.rect = new Rect(
            point,
            widths.stream().reduce(0, Integer::sum), fontRenderer.FONT_HEIGHT,
            xAlignment, yAlignment
        );
        this.fontRenderer = fontRenderer;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int x = rect.point.x;
        for(int i = 0; i < texts.size(); i++) {
            Pair<String, Integer> textAndColor = texts.get(i);
            fontRenderer.drawStringWithShadow(matrixStack, textAndColor.getKey(), x, rect.point.y, textAndColor.getValue());
            x += widths.get(i);
        }
    }

}
