package commands;

public record ImageCommand(
        Position position,
        Float width,
        Float height,
        String filename
) implements Command {}
