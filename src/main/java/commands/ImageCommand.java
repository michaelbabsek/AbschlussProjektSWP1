package commands;

public record ImageCommand(
        Position position,    // @ x,y (optional)
        CellRef cell,         // @cell x,y (optional)
        Float width,          // optional
        Float height,         // optional
        String filename
) implements Command {}
