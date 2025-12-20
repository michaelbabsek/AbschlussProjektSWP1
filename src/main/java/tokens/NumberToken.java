package tokens;

public record NumberToken(double value, int line, int column) implements Token {
    public TokenType type() { return TokenType.NUMBER; }
}
