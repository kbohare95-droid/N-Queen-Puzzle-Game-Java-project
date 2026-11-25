/**
 * Immutable representation of a queen placement at a specific (row, col)
 * coordinate on the N x N chessboard.
 */
public class QueenMove {
    private final int row;
    private final int col;

    public QueenMove(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof QueenMove)) {
            return false;
        }
        QueenMove other = (QueenMove) obj;
        return this.row == other.row && this.col == other.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }
}
