package executor;

import commands.FontStyle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.Map;

public class RenderState {
    private float x;
    private float y;
    private PDFont font;
    private FontStyle fontStyle;
    private int fontSize;
    private Color color;
    private float leading;

    public RenderState(PageConfig config) {
        this.x = config.startX();
        this.y = config.startY();
        this.fontStyle = FontStyle.OBLIQUE;
        this.font = FontResolver.resolve("helvetica", fontStyle);
        this.fontSize = 12;
        this.color = Color.BLACK;
        this.leading = fontSize * 1.5f;
    }

    // Getters
    public float x() { return x; }
    public float y() { return y; }
    public PDFont font() { return font; }
    public int fontSize() { return fontSize; }
    public Color color() { return color; }
    public float leading() { return leading; }

    // Position
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void newLine() {
        this.y -= leading;
    }

    public void resetToTop(PageConfig config) {
        this.y = config.startY();
    }

    public boolean isBelowMargin(PageConfig config) {
        return y < config.minY();
    }

    // Font-Handling
    public void applyFont(String fontName, FontStyle style, String colour, int size) {
        if (fontName != null) {
            this.font = FontResolver.resolve(fontName, style);
            this.fontStyle = style;
        }
        if (size > 0) {
            this.fontSize = size;
            this.leading = size * 1.5f;
        }
        if (colour != null) {
            this.color = parseColor(colour);
        }
    }

    private Color parseColor(String input) {
        if (input == null) throw new IllegalArgumentException("Color cannot be null");

        input = input.trim();
        Color color = null;

        try {
            if (input.startsWith("#")) {
                color = Color.decode(input);
            } else if (input.startsWith("0x") || input.startsWith("0X")) {
                color = Color.decode(input);
            } else if (input.matches("[0-9a-fA-F]{6}")) {
                color = Color.decode("#" + input);
            }
        } catch (NumberFormatException ignored) {}

        if (color == null) {
            try {
                Field field = Color.class.getField(input.toUpperCase());
                color = (Color) field.get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalArgumentException("Invalid color: " + input);
            }
        }

        return color;
    }
}