package executor;

import commands.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDTableAttributeObject;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

public class PdfExecutor {
    private PDDocument document;
    private PDPage currentPage;
    private PDPageContentStream contentStream;

    // Aktueller Zustand
    private File outputFile;
    private float currentX = 0;
    private float currentY = 0;
    private PDType1Font currentFont = new PDType1Font((Standard14Fonts.FontName.HELVETICA));
    private FontStyle currentFontStyle = FontStyle.REGULAR;
    private int currentFontSize = 12;
    private Color currentColour = Color.BLACK;
    private PDTableAttributeObject currentTable;

    public void execute(List<Command> commands) throws IOException, NoSuchFieldException, IllegalAccessException {
        document = createDocument();

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
    }

    private void executeOutput(OutputCommand cmd) {
        if (outputFile != null) {
            throw new RuntimeException("Output already specified");
        }

        File file = new File(cmd.filename());
        if (file.exists()) {
            if (!file.delete()) {
                throw new RuntimeException("File already exists and cannot be deleted: " + cmd.filename());
            }
        }
        outputFile = file;
    }

    private PDDocument createDocument() throws IOException {
        document = new PDDocument();
        addPage();

        try {
            updateContentStream();
        } catch (IOException e) {
            throw new IOException("Failed to create new PDF document");
        }
        return document;
    }

    private void saveDocument() throws IOException {
        closeContentStream();
        document.save(outputFile);
    }

    private void addPage() {
        if (document == null) {
            throw new RuntimeException("Document is null");
        }
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        currentPage = page;
    }

    public Color parseColor(String input) {
        if (input == null) throw new IllegalArgumentException("Color cannot be null");

        input = input.trim();
        Color color = null;

        try {
            if (input.startsWith("#")) {
                color = Color.decode(input);
            } else if (input.matches("[0-9a-fA-F]{6}")) {
                color = Color.decode("#" + input);
            }
        } catch (NumberFormatException ignored) {}

        if (color == null) {
            try {
                Field field = Color.class.getField(input.toUpperCase());
                color = (Color) field.get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalArgumentException("Invalid color: " + input);
            }
        }

        return color;
    }
    private void executeFont(FontCommand cmd) {
        StringBuilder fontString = new StringBuilder();
        fontString.append(cmd.fontName());

        switch (cmd.style()){
            case BOLD -> fontString.append("_bold");
            case ITALIC -> fontString.append("_italic");
            case BOLD_ITALIC -> fontString.append("_bold_italic");
            case OBLIQUE -> fontString.append("_oblique");
            case BOLD_OBLIQUE -> fontString.append("_bold_oblique");
        }

        try {
            currentFont = new PDType1Font(Standard14Fonts.FontName.valueOf(fontString.toString().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Font not found: " + fontString);
        }

        currentColour = parseColor(cmd.colour());
        currentFontSize = cmd.size();

        // TODO: 29.12.25
    }

    private void printTextToCell(CellRef cell, String text) {
        // TODO: 29.12.25
    }

    private void printText(Position pos, String text) throws IOException {
        contentStream.beginText();

        if (pos != null) {
            currentX = pos.x();
            currentY = pos.y();
            contentStream.newLineAtOffset(currentX, currentY);
        } else {
            contentStream.newLineAtOffset(currentX, currentY);
        }

        contentStream.setFont(currentFont, currentFontSize);
        contentStream.setNonStrokingColor(currentColour);

        contentStream.showText(text);
        contentStream.endText();
    }

    private void executePrint(PrintCommand cmd) throws IOException {
        if (document == null || currentPage == null) {
            throw new IOException("print ohne output");
        }

        if (cmd.position() != null && cmd.cell() != null) {
            throw new RuntimeException("position and cell cannot both be specified: " + cmd);
        }

        if (cmd.cell() != null) {
            printTextToCell(cmd.cell(), cmd.text());
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
        if (document  == null) {
            throw new IOException("Document is null");
        }

        addPage();
        updateContentStream();
        currentTable = null;
    }

    private void executeImage(ImageCommand cmd) throws IOException {
        if (document == null || currentPage == null) {
            throw new IOException("Image without Output");
        }
    }

    private void closeContentStream() throws IOException {
        if (contentStream != null) {
            contentStream.close();
        }
    }

    private void updateContentStream() throws IOException {
        closeContentStream();
        contentStream = new PDPageContentStream(document, currentPage);
    }
}