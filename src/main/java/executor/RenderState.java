package executor;

import commands.FontStyle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.lang.reflect.Field;

public class RenderState {
    private float x;
    private float y;
    private PDType1Font font;
    private FontStyle fontStyle;
    private int fontSize;
    private Color color;
    private float leading;

    public RenderState(PageConfig config) {
        this.x = config.startX();
        this.y = config.startY();
        this.font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        this.fontStyle = FontStyle.REGULAR;
        this.fontSize = 12;
        this.color = Color.BLACK;
        this.leading = fontSize * 1.5f;
    }

    // Getters
    public float x() { return x; }
    public float y() { return y; }
    public PDType1Font font() { return font; }
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
            this.font = resolveFont(fontName, style);
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

    private PDType1Font resolveFont(String name, FontStyle style) {
        StringBuilder fontString = new StringBuilder(name);

        switch (style) {
            case BOLD -> fontString.append("_BOLD");
            case ITALIC -> fontString.append("_ITALIC");
            case BOLD_ITALIC -> fontString.append("_BOLD_ITALIC");
            case OBLIQUE -> fontString.append("_OBLIQUE");
            case BOLD_OBLIQUE -> fontString.append("_BOLD_OBLIQUE");
            case REGULAR -> {}
        }

        try {
            return new PDType1Font(Standard14Fonts.FontName.valueOf(fontString.toString().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Font not found: " + fontString);
        }
    }

    private Color parseColor(String input) {
        if (input == null) throw new IllegalArgumentException("Color cannot be null");

        input = input.trim();
        Color color = null;

        try {
            if (input.startsWith("#")) {
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