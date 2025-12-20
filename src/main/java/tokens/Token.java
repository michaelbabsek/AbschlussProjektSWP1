package tokens;

public sealed interface Token permits WordToken, NumberToken, StringToken, SymbolToken {
    TokenType type();
    int line();
    int column();
}

