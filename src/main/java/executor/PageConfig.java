package executor;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

public record PageConfig(
        float marginX,
        float marginY,
        PDRectangle pageSize
) {
    public static final PageConfig DEFAULT = new PageConfig(32f, 32f, PDRectangle.A4);

    public float textWidth() {
        return pageSize.getWidth() - 2 * marginX;
    }

    public float textHeight() {
        return pageSize.getHeight() - 2 * marginY;
    }

    public float startX() {
        return marginX;
    }

    public float startY() {
        return pageSize.getHeight() - marginY;
    }

    public float minY() {
        return marginY;
    }
}