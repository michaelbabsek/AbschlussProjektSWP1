package tokens;

public sealed interface Token permits CommandToken, EnumToken, NumberToken, StringToken, SymbolToken {
    TokenType type();
    int line();
    int column();
}

