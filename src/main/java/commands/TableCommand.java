package commands;

import java.awt.*;
import java.util.List;

public record TableCommand(Integer columns, Integer rows, List<String> widths, List<String> heights, String lineColour, String background, Float thickness) implements Command {}
