package me.pindour.catppuccin.renderer.text;

import me.pindour.catppuccin.api.text.FontStyle;
import me.pindour.catppuccin.api.text.RichText;
import me.pindour.catppuccin.api.text.RichTextSegment;
import me.pindour.catppuccin.gui.themes.catppuccin.CatppuccinGuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderOperation;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//? if >=26.2
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class CatppuccinTextRenderer {
    private final Pool<RichTextOperation> textPool = new Pool<>(RichTextOperation::new);
    private final Map<StyleKey, List<RichTextOperation>> groupedOperations = new HashMap<>();

    public void text(RichText text, double x, double y, Color color, CatppuccinGuiTheme theme) {
        double segmentX = x;

        for (RichTextSegment segment : text.getSegments()) {
            if (segment.getText() == null || segment.getText().isEmpty()) continue;

            RichTextOperation operation = getOperation(textPool, segmentX, y, color)
                    .set(segment, theme.richTextRenderer());

            StyleKey key = new StyleKey(theme.richTextRenderer(), operation.getStyle(), theme.scale(operation.getScale()));

            groupedOperations
                    .computeIfAbsent(key, k -> new ArrayList<>())
                    .add(operation);

            segmentX += theme.textWidth(segment);
        }
    }

    public void render(
            //? if >=26.2
            GuiGraphicsExtractor graphics,
            CatppuccinGuiTheme theme
    ) {
        for (Map.Entry<StyleKey, List<RichTextOperation>> entry : groupedOperations.entrySet()) {
            List<RichTextOperation> textOps = entry.getValue();

            if (textOps.isEmpty()) continue;

            StyleKey key = entry.getKey();

            // Use the renderer captured when queued, including during a theme transition.
            RichTextRenderer renderer = key.renderer();
            renderer.setFontStyle(key.style());
            renderer.begin(
                    //? if >=26.2
                    graphics,
                    key.scale()
            );

            for (RichTextOperation text : textOps) {
                text.run(textPool);
            }

            renderer.end();
            textOps.clear();
        }
        groupedOperations.clear();
    }

    private <T extends GuiRenderOperation<T>> T getOperation(Pool<T> pool, double x, double y, Color color) {
        T op = pool.get();
        op.set(x, y, color);
        return op;
    }

    public record StyleKey(RichTextRenderer renderer, FontStyle style, double scale) { }
}