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
    private PDType1Font currentFont;
    private int currentFontSize = 12;
    private Color currentColour = Color.BLACK;
    private TableCommand currentTable;

    public void execute(List<Command> commands) throws IOException {
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
    }

    private void executeOutput(OutputCommand cmd) throws IOException {
        File file= new File(cmd.filename());
       try {
           if (file.exists()) {

               if (!file.delete()) {
                   throw new IOException("Delete failed " + file.getAbsolutePath());
               }
           }
           document = new PDDocument();
           PDPage page = new PDPage(PDRectangle.A4);
           document.addPage(page);
       }catch (IOException e) {
           System.err.println(e.getMessage());
           throw e;
       }


    }

    private void executeFont(FontCommand cmd) throws IOException {

    }

    private void executePrint(PrintCommand cmd) throws IOException {
        if (document == null || currentPage == null) {
            throw new IOException("print ohne output");
        }

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