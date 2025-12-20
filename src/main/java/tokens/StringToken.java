package tokens;

public record StringToken(String value, int line, int column) implements Token {
    public TokenType type() { return TokenType.STRING; }
}
