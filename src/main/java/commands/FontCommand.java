package commands;

import java.awt.*;

public record FontCommand(Integer size, FontStyle style, String colour, String fontName) implements Command {}
