import java.util.ArrayList;
import java.util.List;

/**
 * Core Backtracking Engine for the N-Queen Puzzle Game.
 *
 * Implements:
 * 1. Recursive Backtracking (single solution + all solutions + step tracer)
 * 2. Iterative Backtracking using a custom MoveStack (explicit stack simulation + step tracer)
 * 3. Recursive Hint & Auto-Complete from any valid partial player board
 */
public class NQueenSolver {

    /**
     * Represents a single recorded action during animated backtracking visualization.
     */
    public static class Step {
        public enum Action {
            PLACE,
            BACKTRACK
        }

        private final Action action;
        private final int row;
        private final int col;
        private final int depth;

        public Step(Action action, int row, int col, int depth) {
            this.action = action;
            this.row = row;
            this.col = col;
            this.depth = depth;
        }

        public Action getAction() {
            return action;
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }

        public int getDepth() {
            return depth;
        }
    }

    /**
     * Holds execution metrics and board solutions from a solver run.
     */
    public static class SolverResult {
        private final String methodName;
        private final int boardSize;
        private final boolean solved;
        private final long nodesVisited;
        private final long backtracks;
        private final long elapsedNanos;
        private final int totalSolutions;
        private final List<Board> storedSolutions;

        public SolverResult(
                String methodName,
                int boardSize,
                boolean solved,
                long nodesVisited,
                long backtracks,
                long elapsedNanos,
                int totalSolutions,
                List<Board> storedSolutions) {
            this.methodName = methodName;
            this.boardSize = boardSize;
            this.solved = solved;
            this.nodesVisited = nodesVisited;
            this.backtracks = backtracks;
            this.elapsedNanos = elapsedNanos;
            this.totalSolutions = totalSolutions;
            this.storedSolutions = storedSolutions;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getBoardSize() {
            return boardSize;
        }

        public boolean isSolved() {
            return solved;
        }

        public long getNodesVisited() {
            return nodesVisited;
        }

        public long getBacktracks() {
            return backtracks;
        }

        public double getElapsedMillis() {
            return elapsedNanos / 1_000_000.0;
        }

        public int getTotalSolutions() {
            return totalSolutions;
        }

        public List<Board> getStoredSolutions() {
            return storedSolutions;
        }

        public Board getFirstSolution() {
            return storedSolutions.isEmpty() ? null : storedSolutions.get(0);
        }
    }

    private static class Metrics {
        long nodesVisited = 0;
        long backtracks = 0;
        int totalSolutions = 0;
    }

    // -------------------------------------------------------------------------
    // 1. RECURSIVE BACKTRACKING (Find First Solution)
    // -------------------------------------------------------------------------

    public SolverResult solveOneRecursive(int n, boolean trace) {
        Board board = new Board(n);
        Metrics metrics = new Metrics();
        List<Board> solutions = new ArrayList<>();

        long start = System.nanoTime();
        boolean solved = backtrackSingle(board, 0, metrics, trace, null, 0);
        long elapsed = System.nanoTime() - start;

        if (solved) {
            solutions.add(new Board(board));
            metrics.totalSolutions = 1;
        }

        return new SolverResult(
                "Recursive Backtracking",
                n,
                solved,
                metrics.nodesVisited,
                metrics.backtracks,
                elapsed,
                metrics.totalSolutions,
                solutions);
    }

    /**
     * Records step-by-step PLACE and BACKTRACK actions for GUI animation using
     * recursive backtracking.
     */
    public List<Step> traceRecursiveSteps(int n, int maxSteps) {
        Board board = new Board(n);
        Metrics metrics = new Metrics();
        List<Step> steps = new ArrayList<>();
        backtrackSingle(board, 0, metrics, false, steps, maxSteps);
        return steps;
    }

