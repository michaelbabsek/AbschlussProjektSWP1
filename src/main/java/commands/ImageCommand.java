package commands;

public record ImageCommand(
        Position position,    // @ x,y (optional)
        CellRef cell,         // @cell x,y (optional)
        Size size,            // optional
        String filename
) implements Command {}
