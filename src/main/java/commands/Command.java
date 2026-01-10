package commands;

public sealed interface Command permits ControlCommand, FontCommand, ImageCommand, NextPageCommand, OutputCommand, PrintCommand, SymbolCommand, TableCommand {}