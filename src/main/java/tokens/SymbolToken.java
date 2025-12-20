package tokens;

public record SymbolToken(TokenType type, int line, int column) implements Token {}
