package executor;

import commands.FontStyle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class FontResolver {

    private static final String REPLACEMENT = "[?]";

    // Standard-14 Fonts
    private static final Map<String, Map<FontStyle, Standard14Fonts.FontName>> STANDARD_FONTS = new HashMap<>();

    // Cache für geladene TTF-Fonts (pro Dokument)
    private static final Map<String, PDFont> loadedFonts = new HashMap<>();
    private static PDDocument currentDocument = null;

    // System Font-Verzeichnisse
    private static final String[] FONT_DIRS = {
            // Windows
            "C:/Windows/Fonts",
            // macOS
            "/Library/Fonts",
            "/System/Library/Fonts",
            System.getProperty("user.home") + "/Library/Fonts",
            // Linux
            "/usr/share/fonts",
            "/usr/local/share/fonts",
            System.getProperty("user.home") + "/.fonts"
    };

    static {
        // Helvetica Familie
        Map<FontStyle, Standard14Fonts.FontName> helvetica = new EnumMap<>(FontStyle.class);
        helvetica.put(FontStyle.REGULAR, Standard14Fonts.FontName.HELVETICA);
        helvetica.put(FontStyle.BOLD, Standard14Fonts.FontName.HELVETICA_BOLD);
        helvetica.put(FontStyle.OBLIQUE, Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        helvetica.put(FontStyle.ITALIC, Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        helvetica.put(FontStyle.BOLD_OBLIQUE, Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
        helvetica.put(FontStyle.BOLD_ITALIC, Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
        STANDARD_FONTS.put("HELVETICA", helvetica);

        // Times Familie
        Map<FontStyle, Standard14Fonts.FontName> times = new EnumMap<>(FontStyle.class);
        times.put(FontStyle.REGULAR, Standard14Fonts.FontName.TIMES_ROMAN);
        times.put(FontStyle.BOLD, Standard14Fonts.FontName.TIMES_BOLD);
        times.put(FontStyle.ITALIC, Standard14Fonts.FontName.TIMES_ITALIC);
        times.put(FontStyle.OBLIQUE, Standard14Fonts.FontName.TIMES_ITALIC);
        times.put(FontStyle.BOLD_ITALIC, Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
        times.put(FontStyle.BOLD_OBLIQUE, Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
        STANDARD_FONTS.put("TIMES", times);
        STANDARD_FONTS.put("TIMES_ROMAN", times);

        // Courier Familie
        Map<FontStyle, Standard14Fonts.FontName> courier = new EnumMap<>(FontStyle.class);
        courier.put(FontStyle.REGULAR, Standard14Fonts.FontName.COURIER);
        courier.put(FontStyle.BOLD, Standard14Fonts.FontName.COURIER_BOLD);
        courier.put(FontStyle.OBLIQUE, Standard14Fonts.FontName.COURIER_OBLIQUE);
        courier.put(FontStyle.ITALIC, Standard14Fonts.FontName.COURIER_OBLIQUE);
        courier.put(FontStyle.BOLD_OBLIQUE, Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE);
        courier.put(FontStyle.BOLD_ITALIC, Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE);
        STANDARD_FONTS.put("COURIER", courier);

        // Symbol & ZapfDingbats
        Map<FontStyle, Standard14Fonts.FontName> symbol = new EnumMap<>(FontStyle.class);
        symbol.put(FontStyle.REGULAR, Standard14Fonts.FontName.SYMBOL);
        STANDARD_FONTS.put("SYMBOL", symbol);

        Map<FontStyle, Standard14Fonts.FontName> zapf = new EnumMap<>(FontStyle.class);
        zapf.put(FontStyle.REGULAR, Standard14Fonts.FontName.ZAPF_DINGBATS);
        STANDARD_FONTS.put("ZAPF_DINGBATS", zapf);
        STANDARD_FONTS.put("ZAPFDINGBATS", zapf);
    }

    private FontResolver() {}

    /**
     * Setzt das aktuelle Dokument für TTF-Font-Loading.
     * Muss am Anfang jeder PDF-Erstellung aufgerufen werden!
     */
    public static void setDocument(PDDocument document) {
        if (currentDocument != document) {
            currentDocument = document;
            loadedFonts.clear();  // Cache leeren bei neuem Dokument
        }
    }

    /**
     * Löst Font-Name zu einem PDFont auf.
     * Prüft zuerst Standard-14, dann System-Fonts.
     */
    public static PDFont resolve(String name, FontStyle style) {
        String key = normalize(name);

        // 1. Prüfe Standard-14 Fonts
        if (STANDARD_FONTS.containsKey(key)) {
            return resolveStandard14(key, style);
        }

        // 2. Versuche System-Font zu laden
        return resolveSystemFont(name, style);
    }

    private static PDType1Font resolveStandard14(String key, FontStyle style) {
        Map<FontStyle, Standard14Fonts.FontName> family = STANDARD_FONTS.get(key);
        Standard14Fonts.FontName fontName = family.get(style);
        if (fontName == null) {
            fontName = family.get(FontStyle.REGULAR);
        }
        return new PDType1Font(fontName);
    }

    private static PDFont resolveSystemFont(String name, FontStyle style) {
        if (currentDocument == null) {
            throw new IllegalStateException("Document nicht gesetzt! Rufe FontResolver.setDocument() auf.");
        }

        // Cache-Key mit Style
        String cacheKey = name + "_" + style;
        if (loadedFonts.containsKey(cacheKey)) {
            return loadedFonts.get(cacheKey);
        }

        // Font-Datei suchen
        File fontFile = findFontFile(name, style);
        if (fontFile == null) {
            throw new IllegalArgumentException("Font nicht gefunden: " + name +
                    ". Verfügbare System-Fonts: " + getAvailableFonts());
        }

        try {
            PDFont font = PDType0Font.load(currentDocument, fontFile);
            loadedFonts.put(cacheKey, font);
            System.out.println("[FontResolver] Geladen: " + fontFile.getAbsolutePath());
            return font;
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Laden von Font: " + fontFile, e);
        }
    }

    private static File findFontFile(String name, FontStyle style) {
        // Mögliche Dateinamen generieren
        List<String> candidates = new ArrayList<>();
        String baseName = name.replace(" ", "");

        // Style-Suffixe
        String styleSuffix = switch (style) {
            case BOLD -> "-Bold";
            case ITALIC, OBLIQUE -> "-Italic";
            case BOLD_ITALIC, BOLD_OBLIQUE -> "-BoldItalic";
            default -> "-Regular";
        };

        candidates.add(baseName + styleSuffix + ".ttf");
        candidates.add(baseName + styleSuffix + ".otf");
        candidates.add(baseName + ".ttf");
        candidates.add(baseName + ".otf");
        candidates.add(name + ".ttf");
        candidates.add(name + ".otf");

        // Lowercase Varianten
        candidates.add(baseName.toLowerCase() + styleSuffix.toLowerCase() + ".ttf");
        candidates.add(baseName.toLowerCase() + ".ttf");

        // In allen Font-Verzeichnissen suchen
        for (String dir : FONT_DIRS) {
            File fontDir = new File(dir);
            if (fontDir.exists()) {
                File found = searchInDirectory(fontDir, candidates);
                if (found != null) return found;
            }
        }

        return null;
    }

    private static File searchInDirectory(File dir, List<String> candidates) {
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isDirectory()) {
                File found = searchInDirectory(file, candidates);
                if (found != null) return found;
            } else {
                for (String candidate : candidates) {
                    if (file.getName().equalsIgnoreCase(candidate)) {
                        return file;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gibt alle verfügbaren System-Fonts zurück.
     */
    public static List<String> getAvailableFonts() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return Arrays.asList(ge.getAvailableFontFamilyNames());
    }

    /**
     * Prüft ob ein Font verfügbar ist (Standard-14 oder System).
     */
    public static boolean isValidFont(String name) {
        String key = normalize(name);
        if (STANDARD_FONTS.containsKey(key)) return true;

        // Prüfe ob System-Font existiert
        return getAvailableFonts().stream()
                .anyMatch(f -> f.equalsIgnoreCase(name));
    }

    /**
     * Ersetzt nicht darstellbare Zeichen.
     */
    public static String sanitize(String text, PDFont font) {
        if (text == null || text.isEmpty()) return text;

        StringBuilder result = new StringBuilder();
        List<Character> replaced = new ArrayList<>();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (canEncode(font, c)) {
                result.append(c);
            } else {
                result.append(REPLACEMENT);
                replaced.add(c);
            }
        }

        if (!replaced.isEmpty()) {
            System.err.printf("[FontResolver] %d Zeichen nicht darstellbar in '%s': %s%n",
                    replaced.size(), font.getName(), formatReplacedChars(replaced));
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
        for (int i = 0; i < chars.size(); i++) {
            if (i > 0) sb.append(", ");
            char c = chars.get(i);
            sb.append(String.format("'%c' (U+%04X)", c, (int) c));
            if (i >= 4 && chars.size() > 5) {
                sb.append(String.format(" ... und %d weitere", chars.size() - 5));
                break;
            }
        }
        return sb.toString();
    }
}