    private boolean backtrackSingle(
            Board board,
            int row,
            Metrics metrics,
            boolean printTrace,
            List<Step> recordedSteps,
            int maxSteps) {
        int n = board.getSize();
        if (row == n) {
            return true; // Base case: all N queens placed safely
        }

        for (int col = 0; col < n; col++) {
            metrics.nodesVisited++;
            if (board.isSafe(row, col)) {
                board.placeQueen(row, col);
                if (printTrace) {
                    System.out.printf("  [Depth %d] Place Q at (%d, %d)%n", row, row, col);
                }
                if (recordedSteps != null && recordedSteps.size() < maxSteps) {
                    recordedSteps.add(new Step(Step.Action.PLACE, row, col, row + 1));
                }

                if (backtrackSingle(board, row + 1, metrics, printTrace, recordedSteps, maxSteps)) {
                    return true;
                }

                // Backtrack: remove queen and try next column
                board.removeQueen(row, col);
                metrics.backtracks++;
                if (printTrace) {
                    System.out.printf("  [Depth %d] Backtrack -> Remove Q from (%d, %d)%n", row, row, col);
                }
                if (recordedSteps != null && recordedSteps.size() < maxSteps) {
                    recordedSteps.add(new Step(Step.Action.BACKTRACK, row, col, row));
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // 2. ITERATIVE BACKTRACKING USING CUSTOM STACK (Find First Solution)
    // -------------------------------------------------------------------------

    public SolverResult solveOneIterativeWithStack(int n, boolean trace) {
        return runIterativeWithStack(n, trace, null, 0);
    }

    /**
     * Records step-by-step PLACE and BACKTRACK actions for GUI animation using
     * the custom MoveStack iterative backtracking solver.
     */
    public List<Step> traceIterativeStackSteps(int n, int maxSteps) {
        List<Step> steps = new ArrayList<>();
        runIterativeWithStack(n, false, steps, maxSteps);
        return steps;
    }

    private SolverResult runIterativeWithStack(
            int n,
            boolean printTrace,
            List<Step> recordedSteps,
            int maxSteps) {
        Board board = new Board(n);
        MoveStack stack = new MoveStack(n);
        long nodesVisited = 0;
        long backtracks = 0;

        long start = System.nanoTime();
        int row = 0;
        int col = 0;
        boolean solved = false;

        while (row < n) {
            boolean placed = false;

            while (col < n) {
                nodesVisited++;
                if (board.isSafe(row, col)) {
                    board.placeQueen(row, col);
                    stack.push(new QueenMove(row, col));
                    if (printTrace) {
                        System.out.printf("  [Stack size %d] Push & Place Q at (%d, %d)%n",
                                stack.size(), row, col);
                    }
                    if (recordedSteps != null && recordedSteps.size() < maxSteps) {
                        recordedSteps.add(new Step(Step.Action.PLACE, row, col, stack.size()));
                    }
                    row++;
                    col = 0;
                    placed = true;
                    break;
                }
                col++;
            }

            if (!placed) {
                if (stack.isEmpty()) {
                    solved = false;
                    break;
                }
                QueenMove lastMove = stack.pop();
                board.removeQueen(lastMove.getRow(), lastMove.getCol());
                backtracks++;
                if (printTrace) {
                    System.out.printf("  [Stack size %d] Pop & Backtrack from (%d, %d)%n",
                            stack.size(), lastMove.getRow(), lastMove.getCol());
                }
                if (recordedSteps != null && recordedSteps.size() < maxSteps) {
                    recordedSteps.add(new Step(Step.Action.BACKTRACK, lastMove.getRow(), lastMove.getCol(), stack.size()));
                }
                row = lastMove.getRow();
                col = lastMove.getCol() + 1;
            }
        }

        if (row == n) {
            solved = true;
        }

        long elapsed = System.nanoTime() - start;
        List<Board> solutions = new ArrayList<>();
        if (solved) {
            solutions.add(new Board(board));
        }

        return new SolverResult(
                "Iterative Backtracking (Custom Stack)",
                n,
                solved,
                nodesVisited,
                backtracks,
                elapsed,
                solved ? 1 : 0,
                solutions);
    }

    // -------------------------------------------------------------------------
    // 3. RECURSIVE BACKTRACKING (Find All Distinct Solutions)
    // -------------------------------------------------------------------------

    public SolverResult findAllSolutionsRecursive(int n, int maxBoardsToStore) {
        Board board = new Board(n);
        Metrics metrics = new Metrics();
        List<Board> stored = new ArrayList<>();

        long start = System.nanoTime();
        backtrackAll(board, 0, metrics, stored, maxBoardsToStore);
        long elapsed = System.nanoTime() - start;

        return new SolverResult(
                "Recursive Backtracking (All Solutions)",
                n,
                metrics.totalSolutions > 0,
                metrics.nodesVisited,
                metrics.backtracks,
                elapsed,
                metrics.totalSolutions,
                stored);
    }

    private void backtrackAll(
            Board board,
            int row,
            Metrics metrics,
            List<Board> stored,
            int maxBoardsToStore) {
        int n = board.getSize();
        if (row == n) {
            metrics.totalSolutions++;
            if (stored.size() < maxBoardsToStore) {
                stored.add(new Board(board));
            }
            return;
        }

        for (int col = 0; col < n; col++) {
            metrics.nodesVisited++;
            if (board.isSafe(row, col)) {
                board.placeQueen(row, col);
                backtrackAll(board, row + 1, metrics, stored, maxBoardsToStore);
                board.removeQueen(row, col);
                metrics.backtracks++;
            }
        }
    }

    // -------------------------------------------------------------------------
    // 4. INTERACTIVE GAME HELPERS: HINT & AUTO-COMPLETE FROM PARTIAL BOARD
    // -------------------------------------------------------------------------

    /**
     * Suggests the next safe QueenMove that leads to a valid full N-Queen solution
     * from the player's current board state. Returns null if the board already has
     * conflicts, is already solved, or cannot be completed without backtracking.
     */
    public QueenMove getHint(Board currentBoard) {
        if (currentBoard.isSolved() || currentBoard.hasAnyConflict()) {
            return null;
        }

        Board completed = completeFromBoard(currentBoard);
        if (completed == null) {
            return null;
        }

        int n = currentBoard.getSize();
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (completed.hasQueen(r, c) && !currentBoard.hasQueen(r, c)) {
                    return new QueenMove(r, c);
                }
            }
        }
        return null;
    }

    /**
     * Attempts to complete a player's current conflict-free partial board into a
     * full valid N-Queen solution using recursive backtracking.
     */
    public Board completeFromBoard(Board currentBoard) {
        if (currentBoard.hasAnyConflict()) {
            return null;
        }
        Board workingCopy = new Board(currentBoard);
        if (completeRowByRow(workingCopy, 0)) {
            return workingCopy;
        }
        return null;
    }

    private boolean completeRowByRow(Board board, int row) {
        int n = board.getSize();
        if (row == n) {
            return board.isSolved();
        }
        if (board.rowHasQueen(row)) {
            return completeRowByRow(board, row + 1);
        }

        for (int col = 0; col < n; col++) {
            if (board.isSafe(row, col)) {
                board.placeQueen(row, col);
                if (completeRowByRow(board, row + 1)) {
                    return true;
                }
                board.removeQueen(row, col);
            }
        }
        return false;
    }
}
