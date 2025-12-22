package ui;

import commands.Command;
import commands.Parser;
import tokens.Token;
import tokens.Tokenizer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.lang.reflect.Parameter;
import java.text.ParseException;
import java.util.List;

public class EditorWindow extends JFrame {
    private final JTextArea codeEditor;
    private final JTextArea lineNumbers;

    public EditorWindow() {
        setTitle("PDFScript Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(30, 30, 30));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Terminal-style code editor
        codeEditor = new JTextArea();
        codeEditor.setBackground(new Color(20, 20, 20));
        codeEditor.setForeground(new Color(255, 0, 100)); // red text like terminal
        codeEditor.setCaretColor(new Color(0, 255, 0));
        codeEditor.setFont(new Font("Monospaced", Font.PLAIN, 14));
        codeEditor.setBorder(new EmptyBorder(10, 10, 10, 10));
        codeEditor.setTabSize(4);

        // Line numbers panel
        lineNumbers = new JTextArea("1");
        lineNumbers.setBackground(new Color(30, 30, 30));
        lineNumbers.setForeground(new Color(100, 100, 100));
        lineNumbers.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lineNumbers.setBorder(new EmptyBorder(10, 10, 10, 5));
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);

        // Update line numbers when text changes
        codeEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateLineNumbers(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateLineNumbers(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateLineNumbers(); }
        });

        // Add placeholder text
        codeEditor.setText("print @cell 50, 50 \"Hello\"");

        // Scroll pane for the editor
        JScrollPane scrollPane = new JScrollPane(codeEditor);
        scrollPane.setRowHeaderView(lineNumbers);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 2));
        scrollPane.getVerticalScrollBar().setBackground(new Color(40, 40, 40));
        scrollPane.getHorizontalScrollBar().setBackground(new Color(40, 40, 40));

        // Bottom panel with button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(new Color(30, 30, 30));
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton createPdfButton = new JButton("Create PDF");
        createPdfButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        createPdfButton.setBackground(new Color(0, 120, 200));
        createPdfButton.setForeground(Color.WHITE);
        createPdfButton.setFocusPainted(false);
        createPdfButton.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        createPdfButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Button hover effect
        createPdfButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                createPdfButton.setBackground(new Color(0, 150, 230));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                createPdfButton.setBackground(new Color(0, 120, 200));
            }
        });

        // Button action
        createPdfButton.addActionListener(e -> onCreatePdf());

        bottomPanel.add(createPdfButton);

        // Add components to main panel
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void onCreatePdf() {
        String code = codeEditor.getText();
        // TODO: Parse markup language and generate PDF
        JOptionPane.showMessageDialog(this,
                "PDF creation will be implemented with your markup language.\n\nCode to process:\n" +
                (code.length() > 200 ? code.substring(0, 200) + "..." : code),
                "Create PDF",
                JOptionPane.INFORMATION_MESSAGE);

        Tokenizer tokenizer = new Tokenizer(code);
        List<Token> tokens = tokenizer.tokenize();
        tokens.forEach(System.out::println);
        Parser parser = new Parser(tokens);
        try {
            List<Command> commands = parser.parse();
            commands.forEach(System.out::println);
        } catch (ParseException e){
            System.out.println("failed parsing");
        }

    }

    public String getCode() {
        return codeEditor.getText();
    }

    public void setCode(String code) {
        codeEditor.setText(code);
    }

    private void updateLineNumbers() {
        String text = codeEditor.getText();
        int lineCount = text.isEmpty() ? 1 : text.split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            sb.append(i);
            if (i < lineCount) {
                sb.append("\n");
            }
        }
        lineNumbers.setText(sb.toString());
    }
}