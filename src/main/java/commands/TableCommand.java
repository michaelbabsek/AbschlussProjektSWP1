package commands;

import java.awt.*;
import java.util.List;

public record TableCommand(
        int columns,
        int rows,
        List<Float> widths,
        List<Float> heights,
        Color lineColor,
        Color background,
        float thickness
) implements Command {}
