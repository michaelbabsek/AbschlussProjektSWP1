package commands;

public record ControlCommand(
        Position position,
        CellRef cell,
        ControlType type,     // enum: TEXTBOX, DROPDOWN, RADIO, OPTION
        String content
) implements Command {}