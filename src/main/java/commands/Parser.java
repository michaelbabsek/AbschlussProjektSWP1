package commands;

import tokens.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Parser {
    List<Token> tokens;
    int pos = 0;
    Set<String> COMMANDS = Set.of("output", "font", "print", "table", "control", "nextpage", "image", "symbol");

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
                case "nextpage" -> parseNextPage();
                case "image" -> parseImage();
                case "symbol" -> parseSymbol();
                default -> throw new ParseException("Unknown command: " + w.value(), pos);
            };

            commands.add(cmd);
        }
        return commands;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
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

    private boolean isAtEnd() {
        return pos >= tokens.size();
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

    public Command parseNextPage() {
        advance();
        return new NextPageCommand();
    }

    public Command parseOutput() throws ParseException {
        advance();
        expect(TokenType.STRING);
        StringToken token = (StringToken) getCurrentToken();
        advance();
        return new OutputCommand(token.value());
    }

    public Command parsePrint() throws ParseException {
        advance();  // skip "print"

        Position position = null;
        CellRef cellRef = null;
        Float width = null;
        Alignment alignment = null;
        String text = null;

        while (!isAtEnd()) {
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
    public Command parseFont() throws ParseException {
        advance();

        Integer size = null;
        FontStyle style = null;
        String colour = null;
        String fontName = null;

        while (!isAtEnd()) {
            Token token = getCurrentToken();

            if (isCommand(token)) break;

            switch (token.type()) {
                case TokenType.STRING -> {
                    fontName = ((StringToken) token).value();
                    advance();
                }
                case TokenType.WORD -> {
                    String word = ((WordToken) token).value().toLowerCase();
                    switch (word) {
                        case "size" -> {
                            advance();
                            size = (int) ((NumberToken) getCurrentToken()).value();
                            advance();
                        }
                        case "style" -> {
                            advance();
                            String styleValue = ((WordToken) getCurrentToken()).value().toUpperCase();
                            style = FontStyle.valueOf(styleValue);
                            advance();
                        }
                        case "colour", "color" -> {
                            advance();
                            colour = parseColour();
                        }
                        default -> throw new ParseException("Unexpected word: " + word, pos);
                    }
                }
                default -> throw new ParseException("Unexpected token: " + token, pos);
            }
        }

        return new FontCommand(size, style, colour, fontName);
    }

    private String parseColour() throws ParseException {
        Token token = getCurrentToken();
        String colour;

        if (token instanceof NumberToken n) {
            colour = n.rawValue();
        } else if (token instanceof WordToken w) {
            colour = w.value();
        } else {
            throw new ParseException("Expected colour, got: " + token, pos);
        }

        advance();
        return colour;
    }

    public Command parseTable() throws ParseException {
        advance();

        Integer columns = null;
        Integer rows = null;
        List<String> widths = null;
        List<String> heights = null;
        String lineColour = null;
        String background = null;
        Float thickness = null;

        while (!isAtEnd()) {
            Token token = getCurrentToken();

            if (isCommand(token)) break;

            switch (token.type()) {
                case TokenType.WORD -> {
                    String word = ((WordToken) token).value().toLowerCase();
                    switch (word) {
                        case "columns" -> {
                            advance();
                            columns = (int) ((NumberToken) getCurrentToken()).value();
                            advance();
                        }
                        case "rows" -> {
                            advance();
                            rows = (int) ((NumberToken) getCurrentToken()).value();
                            advance();
                        }
                        case "width" -> {
                            advance();
                            widths = parseNumberList();
                        }
                        case "height" -> {
                            advance();
                            heights = parseNumberList();
                        }
                        case "line" -> {
                            advance();
                            Token colourToken = getCurrentToken();
                            if (colourToken instanceof NumberToken n) {
                                lineColour = n.rawValue();
                            } else if (colourToken instanceof WordToken w) {
                                lineColour = w.value();
                            }
                            advance();
                        }
                        case "background" -> {
                            advance();
                            Token colourToken = getCurrentToken();
                            if (colourToken instanceof NumberToken n) {
                                background = n.rawValue();
                            } else if (colourToken instanceof WordToken w) {
                                background = w.value();
                            }
                            advance();
                        }
                        case "thickness" -> {
                            advance();
                            thickness = ((NumberToken) getCurrentToken()).value();
                            advance();
                        }
                        default -> throw new ParseException("Unexpected word: " + word, pos);
                    }
                }
                default -> throw new ParseException("Unexpected token: " + token, pos);
            }
        }

        return new TableCommand(columns, rows, widths, heights, lineColour, background, thickness);
    }

    public Command parseControl() throws ParseException {
        advance();

        Position position = null;
        CellRef cellRef = null;
        ControlType type = null;
        String content = null;

        while (!isAtEnd()) {
            Token token = getCurrentToken();

            if (isCommand(token)) break;

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
                case TokenType.ENUM -> {
                    String enumValue = ((EnumToken) token).value().toUpperCase();
                    type = ControlType.valueOf(enumValue);
                    advance();
                }
                case TokenType.WORD -> {
                    String word = ((WordToken) token).value().toLowerCase();
                    switch (word) {
                        case "type" -> {
                            advance();
                            String typeValue = ((WordToken) getCurrentToken()).value().toUpperCase();
                            type = ControlType.valueOf(typeValue);
                            advance();
                        }
                        case "content" -> {
                            advance();
                            content = ((StringToken) getCurrentToken()).value();
                            advance();
                        }
                        default -> throw new ParseException("Unexpected word: " + word, pos);
                    }
                }
                case TokenType.STRING -> {
                    content = ((StringToken) token).value();
                    advance();
                }
                default -> throw new ParseException("Unexpected token: " + token, pos);
            }
        }

        return new ControlCommand(position, cellRef, type, content);
    }

    private List<String> parseNumberList() throws ParseException {
        List<String> values = new ArrayList<>();

        while (check(TokenType.NUMBER)) {
            NumberToken token = (NumberToken) getCurrentToken();
            String value = token.rawValue();
            advance();

            // Wildcard *
            if (check(TokenType.STAR)) {
                value += "*";
                advance();
            }

            values.add(value);

            if (check(TokenType.COMMA)) {
                advance();
            } else {
                break;
            }
        }

        return values;
    }
    public Command parseImage() throws ParseException {
        advance();

        String fileName = null;
        Position position = null;
        CellRef cellRef = null;
        Size size = null;

        while (!isAtEnd()) {
            Token token = getCurrentToken();

            if (isCommand(token)) break;

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
                    expectString(getCurrentToken(), "size");
                    advance();
                    size = parseSize();
                }
                case TokenType.STRING -> {
                    fileName = ((StringToken) getCurrentToken()).value();
                    advance();
                }
                default -> throw new ParseException("Unexpected token: " + token, pos);
            }
        }

        if (fileName == null) {
            throw new ParseException("image requires file name", pos);
        }

        return new ImageCommand(position, cellRef, size, fileName);
    }
    private Command parseSymbol() throws ParseException {
        advance();

        Position position = null;
        SymbolType symbol = null;
        String text= null;

        while (!isAtEnd()) {
            Token token = getCurrentToken();
            if (isCommand(token)) break;
            switch (token.type()) {
                case TokenType.AT -> {
                    advance();
                    position = parsePosition();
                }
                case TokenType.ENUM -> {
                    String enumValue = ((EnumToken) token).value().toUpperCase();
                    symbol = SymbolType.valueOf(enumValue);
                    advance();
                }
                case TokenType.STRING -> {
                    text = ((StringToken) token).value();
                    advance();
                }
                default -> throw new ParseException("Unexpected token in symbol: " + token, pos);
            }
        }
        if (symbol == null){
            throw new ParseException("symbol requires type", pos);
        }
        return new SymbolCommand(position, symbol, text);
    }

}
