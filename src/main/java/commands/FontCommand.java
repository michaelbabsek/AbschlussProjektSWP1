package commands;

import java.awt.*;

public record FontCommand(
        int size,
        FontStyle style,      // enum: REGULAR, BOLD, ITALIC, BOLDITALIC
        Color colour,
        String fontName
) implements Command {}
