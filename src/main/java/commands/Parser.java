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
                case "image" -> parseImage();
                default -> throw new ParseException("Unknown command: " + w.value(), pos);
            };

            commands.add(cmd);
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

    private Pair parsePair() throws ParseException {
        expect(TokenType.NUMBER);
        float a = ((NumberToken) getCurrentToken()).value();
        advance();

        expect(TokenType.COMMA);
        advance();

        expect(TokenType.NUMBER);
        float b = ((NumberToken) getCurrentToken()).value();
        advance();

        return new Pair(a, b);
    }

    private Position parsePosition() throws ParseException {
        Pair p = parsePair();
        return new Position(p.a(), p.b());
    }

    private Size parseSize() throws ParseException {
        Pair p = parsePair();
        return new Size((int) p.a(), (int) p.b());
    }

    private CellRef parseCellRef() throws ParseException {
        Pair p = parsePair();
        return new CellRef((int) p.a(), (int) p.b());
    }


    public boolean isCommand(Token token) {
        if (token instanceof WordToken) {
            return COMMANDS.contains(((WordToken) token).value().toLowerCase());
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
        advance();  // skip "print"

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
                    text = ((StringToken) token).value();
                    advance();
                }
                case TokenType.ENUM -> {
                    String enumValue = ((EnumToken) token).value().toUpperCase();
                    alignment = Alignment.valueOf(enumValue);
                    advance();
                }
                case TokenType.AT -> {
                    advance();
                    if (check(TokenType.WORD) && ((WordToken) getCurrentToken()).value().equalsIgnoreCase("cell")) {
                        advance();
                        cellRef = parseCellRef();
                    } else {
                        position = parsePosition();
                    }
                }
                case TokenType.WORD -> {
                    String word = ((WordToken) token).value().toLowerCase();
                    if (word.equals("width")) {
                        advance();
                        width = ((NumberToken) getCurrentToken()).value();
                        advance();
                    } else {
                        throw new ParseException("Unexpected word: " + word, pos);
                    }
                }
                default -> throw new ParseException("Unexpected token: " + token, pos);
            }
        }

        if (text == null) {
            throw new ParseException("print requires text", pos);
        }

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
        advance(); // skip "image"

        String fileName = null;
        Position position = null;
        CellRef cellRef = null;
        Size size = null;

        while (pos < tokens.size()) {
            Token token = getCurrentToken();
            switch (token.type()) {
                case TokenType.AT -> {
                    advance();
                    if (check(TokenType.WORD) && ((WordToken) getCurrentToken()).value().equalsIgnoreCase("cell")) {
                        advance();
                        cellRef = parseCellRef();
                    } else {
                        position = parsePosition();
                    }
                }

                case TokenType.WORD -> {
                    if (((WordToken) getCurrentToken()).value().equalsIgnoreCase("size")) {
                        advance();
                        size = parseSize();
                    }
                }

                case TokenType.STRING -> fileName = ((StringToken) getCurrentToken()).value();

                default -> throw new ParseException("Unexpected token: " + token, pos);
            }

        }

        if (fileName == null) {
            throw new ParseException("image requires file name", pos);
        }

        return new ImageCommand(position, cellRef, size, fileName);
    }
}
