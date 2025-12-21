import ui.EditorWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set look and feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }

        // Launch the editor window on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            EditorWindow editor = new EditorWindow();
            editor.setVisible(true);
        });
    }
}