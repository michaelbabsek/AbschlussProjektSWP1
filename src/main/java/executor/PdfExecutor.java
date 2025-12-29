package executor;

import commands.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfExecutor {
    private PDDocument document;
    private PDPage currentPage;
    private PDPageContentStream contentStream;

    // Aktueller Zustand
    private File outputFile;
    private float currentX;
    private float currentY;
    private PDType1Font currentFont;
    private int currentFontSize = 12;
    private Color currentColour = Color.BLACK;
    private TableCommand currentTable;

    public void execute(List<Command> commands) throws IOException {
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

        close();
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

    private PDDocument createDocument() {
        document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return document;
    }

    private void saveDocument() throws IOException {
        document.save(outputFile);
    }

    private void executeFont(FontCommand cmd) throws IOException {

    }

    private void executePrint(PrintCommand cmd) throws IOException {
        if (document == null || currentPage == null) {
            throw new IOException("print ohne output");
        }
        float startX;
        float startY;

        if (cmd.cell() != null){
            if (currentTable == null) {
                throw new IOException("print @ cell without table)");
            }
            //hier soll rein in welche cell der currenttable geschrieben werden soll
         //   startX= CellRef();
           // startY= pos.y();

        }else if (cmd.position() != null) {
            startX = cmd.position().x();
            startY = cmd.position().y();
        } else {
            startX = currentX;
            startY = currentY;
        }

        Alignment alignment = cmd.alignment() != null
                ? cmd.alignment()
                : Alignment.LEFT;

        float offsetX = switch (alignment) {
            case LEFT -> 0;
            case CENTER -> -cmd.width() / 2f;
            case RIGHT -> -cmd.width() ;
        };

        float maxWidth = cmd.width() != null
                ? cmd.width()
                : Float.MAX_VALUE;

    }

    private void executeTable(TableCommand cmd) throws IOException {
        // TODO
    }

    private void executeControl(ControlCommand cmd) throws IOException {
        // TODO
    }

    private void executeNextPage() throws IOException {
        newPage();
    }

    private void executeImage(ImageCommand cmd) throws IOException {
        if (document == null || currentPage == null) {
            throw new IOException("Image without Output");
        }


    }

    private void newPage() throws IOException {
        if (document  == null) {
            throw new IOException("Document is null");

        }
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        currentPage = page;
        contentStream = new PDPageContentStream(document, currentPage);

        //Tabelle wird vergessen ka was er meint aber ist so in der PDF
        currentTable= null;
    }

    private void close() throws IOException {
        if (contentStream != null) {
            contentStream.close();
        }
    }

    public void save(String filename) throws IOException {
        document.save(new File(filename));
        document.close();
    }
}