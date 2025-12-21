package commands;

public sealed interface Command permits
        OutputCommand, FontCommand, PrintCommand, TableCommand,
        ControlCommand, NextPageCommand, ImageCommand {}