import java.util.Arrays;

/**
 * 2D Array representation of the N x N chessboard for the N-Queen Puzzle Game.
 *
 * Uses a char[n][n] grid where:
 * - 'Q' represents a placed Queen
 * - '.' represents an empty square
 */
public class Board {
    public static final char QUEEN = 'Q';
    public static final char EMPTY = '.';

    private final int n;
    private final char[][] grid;
    private int queenCount;

    public Board(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Board size N must be at least 1.");
        }
        this.n = n;
        this.grid = new char[n][n];
        clear();
    }

    /**
     * Deep copy constructor for snapshotting solutions and running hint searches
     * without mutating the active game board.
     */
    public Board(Board other) {
        this.n = other.n;
        this.queenCount = other.queenCount;
        this.grid = new char[n][n];
        for (int r = 0; r < n; r++) {
            this.grid[r] = Arrays.copyOf(other.grid[r], n);
        }
    }

    public int getSize() {
        return n;
    }

    public int getQueenCount() {
        return queenCount;
    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < n && col >= 0 && col < n;
    }

    public boolean hasQueen(int row, int col) {
        return isInBounds(row, col) && grid[row][col] == QUEEN;
    }

    public boolean rowHasQueen(int row) {
        if (row < 0 || row >= n) {
            return false;
        }
        for (int c = 0; c < n; c++) {
            if (grid[row][c] == QUEEN) {
                return true;
            }
        }
        return false;
    }

    public boolean placeQueen(int row, int col) {
        if (!isInBounds(row, col) || grid[row][col] == QUEEN) {
            return false;
        }
        grid[row][col] = QUEEN;
        queenCount++;
        return true;
    }

    public boolean removeQueen(int row, int col) {
        if (!isInBounds(row, col) || grid[row][col] != QUEEN) {
            return false;
        }
        grid[row][col] = EMPTY;
        queenCount--;
        return true;
    }

    public void clear() {
        for (int r = 0; r < n; r++) {
            Arrays.fill(grid[r], EMPTY);
        }
        queenCount = 0;
    }

    /**
     * Checks whether placing a queen at (row, col) is safe from all existing queens
     * across the same row, same column, and both diagonals.
     *
     * Time Complexity: O(N)
     */
    public boolean isSafe(int row, int col) {
        if (!isInBounds(row, col)) {
            return false;
        }
        return findConflictingQueen(row, col) == null;
    }

    /**
     * Returns the coordinates of the first queen attacking (row, col), or null if safe.
     */
    public QueenMove findConflictingQueen(int row, int col) {
        if (!isInBounds(row, col)) {
            return null;
        }

        // Check same row and same column
        for (int i = 0; i < n; i++) {
            if (i != col && grid[row][i] == QUEEN) {
                return new QueenMove(row, i);
            }
            if (i != row && grid[i][col] == QUEEN) {
                return new QueenMove(i, col);
            }
        }

        // Check all four diagonal directions
        int[][] directions = { {-1, -1}, {-1, 1}, {1, -1}, {1, 1} };
        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];
            while (isInBounds(r, c)) {
                if (grid[r][c] == QUEEN) {
                    return new QueenMove(r, c);
                }
                r += dir[0];
                c += dir[1];
            }
        }

        return null;
    }

    /**
     * Returns a human-readable conflict description if (row, col) is attacked by
     * another queen on the board, or null if the square is completely safe.
     */
    public String getConflictReason(int row, int col) {
        if (!isInBounds(row, col)) {
            return "Coordinates (" + row + ", " + col + ") are outside the " + n + "x" + n + " board.";
        }
        QueenMove attacker = findConflictingQueen(row, col);
        if (attacker == null) {
            return null;
        }
        if (attacker.getRow() == row) {
            return "Conflicts with queen at " + attacker + " on the same row.";
        }
        if (attacker.getCol() == col) {
            return "Conflicts with queen at " + attacker + " on the same column.";
        }
        return "Conflicts with queen at " + attacker + " on the same diagonal.";
    }

    /**
     * Checks whether any queen currently placed on the 2D board attacks another queen.
     */
    public boolean hasAnyConflict() {
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (grid[r][c] == QUEEN && findConflictingQueen(r, c) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true when N queens are placed on the board with zero conflicts.
     */
    public boolean isSolved() {
        return queenCount == n && !hasAnyConflict();
    }

    /**
     * Returns a defensive copy of the underlying 2D char array.
     */
    public char[][] getGridCopy() {
        char[][] copy = new char[n][n];
        for (int r = 0; r < n; r++) {
            copy[r] = Arrays.copyOf(grid[r], n);
        }
        return copy;
    }

    /**
     * Formats the 2D array as a readable ASCII chessboard with row/column indices.
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("     ");
        for (int c = 0; c < n; c++) {
            sb.append(String.format("%2d ", c));
        }
        sb.append("\n");

        sb.append("    +");
        for (int c = 0; c < n; c++) {
            sb.append("---");
        }
        sb.append("+\n");

        for (int r = 0; r < n; r++) {
            sb.append(String.format("%2d  |", r));
            for (int c = 0; c < n; c++) {
                sb.append(" ").append(grid[r][c]).append(" ");
            }
            sb.append("|\n");
        }

        sb.append("    +");
        for (int c = 0; c < n; c++) {
            sb.append("---");
        }
        sb.append("+\n");

        return sb.toString();
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}
