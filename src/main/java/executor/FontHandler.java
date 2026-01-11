package executor;

import commands.FontStyle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class FontHandler {

    private static final String REPLACEMENT = "[?]";

    private FontHandler() {}

    /**
     * Löst Font-Name zu einem PDFont auf.
     * Unterstützt nur Standard-14 PDF-Fonts: Helvetica, Times, Courier, Symbol, ZapfDingbats
     */
    public static PDFont resolve(String name, FontStyle style) {
        String normalized = normalize(name);

        return switch (normalized) {
            case "HELVETICA" -> getHelvetica(style);
            case "TIMES", "TIMES_ROMAN" -> getTimes(style);
            case "COURIER" -> getCourier(style);
            case "SYMBOL" -> new PDType1Font(Standard14Fonts.FontName.SYMBOL);
            case "ZAPF_DINGBATS", "ZAPFDINGBATS" -> new PDType1Font(Standard14Fonts.FontName.ZAPF_DINGBATS);
            default -> throw new IllegalArgumentException("Unbekannter Font: " + name +
                    ". Verfügbar: helvetica, times, courier, symbol, zapf_dingbats");
        };
    }

    private static PDType1Font getHelvetica(FontStyle style) {
        if (style == null) {
            return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
        return new PDType1Font(switch (style) {
            case BOLD -> Standard14Fonts.FontName.HELVETICA_BOLD;
            case ITALIC, OBLIQUE -> Standard14Fonts.FontName.HELVETICA_OBLIQUE;
            case BOLD_ITALIC, BOLD_OBLIQUE -> Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE;
            default -> Standard14Fonts.FontName.HELVETICA;
        });
    }

    private static PDType1Font getTimes(FontStyle style) {
        if (style == null) {
            return new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
        }
        return new PDType1Font(switch (style) {
            case BOLD -> Standard14Fonts.FontName.TIMES_BOLD;
            case ITALIC, OBLIQUE -> Standard14Fonts.FontName.TIMES_ITALIC;
            case BOLD_ITALIC, BOLD_OBLIQUE -> Standard14Fonts.FontName.TIMES_BOLD_ITALIC;
            default -> Standard14Fonts.FontName.TIMES_ROMAN;
        });
    }

    private static PDType1Font getCourier(FontStyle style) {
        if (style == null) {
            return new PDType1Font(Standard14Fonts.FontName.COURIER);
        }
        return new PDType1Font(switch (style) {
            case BOLD -> Standard14Fonts.FontName.COURIER_BOLD;
            case ITALIC, OBLIQUE -> Standard14Fonts.FontName.COURIER_OBLIQUE;
            case BOLD_ITALIC, BOLD_OBLIQUE -> Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE;
            default -> Standard14Fonts.FontName.COURIER;
        });
    }

    /**
     * Gibt den ZapfDingbats Font zurück (für Symbole wie Häkchen, Kreise)
     */
    public static PDType1Font getZapfDingbats() {
        return new PDType1Font(Standard14Fonts.FontName.ZAPF_DINGBATS);
    }

    /**
     * Ersetzt nicht darstellbare Zeichen mit [?]
     */
    public static String sanitize(String text, PDFont font) {
        if (text == null || text.isEmpty()) return text;

        StringBuilder result = new StringBuilder();
        List<Character> replaced = new ArrayList<>();

        for (char c : text.toCharArray()) {
            if (canEncode(font, c)) {
                result.append(c);
            } else {
                result.append(REPLACEMENT);
                replaced.add(c);
            }
        }

        if (!replaced.isEmpty()) {
            System.err.printf("[FontResolver] %d Zeichen nicht darstellbar: %s%n",
                    replaced.size(), formatReplacedChars(replaced));
        }

        return result.toString();
    }

    private static boolean canEncode(PDFont font, char c) {
        try {
            font.encode(String.valueOf(c));
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    private static String normalize(String name) {
        return name.toUpperCase().replace("-", "_").replace(" ", "_");
    }

    private static String formatReplacedChars(List<Character> chars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(chars.size(), 5); i++) {
            if (i > 0) sb.append(", ");
            char c = chars.get(i);
            sb.append(String.format("'%c' (U+%04X)", c, (int) c));
        }
        if (chars.size() > 5) {
            sb.append(String.format(" ... und %d weitere", chars.size() - 5));
        }
        return sb.toString();
    }
}