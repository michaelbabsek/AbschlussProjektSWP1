package commands;

import tokens.SymbolType;

public record SymbolCommand(
        Position position,
        SymbolType symbol,
        String text
)implements Command {
}
