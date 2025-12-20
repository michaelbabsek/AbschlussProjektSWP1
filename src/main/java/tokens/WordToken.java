package tokens;

public record WordToken(String value, int line, int column) implements Token {
    public TokenType type() { return TokenType.WORD; }
}
