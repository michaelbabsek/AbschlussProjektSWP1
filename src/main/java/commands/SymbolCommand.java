package commands;

import tokens.SymbolType;

public record SymbolCommand(
        Position position,
        CellRef cellRef,
        SymbolType symbol,
        Alignment alignment
)implements Command {
}
