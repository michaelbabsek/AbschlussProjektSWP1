package tokens;

public sealed interface Token permits WordToken, EnumToken, NumberToken, StringToken, SymbolToken {
    TokenType type();
    int line();
    int column();
}

