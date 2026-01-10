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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PdfExecutor {
    private final PageConfig config;
    private PDDocument document;
    private PDAcroForm acroForm;
    private PDPageContentStream contentStream;
    private PDPage page;
    private RenderState state;
    private ComponentFactory componentFactory;
    private File outputFile;
    private Table currentTable;


    public PdfExecutor() throws IOException {
        this.config = PageConfig.DEFAULT;
        state = new RenderState(config);

        document = new PDDocument();
        acroForm = new PDAcroForm(document);
        acroForm.setNeedAppearances(true);
        acroForm.setDefaultAppearance(getDefaultAppearance());

        // ComponentFactory erstellen und Resources initialisieren
        componentFactory = new ComponentFactory(acroForm);

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
                }
            }

            saveDocument();
        } catch (IOException e) {
            throw new IOException("Error while creating PDF: " + e.getMessage());
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

        // Erst nach Newlines splitten, DANN sanitizen
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

                float lineWidth = getTextWidth(line);

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
        } else {
            printText(cmd.position(), cmd.text());
        }


    }

    private void printTextToCell(CellRef cellRef, String value) throws IOException {
        Cell cell = currentTable.getCell(cellRef);
        componentFactory.createTextfield(page, cell.position(), cell.size(), value, state.font(), state.fontSize(), state.color());
    }

    private void executeTable(TableCommand cmd) throws IOException {
        List<Integer> widths = parseSizes(cmd.widths(), cmd.columns());
        List<Integer> heights = parseSizes(cmd.heights(), cmd.rows());

        System.out.println(widths);
        System.out.println(heights);
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
        // TODO
        if (cmd.position() != null) {
            state.setPosition(cmd.position().x(), cmd.position().y());
        }
        if (cmd.cell() != null) {
            //richard ist dran kuss
        }

        switch (cmd.type()) {
            case TEXTBOX -> createTextbox(cmd);
            case DROPDOWN -> createDropdown(cmd);
            case RADIO -> createRadio(cmd);
            case OPTION -> createOption(cmd);
        }

    }

    private void createOption(ControlCommand cmd) throws IOException {
        float size = componentFactory.createCheckbox(document, page, state.x(), state.y());
        state.setPosition(state.x() + size + 2, state.y());
        printText(state.position(), cmd.content());
    }

    private void createRadio(ControlCommand cmd) throws IOException {
        List<String> options = Arrays.stream(cmd.content().split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (options.isEmpty()) {
            throw new IllegalArgumentException("Radio without options");
        }

        componentFactory.createRadio(document, page, state.x(), state.y(), options,
                new ComponentFactory.RadioCallback() {
                    @Override
                    public void onRadioOption(float x, float y, String text) throws IOException {
                        state.setPosition(x, y);
                        printText(null, text);
                        // Position für nächste Option setzen
                        state.setPosition(config.startX(), state.y() - state.fontSize());
                    }

                    @Override
                    public float getNextY() {
                        return state.y();
                    }

                    @Override
                    public float getNextX() {
                        return config.startX();
                    }
                });
    }

    private void createDropdown(ControlCommand cmd) throws IOException {
        List<String> options = Arrays.stream(cmd.content().split(";"))
                .map(String::trim)
                .toList();

        if (options.isEmpty()) {
            throw new IllegalArgumentException("Dropdown without options");
        }

        float width = Math.min(getTextWidth(cmd.content()) + 5, config.textWidth());
        float height = state.fontSize();

        componentFactory.createDropdown(page, state.x(), state.y(), width, height, options, getDefaultAppearance());
        state.setPosition(state.x() + width, state.y());
    }

    private String getDefaultAppearance() {
        // DefaultAppearance mit aktueller Schriftart, Größe und Farbe

        return String.format(java.util.Locale.US, "/%s %d Tf %.3f %.3f %.3f rg",
                state.font().getName(),
                state.fontSize(),
                state.color().getRed() / 255.0f,
                state.color().getGreen() / 255.0f,
                state.color().getBlue() / 255.0f
        );
    }

    private void createTextbox(ControlCommand cmd) throws IOException {
        Size size = new Size(
                (int) Math.min(getTextWidth(cmd.content()), config.textWidth()),
                state.fontSize()
        );

        Position position = cmd.position() != null? cmd.position() : state.position();
        componentFactory.createTextfield(
                page,
                position,
                size,
                cmd.content(),
                state.font(),
                state.fontSize(),
                state.color());
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