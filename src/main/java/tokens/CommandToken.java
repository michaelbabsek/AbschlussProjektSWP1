package tokens;

public record CommandToken(String value, int line, int column) implements Token {
    public TokenType type() { return TokenType.COMMAND; }
}
