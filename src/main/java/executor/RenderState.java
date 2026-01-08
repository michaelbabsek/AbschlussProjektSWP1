package executor;

import commands.FontStyle;
import commands.Position;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.*;
import java.lang.reflect.Field;

public class RenderState {
    private float x;
    private float y;
    private PDFont font;
    private FontStyle fontStyle;
    private Color color;
    private int fontSize = 12;
    private final float leading = 18;

    public RenderState(PageConfig config) {
        this.x = config.startX();
        this.y = config.startY();
        this.fontStyle = FontStyle.REGULAR;
        this.font = FontResolver.resolve("helvetica", fontStyle);
        this.color = Color.BLACK;
    }

    // Getters
    public float x() { return x; }
    public float y() { return y; }
    public Position position() { return new Position(x, y); }
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

    public void advanceX(float distance) {
        this.x += distance;
    }

    public void resetX(PageConfig config) {
        this.x = config.startX();
    }

    public void resetPosition(PageConfig config) {
        this.y = config.startY();
        this.x = config.startX();
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