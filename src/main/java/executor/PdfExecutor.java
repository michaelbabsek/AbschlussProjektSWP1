package executor;

import commands.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PdfExecutor {
    private final PageConfig config;
    private PDDocument document;
    private PDPageContentStream contentStream;
    private RenderState state;
    private File outputFile;

    public PdfExecutor() {
        this.config = PageConfig.DEFAULT;
    }

    public void execute(List<Command> commands) throws IOException {
        try {
            document = new PDDocument();
            FontResolver.setDocument(document);
            state = new RenderState(config);
            outputFile = null;

            addPage();

            for (Command cmd : commands) {
                switch (cmd) {
                    case OutputCommand c -> executeOutput(c);
                    case FontCommand c -> executeFont(c);
                    case PrintCommand c -> executePrint(c);
                    case TableCommand c -> executeTable(c);
                    case ControlCommand c -> executeControl(c);
                    case NextPageCommand c -> executeNextPage();
                    case ImageCommand c -> executeImage(c);
                }
            }

            saveDocument();
        } finally {
            closeContentStream();
            if (document != null) {
                document.close();
            }
            document = null;
            state = null;
        }
    }

    private void executeOutput(OutputCommand cmd) {
        if (outputFile != null) {
            throw new RuntimeException("Output already specified");
        }

        File file = new File(cmd.filename());
        if (file.exists() && !file.delete()) {
            throw new RuntimeException("File already exists and cannot be deleted: " + cmd.filename());
        }
        outputFile = file;
    }

    private void saveDocument() throws IOException {
        if (outputFile == null) {
            throw new RuntimeException("Output file not specified");
        }
        closeContentStream();
        document.save(outputFile);
    }

    private void addPage() throws IOException {
        closeContentStream();
        PDPage page = new PDPage(config.pageSize());
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);
    }

    private void executeFont(FontCommand cmd) {
        state.applyFont(cmd.fontName(), cmd.style(), cmd.colour(), cmd.size());
    }

    private List<String> parseLines(String input) throws IOException {
        float maxWidth = config.textWidth();
        List<String> result = new LinkedList<>();

        String sanitizedInput = FontResolver.sanitize(input, state.font());
        String[] lines = sanitizedInput.split(System.lineSeparator());


        for (String line : lines) {
            String[] words = line.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                float wordWidth = state.fontSize() * state.font().getStringWidth(word) / 1000.0f;

                if (wordWidth > maxWidth) {
                    if (!currentLine.isEmpty()) {
                        result.add(currentLine.toString());
                    }

                    StringBuilder chunk = new StringBuilder();
                    for (char c : word.toCharArray()) {
                        float chunkWidth = state.fontSize() * state.font().getStringWidth(chunk.toString() + c) / 1000.0f;
                        if (chunkWidth > maxWidth && !chunk.isEmpty()) {
                            result.add(chunk.toString());
                            chunk = new StringBuilder();
                        }
                        chunk.append(c);
                    }
                    currentLine = chunk;
                    continue;
                }

                String testStr = currentLine.isEmpty() ? word : currentLine + " " + word;
                float lineWidth = state.fontSize() * state.font().getStringWidth(testStr) / 1000.0f;

                if (lineWidth > maxWidth) {
                    result.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testStr);
                }
            }

            if (!currentLine.isEmpty()) {
                result.add(currentLine.toString());
            }
        }

        return result;
    }

    private void printText(Position pos, String text) throws IOException {
        List<String> lines = parseLines(text);

        if (pos != null) {
            state.setPosition(pos.x(), pos.y());
        }

        contentStream.beginText();
        contentStream.setFont(state.font(), state.fontSize());
        contentStream.setNonStrokingColor(state.color());
        contentStream.newLineAtOffset(state.x(), state.y());

        for (String line : lines) {
            state.newLine();

            if (state.isBelowMargin(config)) {
                contentStream.endText();
                addPage();
                state.resetToTop(config);

                contentStream.beginText();
                contentStream.setFont(state.font(), state.fontSize());
                contentStream.setNonStrokingColor(state.color());

                state.newLine();
                contentStream.newLineAtOffset(state.x(), state.y());
            } else {
                contentStream.newLineAtOffset(0, -state.leading());
            }

            contentStream.showText(line);
        }
        contentStream.endText();
    }

    private void executePrint(PrintCommand cmd) throws IOException {
        if (cmd.position() != null && cmd.cell() != null) {
            throw new RuntimeException("position and cell cannot both be specified");
        }

        if (cmd.cell() != null) {
            // TODO: printTextToCell
        } else {
            printText(cmd.position(), cmd.text());
        }
    }

    private void executeTable(TableCommand cmd) throws IOException {
        // TODO
    }

    private void executeControl(ControlCommand cmd) throws IOException {
        // TODO
    }

    private void executeNextPage() throws IOException {
        addPage();
        state.resetToTop(config);
    }

    private void executeImage(ImageCommand cmd) throws IOException {
        // TODO
    }

    private void closeContentStream() throws IOException {
        if (contentStream != null) {
            contentStream.close();
            contentStream = null;
        }
    }
}