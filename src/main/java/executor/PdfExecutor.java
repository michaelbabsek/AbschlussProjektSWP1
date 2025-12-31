package executor;

import commands.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

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

        state.resetPosition(config);
    }

    private void executeFont(FontCommand cmd) {
        state.applyFont(cmd.fontName(), cmd.style(), cmd.colour(), cmd.size());
    }

    private List<String> parseLines(String input, float currentX) throws IOException {
        float maxX = config.startX() + config.textWidth();
        float availableWidth = maxX - currentX;
        List<String> result = new LinkedList<>();

        // Erst nach Newlines splitten, DANN sanitizen
        String[] lines = input.split("\n", -1);

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            String sanitizedLine = FontResolver.sanitize(line, state.font());
            StringBuilder currentLine = new StringBuilder();
            float currentAvailable = (lineIndex == 0) ? availableWidth : config.textWidth();

            int i = 0;
            while (i < sanitizedLine.length()) {
                // Whitespace-Sequenz sammeln
                StringBuilder whitespace = new StringBuilder();
                while (i < sanitizedLine.length() && sanitizedLine.charAt(i) == ' ') {
                    whitespace.append(sanitizedLine.charAt(i));
                    i++;
                }

                // Wort sammeln (alle Nicht-Leerzeichen)
                StringBuilder word = new StringBuilder();
                while (i < sanitizedLine.length() && sanitizedLine.charAt(i) != ' ') {
                    word.append(sanitizedLine.charAt(i));
                    i++;
                }

                // Whitespace + Wort als Einheit behandeln
                String segment = whitespace.toString() + word.toString();
                if (segment.isEmpty()) continue;

                String testStr = currentLine.toString() + segment;
                float lineWidth = state.fontSize() * state.font().getStringWidth(testStr) / 1000.0f;

                if (lineWidth > currentAvailable) {
                    // Segment passt nicht mehr in aktuelle Zeile
                    if (!currentLine.isEmpty()) {
                        result.add(currentLine.toString());
                        currentLine = new StringBuilder();
                        currentAvailable = config.textWidth();
                    }

                    // Führende Leerzeichen am Zeilenanfang weglassen
                    String segmentTrimmed = segment.replaceFirst("^\\s+", "");
                    currentLine.append(segmentTrimmed);
                } else {
                    currentLine.append(segment);
                }
            }

            if (!currentLine.isEmpty()) {
                result.add(currentLine.toString());
            } else if (sanitizedLine.isEmpty()) {
                // Leere Zeilen erhalten (z.B. bei \n)
                result.add("");
            }

            // Nach jeder \n-Zeile außer der letzten einen expliziten Zeilenumbruch markieren
            if (lineIndex < lines.length - 1) {
                result.add("\n");
            }
        }

        return result;
    }

    private void printText(Position pos, String text) throws IOException {
        if (pos != null) {
            state.setPosition(pos.x(), pos.y());
        }

        List<String> lines = parseLines(text, state.x());

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Expliziter Zeilenumbruch durch \n
            if (line.equals("\n")) {
                state.newLine();
                state.resetX(config);

                if (state.isBelowMargin(config)) {
                    addPage();
                    state.resetPosition(config);
                }
                continue;
            }

            // Prüfen ob Umbruch vor diesem Segment nötig ist (nur beim ersten Segment)
            if (i == 0) {
                float textWidth = state.fontSize() * state.font().getStringWidth(line) / 1000.0f;
                float maxX = config.startX() + config.textWidth();
                if (state.x() + textWidth > maxX) {
                    state.newLine();
                    state.resetX(config);

                    if (state.isBelowMargin(config)) {
                        addPage();
                        state.resetPosition(config);
                    }
                }
            }

            // Text in aktuelle Zeile schreiben
            contentStream.beginText();
            contentStream.setFont(state.font(), state.fontSize());
            contentStream.setNonStrokingColor(state.color());
            contentStream.newLineAtOffset(state.x(), state.y());
            contentStream.showText(line);
            contentStream.endText();

            // X-Position aktualisieren
            float textWidth = state.fontSize() * state.font().getStringWidth(line) / 1000.0f;
            state.advanceX(textWidth);

            // Automatischer Zeilenumbruch zwischen Segmenten (parseLines hat sie getrennt, weil sie nicht in eine Zeile passen)
            if (i < lines.size() - 1 && !lines.get(i + 1).equals("\n")) {
                state.newLine();
                state.resetX(config);

                if (state.isBelowMargin(config)) {
                    addPage();
                    state.resetPosition(config);
                }
            }
        }
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
        state.resetPosition(config);
    }

    private PDImageXObject loadImage(String path) throws IOException {
        try {
            return PDImageXObject.createFromFile(path, document);
        } catch (IOException e) {
            throw new IOException("No Image found " +  path);
        }
    }

    private void executeImage(ImageCommand cmd) throws IOException {
        PDImageXObject image = loadImage(cmd.filename());

        // Position setzen falls angegeben
        if (cmd.position() != null) {
            state.setPosition(cmd.position().x(), cmd.position().y());
        }

        // Größe bestimmen
        float width = image.getWidth();
        float height = image.getHeight();

        if (cmd.size() != null) {
            width = cmd.size().width();
            height = cmd.size().height();
        }

        // Bild auf textWidth/textHeight skalieren falls zu groß
        if (width > config.textWidth() || height > config.textHeight()) {
            double scale = Math.min((double) config.textWidth() / width, (double) config.textHeight() / height);
            width = (float) (width * scale);
            height = (float) (height * scale);
        }

        // Verfügbaren Platz berechnen (vom aktuellen Punkt bis zum rechten/unteren Rand)
        // NACH der Skalierung!
        float maxX = config.startX() + config.textWidth();
        float minY = config.minY();
        float availableWidth = maxX - state.x();
        float availableHeight = state.y() - minY;

        // Prüfen ob Bild in verfügbaren Platz passt
        if (height > availableHeight) {
            // Bild passt nicht in verfügbare Höhe -> neue Seite
            addPage();
        } else if (width > availableWidth) {
            // Bild passt nicht in verfügbare Breite -> neue Zeile versuchen
            state.newLine();
            state.resetX(config);

            // Prüfen ob es nach Zeilenumbruch immer noch nicht passt (Höhe)
            if (state.y() - height < minY) {
                addPage();
            }
        }

        contentStream.drawImage(image, state.x(), state.y() - height + state.fontSize(), width, height);
        state.setPosition(state.x() + width, state.y() - height);
    }

    private void closeContentStream() throws IOException {
        if (contentStream != null) {
            contentStream.close();
            contentStream = null;
        }
    }
}