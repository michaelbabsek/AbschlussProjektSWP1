package tokens;

public record EnumToken(String value, int line, int column) implements Token {
    public TokenType type() { return TokenType.ENUM; }
}
