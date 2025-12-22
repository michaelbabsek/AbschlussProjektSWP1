package commands;

import tokens.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Parser {
    List<Token> tokens;
    int pos = 0;

    Set<String> COMMANDS = Set.of("output", "font", "print", "table", "control", "nextpage", "image");

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Command> parse() throws ParseException {
        List<Command> commands = new ArrayList<>();

        while (pos < tokens.size()) {
            Token token = tokens.get(pos);
            if (!(token instanceof WordToken w)) {
                throw new ParseException("Expected command, got: " + token, pos);
            }

            Command cmd = switch (w.value().toLowerCase()){
                case "output" -> parseOutput();
                case "font" -> parseFont();
                case "print" -> parsePrint();
                case "table" -> parseTable();
                case "control" -> parseControl();
                case "nextpage" -> new NextPageCommand();
                case "image", "import" -> parseImage();
                default -> throw new ParseException("Unknown command: " + w.value(), pos);
            };

            commands.add(cmd);

            advance();
        }
        return commands;
    }

    private boolean check(TokenType type) {
        Token token = tokens.get(pos);
        return switch (type) {
            case AT, COMMA, STAR -> token instanceof SymbolToken s && s.type() == type;
            case ENUM -> token instanceof EnumToken;
            case STRING -> token instanceof StringToken;
            case NUMBER -> token instanceof NumberToken;
            case WORD -> token instanceof WordToken;
        };
    }

    private Token getCurrentToken() {
        return tokens.get(pos);
    }

    private void advance() {
        pos++;
    }

    private void expect(TokenType type) throws ParseException {
        if (!check(type)) {
            throw new ParseException("Expected " + type + ", got: " + getCurrentToken(), pos);
        }
    }

    private void expectString(Token token, String expected) throws ParseException {
        if (!(check(TokenType.STRING) || check(TokenType.WORD))) {
            throw new ParseException("Expected " + token.type() + ", got: " + getCurrentToken(), pos);
        }

        String actual;

        if (token instanceof StringToken) {
            actual = ((StringToken) token).value();
        } else {
            actual = ((WordToken) token).value();
        }

        if (!(actual.equalsIgnoreCase(expected))) {
            throw new ParseException("Expected String value: " + expected + ", got: " + actual, pos);
        }
    }

    public Position getPosition() throws ParseException {
        expect(TokenType.NUMBER);
        float x = ((NumberToken) getCurrentToken()).value();
        advance();

        expect(TokenType.COMMA);
        advance();

        expect(TokenType.NUMBER);
        float y = ((NumberToken) getCurrentToken()).value();
        advance();

        return new Position(x, y);
    }
    public CellRef getCellRef() throws ParseException {
        expect(TokenType.NUMBER);
        float column = ((NumberToken) getCurrentToken()).value();
        advance();

        expect(TokenType.COMMA);
        advance();

        expect(TokenType.NUMBER);
        float row = ((NumberToken) getCurrentToken()).value();
        advance();

        return new CellRef((int) column, (int) row);
    }

    public boolean isCommand(Token token) {
        if (token instanceof WordToken) {
            return COMMANDS.contains(((WordToken) token).value());
        }

        return false;
    }

    public Command parseOutput() throws ParseException {
        advance();
        expect(TokenType.STRING);
        StringToken token = (StringToken) getCurrentToken();
        return new OutputCommand(token.value());
    }

    public Command parseFont() throws ParseException {
        // TODO: 21.12.25
        return null;
    }
    public Command parsePrint() throws ParseException {
        advance();

        Position position = null;
        CellRef cellRef = null;
        Float width = null;
        Alignment alignment = null;
        String text = null;

        while (pos < tokens.size()) {
            Token token = getCurrentToken();

            if (isCommand(token)) break;

            switch (token.type()) {
                case TokenType.STRING -> {
                    assert token instanceof StringToken;
                    text = ((StringToken) token).value();
                }
                case TokenType.ENUM -> {
                    String enumValue = "";
                    try {
                        assert token instanceof EnumToken;
                        enumValue = ((EnumToken) token).value().toUpperCase();
                        alignment = Alignment.valueOf(enumValue);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Illegal Argument: " + enumValue);
                    }
                }
                case TokenType.AT -> {
                    advance();
                    Token currToken = getCurrentToken();
                    switch (currToken.type()) {
                        case TokenType.WORD -> {
                            expectString(currToken, "cell");
                            advance();
                            cellRef = getCellRef();

                        }
                        case TokenType.NUMBER -> position = getPosition();

                        default -> throw new ParseException("Unexpected token " + token + " at " + token.line() + ":" + token.column(), pos);
                    }

                }
                case TokenType.NUMBER -> {
                    assert token instanceof NumberToken;
                    width = ((NumberToken) token).value();
                }
                default ->
                        throw new ParseException("Unexpected token " + token + " at " + token.line() + ":" + token.column(), pos);
            }
            advance();
        }

        if (text == null) throw new ParseException("print requires text to not be null", pos);

        return new PrintCommand(position, cellRef, width, alignment, text);
    }
    public Command parseTable() throws ParseException {
        // TODO: 21.12.25
        return null;
    }
    public Command parseControl() throws ParseException {
        // TODO: 21.12.25
        return null;
    }
    public Command parseImage() throws ParseException {
        // TODO: 21.12.25
        return null;
    }
}
