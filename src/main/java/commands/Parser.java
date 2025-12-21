package commands;

import tokens.Token;
import tokens.WordToken;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    List<Token> tokens;
    int pos = 0;

    public Parser(List<Token> tokens) throws ParseException {
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
        }
        return commands;
    }

    public Command parseOutput() throws ParseException {
        // TODO: 21.12.25
        return null;
    }

    public Command parseFont() throws ParseException {
        // TODO: 21.12.25
        return null;
    }
    public Command parsePrint() throws ParseException {
        // TODO: 21.12.25
        return null;
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
