package executor;

import commands.Position;
import commands.Size;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ComponentFactory {
    private final PDAcroForm acroForm;
    private final PDResources resources;

    public ComponentFactory(PDAcroForm acroForm) {
        this.acroForm = acroForm;
        this.resources = new PDResources();
        acroForm.setDefaultResources(resources);
    }

    void drawRect(PDPageContentStream content, Rectangle rect, Color lineColor, Color backgroundColor, Float thickness) throws IOException {
        float DEFAULT_THICKNESS = 1.0f;
        float lineWidth = thickness != null ? thickness : DEFAULT_THICKNESS;

        // Erst Hintergrund füllen (falls vorhanden)
        if (backgroundColor != null) {
            content.setNonStrokingColor(backgroundColor);
            content.addRect(rect.x, rect.y, rect.width, rect.height);
            content.fill();
        }

        // Dann Rahmen zeichnen
        content.setStrokingColor(lineColor);
        content.setLineWidth(lineWidth);
        content.addRect(rect.x, rect.y, rect.width, rect.height);
        content.stroke();
    }

    private String getDefaultAppearance(String fontName, int fontSize, Color color) {
        // DefaultAppearance mit aktueller Schriftart, Größe und Farbe

        return String.format(java.util.Locale.US, "/%s %d Tf %.3f %.3f %.3f rg",
                fontName,
                fontSize,
                color.getRed() / 255.0f,
                color.getGreen() / 255.0f,
                color.getBlue() / 255.0f
        );
    }

    void createTextfield(PDPage page, Position position, Size size, String content, PDFont font, int fontSize, Color color) throws IOException {
        // Font zu den gemeinsamen Resources hinzufügen
        resources.put(COSName.getPDFName(font.getName()), font);

        PDTextField textField = new PDTextField(acroForm);
        textField.setPartialName(UUID.randomUUID().toString());
        textField.setValue(content);
        textField.setDefaultAppearance(getDefaultAppearance(font.getName(), fontSize, color));

        PDAnnotationWidget widget = textField.getWidgets().getFirst();
        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(position.x());
        rect.setLowerLeftY(position.y());
        rect.setUpperRightX(position.x() + size.width());
        rect.setUpperRightY(position.y() + size.height());

        widget.setRectangle(rect);
        widget.setPage(page);

        page.getAnnotations().add(widget);
        acroForm.getFields().add(textField);
    }

    /**
     * Erstellt eine Checkbox an der angegebenen Position.
     * @param maxSize maximale Größe (für Zellen), null für Default
     * @return die Größe der Checkbox (für Positionsupdate)
     */
    float createCheckbox(PDDocument document, PDPage page, Position position, Size maxSize) throws IOException {
        float size = maxSize != null
                ? Math.min(maxSize.width(), maxSize.height())
                : 10;

        PDCheckBox checkBox = new PDCheckBox(acroForm);
        checkBox.setPartialName(UUID.randomUUID().toString());

        PDAnnotationWidget widget = checkBox.getWidgets().getFirst();
        widget.setPrinted(true);
        widget.setRectangle(new PDRectangle(position.x(), position.y(), size, size));
        widget.setPage(page);

        // Appearances selbst generieren für Mac-Kompatibilität
        PDAppearanceDictionary appearanceDict = new PDAppearanceDictionary();

        // Off-State (leere Box)
        PDAppearanceStream offStream = new PDAppearanceStream(document);
        offStream.setResources(new PDResources());
        offStream.setBBox(new PDRectangle(size, size));
        try (PDPageContentStream offContent = new PDPageContentStream(document, offStream)) {
            offContent.setStrokingColor(0f, 0f, 0f);
            offContent.setLineWidth(1);
            offContent.addRect(0.5f, 0.5f, size - 1, size - 1);
            offContent.stroke();
        }
        appearanceDict.setNormalAppearance(offStream);

        // On-State (Box mit Häkchen)
        PDAppearanceStream onStream = new PDAppearanceStream(document);
        onStream.setResources(new PDResources());
        onStream.setBBox(new PDRectangle(size, size));
        try (PDPageContentStream onContent = new PDPageContentStream(document, onStream)) {
            onContent.setStrokingColor(0f, 0f, 0f);
            onContent.setLineWidth(1);
            onContent.addRect(0.5f, 0.5f, size - 1, size - 1);
            onContent.stroke();
            // Häkchen zeichnen
            onContent.setStrokingColor(0f, 0f, 0f);
            onContent.setLineWidth(1.5f);
            onContent.moveTo(2, size / 2);
            onContent.lineTo(size / 2.5f, 2);
            onContent.lineTo(size - 2, size - 2);
            onContent.stroke();
        }

        // Normal appearance mit beiden States
        appearanceDict.getCOSObject().setItem(COSName.N, createAppearanceSubDict(offStream, onStream));

        widget.setAppearance(appearanceDict);
        widget.getCOSObject().setItem(COSName.AS, COSName.OFF);

        page.getAnnotations().add(widget);
        acroForm.getFields().add(checkBox);

        return size;
    }

    /**
     * Erstellt Radio-Buttons.
     * @param maxSize maximale Größe (für Zellen), null für Default. Höhe wird durch Anzahl Optionen geteilt.
     */
    public void createRadio(PDDocument document, PDPage page, Position position, List<String> options, Size maxSize) throws IOException {
        float size = maxSize != null
                ? Math.min(maxSize.width(), (float) maxSize.height() / options.size())
                : 10;

        PDRadioButton radio = new PDRadioButton(acroForm);
        radio.setPartialName(UUID.randomUUID().toString());
        radio.setExportValues(options);

        // Von oben nach unten zeichnen (Y wird kleiner)
        float currentX = position.x();
        float currentY = position.y() - size;  // Erste Position: oben minus eine Größe

        for (String option : options) {
            PDAnnotationWidget widget = new PDAnnotationWidget();
            widget.setPrinted(true);
            widget.setRectangle(new PDRectangle(currentX, currentY, size, size));
            widget.setParent(radio);
            widget.getCOSObject().setItem(COSName.AS, COSName.OFF);

            // Appearances für Radio-Button generieren
            PDAppearanceDictionary appearanceDict = new PDAppearanceDictionary();

            // Off-State (leerer Kreis)
            PDAppearanceStream offStream = new PDAppearanceStream(document);
            offStream.setResources(new PDResources());
            offStream.setBBox(new PDRectangle(size, size));
            try (PDPageContentStream offContent = new PDPageContentStream(document, offStream)) {
                drawCircle(offContent, size / 2, size / 2, (size - 2) / 2);
                offContent.stroke();
            }

            // On-State (Kreis mit Punkt)
            PDAppearanceStream onStream = new PDAppearanceStream(document);
            onStream.setResources(new PDResources());
            onStream.setBBox(new PDRectangle(size, size));
            try (PDPageContentStream onContent = new PDPageContentStream(document, onStream)) {
                drawCircle(onContent, size / 2, size / 2, (size - 2) / 2);
                onContent.stroke();
                onContent.setNonStrokingColor(0f, 0f, 0f);
                drawCircle(onContent, size / 2, size / 2, size / 4);
                onContent.fill();
            }

            // Normal appearance Dictionary mit Off und On States
            COSDictionary nDict = new COSDictionary();
            nDict.setItem(COSName.OFF, offStream);
            nDict.setItem(COSName.getPDFName(option), onStream);
            appearanceDict.getCOSObject().setItem(COSName.N, nDict);

            widget.setAppearance(appearanceDict);
            widget.setPage(page);

            radio.getWidgets().add(widget);
            page.getAnnotations().add(widget);

            // Nächste Position (nach unten)
            currentY -= size;
        }

        acroForm.getFields().add(radio);
    }

    /**
     * Erstellt eine Dropdown-Box an der angegebenen Position.
     */
    public void createDropdown(PDPage page, Position position, Size size,
                               List<String> options, String defaultAppearance) throws IOException {
        PDComboBox comboBox = new PDComboBox(acroForm);
        comboBox.setPartialName(UUID.randomUUID().toString());
        comboBox.setDefaultAppearance(defaultAppearance);
        comboBox.setOptions(options);

        PDAnnotationWidget widget = comboBox.getWidgets().getFirst();
        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(position.x());
        rect.setLowerLeftY(position.y());
        rect.setUpperRightX(position.x() + size.width());
        rect.setUpperRightY(position.y() + size.height());

        widget.setRectangle(rect);
        widget.setPage(page);

        page.getAnnotations().add(widget);
        acroForm.getFields().add(comboBox);

    }

    // Hilfsmethode für Checkbox Appearance Dictionary
    private COSDictionary createAppearanceSubDict(PDAppearanceStream offStream, PDAppearanceStream onStream) {
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.OFF, offStream);
        dict.setItem(COSName.getPDFName("Yes"), onStream);
        return dict;
    }

    // Hilfsmethode um einen Kreis zu zeichnen (Bezier-Approximation)
    private void drawCircle(PDPageContentStream cs, float cx, float cy, float r) throws IOException {
        float k = 0.5523f;
        cs.moveTo(cx + r, cy);
        cs.curveTo(cx + r, cy + r * k, cx + r * k, cy + r, cx, cy + r);
        cs.curveTo(cx - r * k, cy + r, cx - r, cy + r * k, cx - r, cy);
        cs.curveTo(cx - r, cy - r * k, cx - r * k, cy - r, cx, cy - r);
        cs.curveTo(cx + r * k, cy - r, cx + r, cy - r * k, cx + r, cy);
        cs.closePath();
    }
}

