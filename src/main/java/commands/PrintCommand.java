package commands;

public record PrintCommand(
        Position position,    // @ x,y
        CellRef cell,         // @cell x,y (optional)
        Float width,          // optional
        Alignment alignment,  // enum: LEFT, CENTER, RIGHT
        String text
) implements Command {}
