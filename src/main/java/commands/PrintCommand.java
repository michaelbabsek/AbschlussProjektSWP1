package commands;

public record PrintCommand(
        Position position,    // @ x,y (optional)
        CellRef cell,         // @cell x,y (optional)
        Float width,          // optional
        Alignment alignment,  // enum: LEFT, CENTER, RIGHT (optional)
        String text
) implements Command {}
