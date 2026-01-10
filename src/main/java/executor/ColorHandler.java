package executor;

import java.awt.*;
import java.lang.reflect.Field;

public class ColorHandler {
    public static Color parseColor(String input) {
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
