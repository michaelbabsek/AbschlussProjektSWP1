package executor;

import commands.*;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class PdfExecutor {
    private final PageConfig config;
    private PDDocument document;
    private PDAcroForm acroForm;
    private PDPageContentStream contentStream;
    private PDPage page;
    private RenderState state;
    private File outputFile;


    public PdfExecutor() {
        this.config = PageConfig.DEFAULT;
    }

    public void execute(List<Command> commands) throws IOException {
        try {
            document = new PDDocument();
            acroForm = new PDAcroForm(document);
            acroForm.setNeedAppearances(true); // PDF-Reader generiert Appearances

            // Default Resources für Form-Felder
            PDResources resources = new PDResources();
            resources.put(COSName.getPDFName("Helv"), new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA));
            resources.put(COSName.getPDFName("ZaDb"), new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.ZAPF_DINGBATS));
            acroForm.setDefaultResources(resources);
            acroForm.setDefaultAppearance("/Helv 12 Tf 0 g");

            document.getDocumentCatalog().setAcroForm(acroForm);
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
        // TODO
    }

    private void createRadio(ControlCommand cmd) throws IOException {
        // TODO
    }

    private void createDropdown(ControlCommand cmd) throws IOException {
        // TODO
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
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName(state.font().getName()), state.font());
        acroForm.setDefaultResources(resources);

        String defaultAppearance = getDefaultAppearance();
        acroForm.setDefaultAppearance(defaultAppearance);

        PDTextField textField = new PDTextField(acroForm);
        textField.setPartialName(UUID.randomUUID().toString());
        textField.setValue(cmd.content());

        // DefaultAppearance auch auf das TextField selbst setzen, damit die Farbe übernommen wird
        textField.setDefaultAppearance(defaultAppearance);

        // Breite auf maximal textWidth beschränken
        float width = Math.min(getTextWidth(cmd.content()), config.textWidth());
        float height = state.fontSize();

        PDAnnotationWidget widget = textField.getWidgets().getFirst();
        PDRectangle rect = new PDRectangle();

        rect.setLowerLeftX(state.x());
        rect.setLowerLeftY(state.y()); // Untere Kante unterhalb der aktuellen Position
        rect.setUpperRightX(state.x() + width);
        rect.setUpperRightY(state.y() + height); // Obere Kante auf aktueller Position

        widget.setRectangle(rect);
        widget.setPage(page);

        page.getAnnotations().add(widget);
        acroForm.getFields().add(textField);

        // Position nach rechts verschieben (nach der Textbox)
        state.setPosition(state.x() + width, state.y());
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