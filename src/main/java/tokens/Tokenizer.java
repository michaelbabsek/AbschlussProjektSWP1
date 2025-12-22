package tokens;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    String script;
    int pos = 0;
    int line = 1;
    int column = 1;

    public Tokenizer(String script) {
        this.script = script;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < script.length()) {
            skipWhitespaces();
            if (pos >= script.length()) break;
            char c = script.charAt(pos);

            if (Character.isDigit(c) || c == '-' || c == '+') {
                tokens.add(readNumber(line, column));
            } else if (Character.isLetter(c)) {
                tokens.add(readWord(line, column));
            } else if (c == '"') {
                tokens.add(readString(line, column));
            } else if (c == '{') {
                tokens.add(readEnum(line, column));
            } else if (c == '*') {
                tokens.add(new SymbolToken(TokenType.STAR, line, column));
                advance();
            } else if (c == '@') {
                tokens.add(new SymbolToken(TokenType.AT, line, column));
                advance();
            } else if (c == ',') {
                tokens.add(new SymbolToken(TokenType.COMMA, line, column));
                advance();
            } else {
                throw new RuntimeException("Unexpected character '" + c + "' at " + line + ":" + column);
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

    private NumberToken readNumber(int startLine, int startColumn) {
        StringBuilder number = new StringBuilder();
        char c = script.charAt(pos);

        // Vorzeichen
        if (c == '-' || c == '+') {
            number.append(c);
            advance();
        }

        // Hex: 0xA0A0A0
        if (pos < script.length() && script.charAt(pos) == '0'
                && pos + 1 < script.length() && script.charAt(pos + 1) == 'x') {
            number.append(script.charAt(pos));
            advance();
            number.append(script.charAt(pos));
            advance();
            while (pos < script.length() && isHexChar(script.charAt(pos))) {
                number.append(script.charAt(pos));
                advance();
            }
            return new NumberToken(0, number.toString(), startLine, startColumn);
        }

        // Normale Zahl
        while (pos < script.length() && (Character.isDigit(script.charAt(pos)) || script.charAt(pos) == '.')) {
            number.append(script.charAt(pos));
            advance();
        }

        // Wildcard *
        if (pos < script.length() && script.charAt(pos) == '*') {
            number.append(script.charAt(pos));
            advance();
        }

        try {
            String raw = number.toString();
            String numPart = raw.endsWith("*") ? raw.substring(0, raw.length() - 1) : raw;
            return new NumberToken(Float.parseFloat(numPart), raw, startLine, startColumn);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed converting String '" + number + "' to number at " + line + ":" + column);
        }
    }

    private boolean isHexChar(char c) {
        return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private WordToken readWord(int startLine, int startColumn) {
        StringBuilder word = new StringBuilder();
        while (pos < script.length() && (Character.isLetterOrDigit(script.charAt(pos)) || script.charAt(pos) == '_')) {
            word.append(script.charAt(pos));
            advance();
        }
        return new WordToken(word.toString(), startLine, startColumn);
    }

    private StringToken readString(int startLine, int startColumn) {
        StringBuilder str = new StringBuilder();
        advance(); //skipping '"'
        while (pos < script.length() && script.charAt(pos) != '"') {
            str.append(script.charAt(pos));
            advance();
        }

        if (pos >= script.length()) {
            throw new RuntimeException("Unterminated string at " + startLine + ":" + startColumn);
        }

        advance(); // last character was '"', advancing to avoid reading the rest of the script
        return new StringToken(str.toString(), startLine, startColumn);
    }

    private EnumToken readEnum(int startLine, int startColumn) {
        StringBuilder str = new StringBuilder();
        advance();

        while (pos < script.length() && script.charAt(pos) != '}') {
            System.out.println(pos);
            str.append(script.charAt(pos));
            advance();
        }

        if (pos >= script.length()) {
            throw new RuntimeException("Unterminated enum at " + startLine + ":" + startColumn);
        }

        advance(); // last character was '}', advancing to avoid reading the rest of the script
        return new EnumToken(str.toString(), startLine, startColumn);
    }
}
