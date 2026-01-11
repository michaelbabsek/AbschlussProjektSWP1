package executor;

import commands.FontStyle;
import commands.Position;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.*;

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
        this.font = FontHandler.resolve("helvetica", fontStyle);
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

    public void setPosition(Position position) {
        this.x = position.x();
        this.y = position.y();
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
    public void applyFont(String fontName, FontStyle style, String colour, Integer size) {
        if (fontName != null) {
            this.font = FontHandler.resolve(fontName, style);
            this.fontStyle = style;
        }
        if (size != null && size > 0) {
            this.fontSize = size;
        }
        if (colour != null) {
            this.color = ColorHandler.parseColor(colour);
        }
    }

    public String getDefaultAppearance() {
        // DefaultAppearance mit aktueller Schriftart, Größe und Farbe

        return String.format(java.util.Locale.US, "/%s %d Tf %.3f %.3f %.3f rg",
                font().getName(),
                fontSize(),
                color().getRed() / 255.0f,
                color().getGreen() / 255.0f,
                color().getBlue() / 255.0f
        );
    }
}