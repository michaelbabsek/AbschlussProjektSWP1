package executor;

import commands.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import table.Cell;
import table.Table;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class PdfExecutor {
    private final PageConfig config;
    private final PDDocument document;
    private PDPageContentStream contentStream;
    private PDPage page;
    private final RenderState state;
    private final ComponentFactory componentFactory;
    private File outputFile;
    private Table currentTable;


    public PdfExecutor() throws IOException {
        this.config = PageConfig.DEFAULT;
        state = new RenderState(config);

        document = new PDDocument();
        PDAcroForm acroForm = new PDAcroForm(document);
        acroForm.setNeedAppearances(true);

        // ComponentFactory erstellen und Resources initialisieren
        componentFactory = new ComponentFactory(acroForm);

        // Initialen Font registrieren (wichtig für Default Appearance)
        componentFactory.registerFont(state.font());
        acroForm.setDefaultAppearance(state.getDefaultAppearance());

        document.getDocumentCatalog().setAcroForm(acroForm);
        addPage();
    }

    public void execute(List<Command> commands) throws IOException {
        try {
            for (Command cmd : commands) {
                switch (cmd) {
                    case OutputCommand c -> executeOutput(c);
                    case FontCommand c -> executeFont(c);
                    case PrintCommand c -> executePrint(c);
                    case TableCommand c -> executeTable(c);
                    case ControlCommand c -> executeControl(c);
                    case NextPageCommand c -> executeNextPage();
                    case ImageCommand c -> executeImage(c);
                    case SymbolCommand c -> executeSymbol(c);
                }
            }

            saveDocument();
        } catch (IOException e) {
            throw new IOException("Error while creating PDF: " + e.getMessage());
        }
    }

    private void executeSymbol(SymbolCommand cmd) throws IOException {
        if (cmd.position() != null && cmd.cellRef() != null) {
            throw new RuntimeException("position and cell cannot both be specified");
        }

        String symbol= switch (cmd.symbol()){
            case BULLET -> "•";
            case DASH -> "–";
        };

        if (cmd.position() != null){
            printText(cmd.position(), symbol);
            return;
        }

        if (cmd.cellRef() != null) {
            printTextToCell(cmd.cellRef(), symbol);
            return;
        }

        printText(state.position(), symbol);
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
        this.page = page;
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

        // String an Newlines aufteilen (wurden bereits vom Tokenizer aus \n konvertiert)
        String[] lines = input.split("\n", -1);

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            String sanitizedLine = FontHandler.sanitize(line, state.font());
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
                String segment = whitespace.toString() + word;
                if (segment.isEmpty()) continue;

                // Prüfen ob currentLine + segment noch passt
                float newLineWidth = getTextWidth(currentLine + segment);

                if (newLineWidth > currentAvailable) {
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
                float textWidth = getTextWidth(line);
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
            float textWidth = getTextWidth(line);
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

    private float getTextWidth(String text) throws IOException {
        return state.fontSize() * state.font().getStringWidth(text) / 1000.0f;
    }

    private void executePrint(PrintCommand cmd) throws IOException {
        if (cmd.position() != null && cmd.cell() != null) {
            throw new RuntimeException("position and cell cannot both be specified");
        }

        if (cmd.cell() != null) {
            printTextToCell(cmd.cell(), cmd.text());
            return;
        }

        printText(cmd.position(), cmd.text());
    }

    private void printTextToCell(CellRef cellRef, String value) throws IOException {
        Cell cell = currentTable.getCell(cellRef);
        componentFactory.createTextfield(page, cell.position(), cell.size(), value, state.font(), state.fontSize(), state.color(), true);
    }

    private void executeTable(TableCommand cmd) throws IOException {
        List<Integer> widths = parseSizes(cmd.widths(), cmd.columns());
        List<Integer> heights = parseSizes(cmd.heights(), cmd.rows());

        currentTable = new Table(state.position(), widths, heights, cmd.columns(), cmd.rows());

        Color lineColor = cmd.lineColour() != null? ColorHandler.parseColor(cmd.lineColour()) : state.color();
        Color backgroundColor = cmd.background() != null? ColorHandler.parseColor(cmd.background()) : null;

        for (int col = 0; col < cmd.columns(); col++) {
            for (int row = 0; row < cmd.rows(); row++) {
                Cell cell = currentTable.getCell(new CellRef(col, row));

                Rectangle rect = new Rectangle(
                        (int) cell.position().x(),
                        (int) cell.position().y(),
                        cell.size().width(),
                        cell.size().height()
                );
                componentFactory.drawRect(contentStream, rect, lineColor, backgroundColor, cmd.thickness());
            }
        }
        Size tableSize = currentTable.getSize();
        state.setPosition(state.position().x() + tableSize.width(), state.position().y() - tableSize.height());
    }

    /**
     * Parst eine Liste von Größenangaben wie "10, 20, 30, 40*"
     * Das * am letzten Wert bedeutet, dass dieser für alle restlichen gilt.
     */
    private List<Integer> parseSizes(List<String> sizes, int count) {
        List<Integer> result = new ArrayList<>();

        if (sizes == null || sizes.isEmpty()) {
            // Default: gleiche Größe für alle
            for (int i = 0; i < count; i++) {
                result.add(50); // Default-Wert
            }
            return result;
        }

        float repeatValue = 0;
        boolean hasRepeat = false;

        for (String size : sizes) {
            String trimmed = size.trim();
            if (trimmed.endsWith("*")) {
                // Wiederholungswert
                repeatValue = Float.parseFloat(trimmed.substring(0, trimmed.length() - 1));
                hasRepeat = true;
                result.add((int) repeatValue);
            } else {
                result.add((int) Float.parseFloat(trimmed));
            }
        }

        // Restliche Werte mit repeatValue auffüllen
        if (hasRepeat) {
            while (result.size() < count) {
                result.add((int) repeatValue);
            }
        }

        return result;
    }

    private void executeControl(ControlCommand cmd) throws IOException {
        if (cmd.position() != null && cmd.cell() != null) {
            throw new RuntimeException("position and cell cannot both be specified");
        }

        Size maxSize = null;
        Position position;

        if (cmd.position() == null && cmd.cell() == null) {
            position = state.position();
        } else if (cmd.position() != null) {
            position = cmd.position();
        } else {
            Cell cell = currentTable.getCell(cmd.cell());
            maxSize = cell.size();
            position = cell.position();
        }

        switch (cmd.type()) {
            case TEXTBOX -> createTextbox(cmd.content(), position, maxSize);
            case DROPDOWN -> createDropdown(cmd.content(), position, maxSize);
            case RADIO -> createRadio(cmd.content(), position, maxSize);
            case OPTION -> createOption(cmd.content(), position, maxSize);
        }
    }

    private void createOption(String label, Position position, Size maxSize) throws IOException {
        componentFactory.createCheckbox(document, page, position, maxSize,
                label, state.font(), state.fontSize(), state.color());
    }

    private void createRadio(String options, Position position, Size size) throws IOException {
        List<String> radioOptions = Arrays.stream(options.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (options.isEmpty()) {
            throw new IllegalArgumentException("Radio without options");
        }

        componentFactory.createRadio(document, page, position, radioOptions, size,
                state.font(), state.fontSize(), state.color());
    }
    private void createDropdown(String options, Position position, Size size) throws IOException {
        List<String> dropdownOptions = Arrays.stream(options.split(";"))
                .map(String::trim)
                .toList();

        if (options.isEmpty()) {
            throw new IllegalArgumentException("Dropdown without options");
        }

        size = size != null? size : new Size(
                (int) Math.min(getTextWidth(dropdownOptions.stream().max(Comparator.comparingInt(String::length)).get()), config.textWidth()),
                state.fontSize()
        );

        componentFactory.createDropdown(page, position, size, dropdownOptions, state.font(), state.fontSize(), state.color());
    }
    private void createTextbox(String label, Position position, Size size) throws IOException {

        size = size != null? size : new Size(
                (int) Math.min(getTextWidth(label), config.textWidth()),
                state.fontSize()
        );

        Position pos = position != null? position : state.position();
        componentFactory.createTextfield(
                page,
                position,
                size,
                label,
                state.font(),
                state.fontSize(),
                state.color(),
                false);
    }
    private void executeNextPage() throws IOException {
        addPage();
        state.resetPosition(config);
    }
    private PDImageXObject loadImage(String path) throws IOException {
        try {
            return PDImageXObject.createFromFile(path, document);
        } catch (IOException e) {
            throw new IOException("No Image found " + path);
        }
    }
    private void executeImage(ImageCommand cmd) throws IOException {
        PDImageXObject image = loadImage(cmd.filename());

        if (cmd.position() != null) {
            state.setPosition(cmd.position().x(), cmd.position().y());
        }

        Size size = calculateImageSize(image, cmd.size());
        adjustPositionForImage(size.width(), size.height());

        contentStream.drawImage(image, state.x(), state.y() - size.height() + state.fontSize(), size.width(), size.height());
        state.setPosition(state.x() + size.width(), state.y() - size.height());
    }
    private Size calculateImageSize(PDImageXObject image, Size requestedSize) {
        float width = image.getWidth();
        float height = image.getHeight();

        if (requestedSize != null) {
            width = requestedSize.width();
            height = requestedSize.height();
        }

        // Auf textWidth/textHeight skalieren falls zu groß
        if (width > config.textWidth() || height > config.textHeight()) {
            double scale = Math.min((double) config.textWidth() / width, (double) config.textHeight() / height);
            width = (float) (width * scale);
            height = (float) (height * scale);
        }

        return new Size((int) width, (int) height);
    }
    private void adjustPositionForImage(float width, float height) throws IOException {
        float maxX = config.startX() + config.textWidth();
        float minY = config.minY();
        float availableWidth = maxX - state.x();
        float availableHeight = state.y() - minY;

        if (height > availableHeight) {
            addPage();
        } else if (width > availableWidth) {
            state.newLine();
            state.resetX(config);

            if (state.y() - height < minY) {
                addPage();
            }
        }
    }
    private void closeContentStream() throws IOException {
        if (contentStream != null) {
            contentStream.close();
            contentStream = null;
        }
    }
}