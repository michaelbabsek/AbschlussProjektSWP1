package commands;

import tokens.SymbolType;

public record SymbolCommand(
        Position position,
        SymbolType symbol
)implements Command {
}
