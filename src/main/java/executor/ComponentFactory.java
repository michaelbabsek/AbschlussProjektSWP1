package executor;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

import java.awt.*;
import java.io.IOException;
import java.util.UUID;

public class ComponentFactory {

    public ComponentFactory() {}

    void drawRect(PDPageContentStream content, Color color, Rectangle rect, boolean fill) throws IOException {
        content.addRect(rect.x, rect.y, rect.width, rect.height);
        if (fill) {
            content.setNonStrokingColor(color);
            content.fill();
        } else {
            content.setStrokingColor(color);
            content.stroke();
        }
    }

    void createTextfield(PDAcroForm acroForm, PDPage page, float x, float y, float width, float height,
                         String content, PDFont font, int fontSize, Color color) throws IOException {
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName(font.getName()), font);
        acroForm.setDefaultResources(resources);

        String defaultAppearance = String.format(java.util.Locale.US, "/%s %d Tf %.3f %.3f %.3f rg",
                font.getName(), fontSize,
                color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f);
        acroForm.setDefaultAppearance(defaultAppearance);

        PDTextField textField = new PDTextField(acroForm);
        textField.setPartialName(UUID.randomUUID().toString());
        textField.setValue(content);
        textField.setDefaultAppearance(defaultAppearance);

        PDAnnotationWidget widget = textField.getWidgets().getFirst();
        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(x);
        rect.setLowerLeftY(y);
        rect.setUpperRightX(x + width);
        rect.setUpperRightY(y + height);

        widget.setRectangle(rect);
        widget.setPage(page);

        page.getAnnotations().add(widget);
        acroForm.getFields().add(textField);
    }
}
