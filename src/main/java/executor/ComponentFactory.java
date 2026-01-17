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
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
    private static final int DEFAULT_RADIO_SIZE = 10;
    private static final int DEFAULT_CHECKBOX_SIZE = 10;
    private static final float CELL_PADDING = 2f;
    private static final float LABEL_PADDING = 3f;
    private final PDAcroForm acroForm;
    private final PDResources resources;

    public ComponentFactory(PDAcroForm acroForm) {
        this.acroForm = acroForm;
        this.resources = new PDResources();
        acroForm.setDefaultResources(resources);
    }

    /**
     * Registriert einen Font in den gemeinsamen Resources.
     * Muss aufgerufen werden für alle Fonts, die in Default Appearance Strings verwendet werden.
     */
    public void registerFont(PDFont font) {
        resources.put(COSName.getPDFName(font.getName()), font);
    }

    void drawRect(
            PDPageContentStream content,
            Rectangle rect,
            Color lineColor,
            Color backgroundColor,
            Float thickness
    ) throws IOException {
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

    void createTextfield(
            PDPage page,
            Position position,
            Size size,
            String content,
            PDFont font,
            int fontSize,
            Color color,
            boolean readOnly
    ) throws IOException {
        // Font zu den gemeinsamen Resources hinzufügen
        resources.put(COSName.getPDFName(font.getName()), font);
        PDTextField textField = new PDTextField(acroForm);
        textField.setPartialName(UUID.randomUUID().toString());
        textField.setValue(content);
        textField.setReadOnly(readOnly);
        textField.setDefaultAppearance(getDefaultAppearance(font.getName(), fontSize, color));

        PDAnnotationWidget widget = textField.getWidgets().getFirst();
        PDRectangle rect = new PDRectangle(position.x(), position.y(), size.width(), size.height());

        widget.setRectangle(rect);
        widget.setPage(page);

        page.getAnnotations().add(widget);
        acroForm.getFields().add(textField);
    }

    /**
     * Erstellt eine Dropdown-Box an der angegebenen Position.
     */
    public void createDropdown(
            PDPage page,
            Position position,
            Size size,
            List<String> options,
            PDFont font,
            int fontSize,
            Color color
    ) throws IOException {
        // Font zu den gemeinsamen Resources hinzufügen
        resources.put(COSName.getPDFName(font.getName()), font);

        PDComboBox comboBox = new PDComboBox(acroForm);
        comboBox.setPartialName(UUID.randomUUID().toString());
        comboBox.setDefaultAppearance(getDefaultAppearance(font.getName(), fontSize, color));
        comboBox.setDefaultValue(options.getFirst());
        comboBox.setOptions(options);

        float boxX = position.x() + CELL_PADDING;
        float boxY = position.y() + CELL_PADDING;
        float width = size.width() - CELL_PADDING * 2;
        float height = size.height() - CELL_PADDING * 2;

        PDAnnotationWidget widget = comboBox.getWidgets().getFirst();
        widget.setRectangle(new PDRectangle(boxX, boxY, width, height));
        widget.setPage(page);
        page.getAnnotations().add(widget);

        acroForm.getFields().add(comboBox);
    }

    // Hilfsfunktion für Control-Größe (Checkbox/Radio)
    private float calculateControlSize(int fontSize, int defaultSize, Size maxSize) {
        float size = Math.min(fontSize, defaultSize);
        if (maxSize != null) {
            size = Math.min(size, maxSize.height());
        }
        return size;
    }

    // Hilfsfunktion für Textfeld-Breite neben Controls
    private float calculateLabelWidth(float controlSize, float padding, Size maxSize) {
        return maxSize != null ? maxSize.width() - controlSize - padding : 100;
    }

    void createCheckbox(
            PDDocument document,
            PDPage page,
            Position position,
            Size maxSize,
            String label,
            PDFont font,
            int fontSize,
            Color color
    ) throws IOException {

        float size = calculateControlSize(fontSize, DEFAULT_CHECKBOX_SIZE, maxSize);

        // Position: links oben mit Padding
        float boxX = position.x() + CELL_PADDING;
        float boxY = maxSize != null
                ? position.y() + maxSize.height() - size - CELL_PADDING
                : position.y();

        // Font zu den gemeinsamen Resources hinzufügen
        resources.put(COSName.getPDFName(font.getName()), font);
        PDCheckBox checkBox = new PDCheckBox(acroForm);
        checkBox.setPartialName(UUID.randomUUID().toString());

        PDAnnotationWidget widget = checkBox.getWidgets().getFirst();
        widget.setPrinted(true);
        widget.setRectangle(new PDRectangle(boxX, boxY, size, size));
        widget.setPage(page);

        PDAppearanceStream onState = createCheckboxOnAppearance(document, widget.getRectangle());
        PDAppearanceStream offState = createCheckboxOffAppearance(document, widget.getRectangle());
        widget.setAppearance(createAppearanceSubDict(offState, onState, label));
        widget.getCOSObject().setItem(COSName.AS, COSName.OFF);

        page.getAnnotations().add(widget);
        acroForm.getFields().add(checkBox);

        // Label rechts neben Checkbox
        float textX = boxX + size + LABEL_PADDING;
        float textWidth = calculateLabelWidth(size, LABEL_PADDING + CELL_PADDING * 2, maxSize);
        createTextfield(page, new Position(textX, boxY), new Size((int) textWidth, (int) size), label, font, fontSize, color, true);
    }

    public void createRadio(
            PDDocument document,
            PDPage page,
            Position position,
            List<String> options,
            Size maxSize,
            PDFont font,
            int fontSize,
            Color color
    ) throws IOException {

        float sizePerOption = maxSize != null ? maxSize.height() / options.size() : fontSize;
        float size = calculateControlSize(fontSize, DEFAULT_RADIO_SIZE, new Size(0, (int) sizePerOption));

        // Font zu den gemeinsamen Resources hinzufügen
        resources.put(COSName.getPDFName(font.getName()), font);
        PDRadioButton radio = new PDRadioButton(acroForm);
        radio.setPartialName(UUID.randomUUID().toString());
        radio.setExportValues(options);

        // Start: links oben mit Padding
        float startX = position.x() + CELL_PADDING;
        float currentY = maxSize != null
                ? position.y() + maxSize.height() - size - CELL_PADDING
                : position.y();
        float textWidth = calculateLabelWidth(size, LABEL_PADDING + CELL_PADDING * 2, maxSize);

        for (String option : options) {
            PDAnnotationWidget widget = new PDAnnotationWidget();
            widget.setPrinted(true);
            widget.setRectangle(new PDRectangle(startX, currentY, size, size));
            widget.setParent(radio);
            widget.getCOSObject().setItem(COSName.AS, COSName.OFF);

            PDAppearanceStream onState = createRadioOnAppearance(document, widget.getRectangle());
            PDAppearanceStream offState = createRadioOffAppearance(document, widget.getRectangle());
            widget.setAppearance(createAppearanceSubDict(offState, onState, option));
            widget.setPage(page);

            radio.getWidgets().add(widget);
            page.getAnnotations().add(widget);

            // Label rechts neben Radio
            float textX = startX + size + LABEL_PADDING;
            createTextfield(page, new Position(textX, currentY), new Size((int) textWidth, (int) size), option, font, fontSize, color, true);

            currentY -= size;
        }

        acroForm.getFields().add(radio);
    }
    private PDAppearanceStream createCheckboxOffAppearance(PDDocument document, PDRectangle rectangle) throws IOException {
        PDAppearanceStream offStream = new PDAppearanceStream(document);

        offStream.setResources(new PDResources());
        offStream.setBBox(new PDRectangle(rectangle.getWidth(), rectangle.getHeight()));

        try (PDPageContentStream content = new PDPageContentStream(document, offStream)) {
            content.setStrokingColor(0f, 0f, 0f);
            content.setLineWidth(1);
            content.addRect(0, 0, rectangle.getWidth(), rectangle.getHeight());
            content.stroke();
        }

        return offStream;
    }

    private PDAppearanceStream createCheckboxOnAppearance(PDDocument document, PDRectangle rectangle) throws IOException {
        PDAppearanceStream onStream = new PDAppearanceStream(document);

        onStream.setResources(new PDResources());
        onStream.setBBox(new PDRectangle(rectangle.getWidth(), rectangle.getHeight()));

        try (PDPageContentStream content = new PDPageContentStream(document, onStream)) {
            content.setStrokingColor(0f, 0f, 0f);
            content.setLineWidth(1);
            content.addRect(0, 0, rectangle.getWidth(), rectangle.getHeight());
            content.moveTo(0, 0);
            content.lineTo(rectangle.getWidth(), rectangle.getHeight());
            content.moveTo(0, rectangle.getHeight());
            content.lineTo(rectangle.getWidth(), 0);
            content.stroke();
        }

        return onStream;
    }

    private PDAppearanceDictionary createAppearanceSubDict(PDAppearanceStream offStream, PDAppearanceStream onStream, String name) {
        PDAppearanceDictionary dict = new PDAppearanceDictionary();

        // Direkt ein COSDictionary für die Normal-Appearances bauen
        COSDictionary normalDict = new COSDictionary();
        normalDict.setItem(COSName.Off, offStream);
        normalDict.setItem(COSName.getPDFName(name), onStream);

        // Als Normal-Appearance setzen
        dict.getCOSObject().setItem(COSName.N, normalDict);

        return dict;
    }

    private PDAppearanceStream createRadioOnAppearance(PDDocument document, PDRectangle rect) throws IOException {
        PDAppearanceStream stream = new PDAppearanceStream(document);
        stream.setBBox(new PDRectangle(rect.getWidth(), rect.getHeight()));

        try (PDPageContentStream cs = new PDPageContentStream(document, stream)) {
            // Kreis zeichnen (Rahmen)
            float r = Math.min(rect.getWidth(), rect.getHeight()) / 2 - 1;
            drawCircle(cs, rect.getWidth()/2, rect.getHeight()/2, r);
            cs.stroke();

            // Gefüllter innerer Kreis für "selected"
            drawCircle(cs, rect.getWidth()/2, rect.getHeight()/2, r * 0.5f);
            cs.fill();
        }
        return stream;
    }

    private void drawCircle(PDPageContentStream cs, float cx, float cy, float r) throws IOException {
        // Magische Zahl für Bézier-Approximation eines Kreises
        float k = 0.552284749831f * r;

        cs.moveTo(cx + r, cy);
        cs.curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r);      // Quadrant oben rechts
        cs.curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy);      // Quadrant oben links
        cs.curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r);      // Quadrant unten links
        cs.curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy);      // Quadrant unten rechts
        cs.closePath();
    }

    private PDAppearanceStream createRadioOffAppearance(PDDocument document, PDRectangle rect) throws IOException {
        PDAppearanceStream stream = new PDAppearanceStream(document);
        stream.setBBox(new PDRectangle(rect.getWidth(), rect.getHeight()));

        try (PDPageContentStream cs = new PDPageContentStream(document, stream)) {
            float r = Math.min(rect.getWidth(), rect.getHeight()) / 2 - 1;
            drawCircle(cs, rect.getWidth()/2, rect.getHeight()/2, r);
            cs.stroke();
        }
        return stream;
    }
}

