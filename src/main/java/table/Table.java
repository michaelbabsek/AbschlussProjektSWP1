package table;

import commands.CellRef;
import commands.Position;
import commands.Size;

import java.util.List;

public class Table {
    private Position position;
    private Size size;
    private final int cols;
    private final int rows;
    private Cell[][] cells;

    public Table(Position position, List<Integer> columnWidths, List<Integer> rowHeights, int cols, int rows) {
        this.position = position;
        this.size = getSize(columnWidths, rowHeights);
        this.cols = cols;
        this.rows = rows;
        initCells(columnWidths, rowHeights);
    }

    public Cell getCell(CellRef cellRef) {
        return cells[cellRef.col()][cellRef.row()];
    }
    private Size getSize(List<Integer> columnWidths, List<Integer> rowHeights) {
        int width = columnWidths.stream().mapToInt(Integer::intValue).sum();
        int height = rowHeights.stream().mapToInt(Integer::intValue).sum();
        return new Size(width, height);
    }
    private void initCells(List<Integer> columnWidths, List<Integer> rowHeights) {
        this.cells = new Cell[cols][rows];

        float startX = position.x();
        for (int i = 0; i < cols; i++) {
            int cellWidth = columnWidths.get(i);
            float currentY = position.y();

            for (int j = 0; j < rows; j++) {
                int cellHeight = rowHeights.get(j);
                currentY -= cellHeight;  // Y nach unten verschieben (PDF: Y geht nach oben)
                cells[i][j] = new Cell(new Position(startX, currentY), new Size(cellWidth, cellHeight));
            }
            startX += cellWidth;  // Nächste Spalte nach rechts
        }
    }
}