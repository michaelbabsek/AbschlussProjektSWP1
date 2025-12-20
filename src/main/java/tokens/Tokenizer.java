package tokens;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    String script = "";
    int pos = 0;
    int line = 1;
    int column = 1;

    public Tokenizer(String script) {
        this.script = script;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        for (; pos < script.length(); pos++) {
            skipWhitespaces();

            char c = script.charAt(pos);
            switch (c) {
                case '@':
                    tokens.add(new SymbolToken(TokenType.AT, line, column));
                    advance();
                    break;
                case ',':
                    tokens.add(new SymbolToken(TokenType.COMMA, line, column));
                    advance();
                    break;
            }
        }
        return tokens;
    }
    private void skipWhitespaces() {
        while (pos < script.length() && Character.isWhitespace(script.charAt(pos))) {
            advance();
        }
    }

    private void advance() {
        char c = script.charAt(pos++);

        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }

    }
}
