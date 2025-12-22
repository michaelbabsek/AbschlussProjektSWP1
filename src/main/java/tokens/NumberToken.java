package tokens;

public record NumberToken(float value, int line, int column) implements Token {
    public TokenType type() { return TokenType.NUMBER; }
}
