import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point for the N-Queen Puzzle Game & DSA Visualizer in Java.
 *
 * Demonstrates:
 * - 2D Array (Board.java)
 * - Custom Stack ADT (MoveStack.java) for Undo/Redo & Iterative Backtracking
 * - Recursion & Backtracking (NQueenSolver.java)
 * - Desktop Swing GUI (NQueenGUI.java)
 *
 * Modes:
 * - java Main            -> Launches the interactive Desktop Swing GUI
 * - java Main --console  -> Launches the interactive Terminal Menu
 * - java Main --test     -> Runs the built-in automated DSA verification suite
 */
public class Main {
    private static final int DEFAULT_N = 8;
    private static final int MAX_PREVIEW_SOLUTIONS = 5;

    private final Scanner scanner = new Scanner(System.in);
    private final NQueenSolver solver = new NQueenSolver();
    private int boardSize = DEFAULT_N;

    public static void main(String[] args) {
        if (args.length > 0 && "--test".equalsIgnoreCase(args[0])) {
            runSelfTests();
            return;
        }
        if (args.length > 0 && "--console".equalsIgnoreCase(args[0])) {
            new Main().runMenu();
            return;
        }
        // Default: Launch the visual Desktop Swing GUI
        SwingUtilities.invokeLater(NQueenGUI::new);
    }

    private void runMenu() {
        System.out.println("====================================================");
        System.out.println("       N-QUEEN PUZZLE GAME & DSA VISUALIZER         ");
        System.out.println("   2D Array | Custom Stack | Recursion | Backtrack  ");
        System.out.println("====================================================");

        boolean running = true;
        while (running) {
            System.out.println("\nCurrent Board Size: N = " + boardSize);
            System.out.println("1. Play Interactive N-Queen Puzzle Game");
            System.out.println("2. Auto-Solve (Recursive Backtracking)");
            System.out.println("3. Auto-Solve (Iterative Backtracking with Custom Stack)");
            System.out.println("4. Find & Count All Distinct Solutions");
            System.out.println("5. Benchmark: Recursive vs. Iterative Stack Solver");
            System.out.println("6. Change Board Size (N)");
            System.out.println("7. Run Built-in Verification Tests");
            System.out.println("0. Exit");

            int choice = readInt("Select an option (0-7): ", 0, 7);
            switch (choice) {
                case 1:
                    playInteractiveGame();
                    break;
                case 2:
                    runSingleSolver(true);
                    break;
                case 3:
                    runSingleSolver(false);
                    break;
                case 4:
                    runAllSolutions();
                    break;
                case 5:
                    runBenchmark();
                    break;
                case 6:
                    changeBoardSize();
                    break;
                case 7:
                    runSelfTests();
                    break;
                case 0:
                    running = false;
                    System.out.println("Thanks for playing N-Queen Puzzle Game!");
                    break;
                default:
                    break;
            }
        }
    }

    private void playInteractiveGame() {
        if (boardSize == 2 || boardSize == 3) {
            System.out.println("\nNote: No valid solution exists for N = " + boardSize
                    + ". You can still experiment on the board or change N to 4 or higher.");
        }

        Board board = new Board(boardSize);
        MoveStack undoStack = new MoveStack(boardSize);
        MoveStack redoStack = new MoveStack(boardSize);

        System.out.println("\n--- Interactive N-Queen Puzzle (N = " + boardSize + ") ---");
        System.out.println("Goal: Place " + boardSize + " queens so no two attack each other.");
        System.out.println("Commands: place | remove | undo | redo | hint | history | solve | reset | back");

        while (true) {
            System.out.println();
            System.out.print(board.toDisplayString());
            System.out.printf("Queens Placed: %d / %d | Undo Stack: %d | Redo Stack: %d%n",
                    board.getQueenCount(), boardSize, undoStack.size(), redoStack.size());

            if (board.isSolved()) {
                System.out.println("\nCONGRATULATIONS! You solved the " + boardSize + "-Queen Puzzle!");
                System.out.println("Winning Move Stack: " + undoStack);
                break;
            }

            System.out.print("Enter command (or 'p r c' to place): ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            if (cmd.equals("back") || cmd.equals("quit") || cmd.equals("exit")) {
                break;
            } else if (cmd.equals("p") || cmd.equals("place")) {
                handlePlaceCommand(parts, board, undoStack, redoStack);
            } else if (cmd.equals("r") || cmd.equals("remove")) {
                handleRemoveCommand(parts, board, undoStack, redoStack);
            } else if (cmd.equals("u") || cmd.equals("undo")) {
                if (undoStack.isEmpty()) {
                    System.out.println("Nothing to undo (MoveStack is empty).");
                } else {
                    QueenMove last = undoStack.pop();
                    board.removeQueen(last.getRow(), last.getCol());
                    redoStack.push(last);
                    System.out.println("Undid placement at " + last + ".");
                }
            } else if (cmd.equals("redo")) {
                if (redoStack.isEmpty()) {
                    System.out.println("Nothing to redo.");
                } else {
                    QueenMove move = redoStack.pop();
                    String conflict = board.getConflictReason(move.getRow(), move.getCol());
                    if (conflict != null) {
                        System.out.println("Cannot redo " + move + ": " + conflict);
                    } else if (board.placeQueen(move.getRow(), move.getCol())) {
                        undoStack.push(move);
                        System.out.println("Redid placement at " + move + ".");
                    }
                }
            } else if (cmd.equals("h") || cmd.equals("hint")) {
                QueenMove hint = solver.getHint(board);
                if (hint != null) {
                    System.out.println("Hint (Recursive Backtracking): Try placing a queen at " + hint + ".");
                } else {
                    System.out.println("No completion possible from the current board state. Try 'undo' or 'reset'.");
                }
            } else if (cmd.equals("history")) {
                System.out.println("Move History Stack: " + undoStack);
            } else if (cmd.equals("solve")) {
                Board completed = solver.completeFromBoard(board);
                if (completed == null) {
                    System.out.println("Current board leads to a dead end. Use 'undo' or 'reset' first.");
                } else {
                    System.out.println("\nAuto-completed solution from your current board:");
                    System.out.print(completed.toDisplayString());
                    break;
                }
            } else if (cmd.equals("reset")) {
                board.clear();
                undoStack.clear();
                redoStack.clear();
                System.out.println("Board and stacks reset.");
            } else {
                System.out.println("Unknown command. Use: place <row> <col>, remove <row> <col>, undo, redo, hint, history, solve, reset, back");
            }
        }
    }

    private void handlePlaceCommand(String[] parts, Board board, MoveStack undoStack, MoveStack redoStack) {
        int row;
        int col;
        if (parts.length == 3) {
            try {
                row = Integer.parseInt(parts[1]);
                col = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid coordinates. Example: place 0 1");
                return;
            }
        } else {
            row = readInt("Row (0-" + (boardSize - 1) + "): ", 0, boardSize - 1);
            col = readInt("Col (0-" + (boardSize - 1) + "): ", 0, boardSize - 1);
        }

        if (!board.isInBounds(row, col)) {
            System.out.println("Out of bounds! Valid range is 0 to " + (boardSize - 1) + ".");
            return;
        }
        if (board.hasQueen(row, col)) {
            System.out.println("A queen is already at (" + row + ", " + col + ").");
            return;
        }

        String conflict = board.getConflictReason(row, col);
        if (conflict != null) {
            System.out.println("Unsafe move rejected! " + conflict);
            return;
        }

        board.placeQueen(row, col);
        undoStack.push(new QueenMove(row, col));
        redoStack.clear();
        System.out.println("Placed queen at (" + row + ", " + col + ").");
    }

    private void handleRemoveCommand(String[] parts, Board board, MoveStack undoStack, MoveStack redoStack) {
        int row;
        int col;
        if (parts.length == 3) {
            try {
                row = Integer.parseInt(parts[1]);
                col = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid coordinates. Example: remove 0 1");
                return;
            }
        } else {
            row = readInt("Row to remove (0-" + (boardSize - 1) + "): ", 0, boardSize - 1);
            col = readInt("Col to remove (0-" + (boardSize - 1) + "): ", 0, boardSize - 1);
        }

        if (!board.removeQueen(row, col)) {
            System.out.println("No queen found at (" + row + ", " + col + ").");
            return;
        }

        QueenMove target = new QueenMove(row, col);
        QueenMove[] moves = undoStack.toArray();
        undoStack.clear();
        for (QueenMove m : moves) {
            if (!m.equals(target)) {
                undoStack.push(m);
            }
        }
        redoStack.clear();
        System.out.println("Removed queen from (" + row + ", " + col + ").");
    }

    private void runSingleSolver(boolean useRecursive) {
        boolean trace = false;
        if (boardSize <= 6) {
            System.out.print("Show step-by-step backtracking trace? (y/n): ");
            trace = scanner.nextLine().trim().equalsIgnoreCase("y");
        } else {
            System.out.println("(Step-by-step trace auto-disabled for N > 6 to keep output readable.)");
        }

        NQueenSolver.SolverResult result = useRecursive
                ? solver.solveOneRecursive(boardSize, trace)
                : solver.solveOneIterativeWithStack(boardSize, trace);

        printSolverResult(result);
    }

    private void runAllSolutions() {
        if (boardSize > 12) {
            System.out.println("Warning: Finding all solutions for N > 12 can take a while.");
        }
        NQueenSolver.SolverResult result = solver.findAllSolutionsRecursive(boardSize, MAX_PREVIEW_SOLUTIONS);

        System.out.println("\n--- All Solutions Result (N = " + boardSize + ") ---");
        System.out.println("Total Distinct Solutions Found: " + result.getTotalSolutions());
        System.out.println("Nodes Visited:                  " + result.getNodesVisited());
        System.out.println("Backtracks Performed:           " + result.getBacktracks());
        System.out.printf("Execution Time:                 %.3f ms%n", result.getElapsedMillis());

        List<Board> previews = result.getStoredSolutions();
        if (!previews.isEmpty()) {
            System.out.println("\nShowing first " + previews.size() + " solution(s):");
            for (int i = 0; i < previews.size(); i++) {
                System.out.println("Solution #" + (i + 1) + ":");
                System.out.print(previews.get(i).toDisplayString());
            }
        }
    }

    private void runBenchmark() {
        System.out.println("\n--- Benchmark: Recursive vs. Iterative Stack (N = " + boardSize + ") ---");
        NQueenSolver.SolverResult rec = solver.solveOneRecursive(boardSize, false);
        NQueenSolver.SolverResult iter = solver.solveOneIterativeWithStack(boardSize, false);

        System.out.printf("%-38s | %-8s | %-10s | %-10s | %-10s%n",
                "Algorithm", "Solved", "Nodes", "Backtracks", "Time (ms)");
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.printf("%-38s | %-8s | %-10d | %-10d | %10.3f%n",
                rec.getMethodName(), rec.isSolved(), rec.getNodesVisited(), rec.getBacktracks(), rec.getElapsedMillis());
        System.out.printf("%-38s | %-8s | %-10d | %-10d | %10.3f%n",
                iter.getMethodName(), iter.isSolved(), iter.getNodesVisited(), iter.getBacktracks(), iter.getElapsedMillis());
    }

    private void changeBoardSize() {
        boardSize = readInt("Enter new board size N (1 to 14): ", 1, 14);
        System.out.println("Board size updated to N = " + boardSize + ".");
    }

    private void printSolverResult(NQueenSolver.SolverResult result) {
        System.out.println("\n--- " + result.getMethodName() + " (N = " + result.getBoardSize() + ") ---");
        if (result.isSolved()) {
            System.out.print(result.getFirstSolution().toDisplayString());
        } else {
            System.out.println("No solution exists for N = " + result.getBoardSize() + ".");
        }
        System.out.println("Nodes Visited:        " + result.getNodesVisited());
        System.out.println("Backtracks Performed: " + result.getBacktracks());
        System.out.printf("Execution Time:       %.3f ms%n", result.getElapsedMillis());
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.println("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter an integer.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // BUILT-IN AUTOMATED VERIFICATION SUITE (--test)
    // -------------------------------------------------------------------------

    private static void runSelfTests() {
        System.out.println("Running N-Queen DSA Project Verification Suite...");
        int passed = 0;

        MoveStack stack = new MoveStack(4);
        for (int i = 0; i < 10; i++) {
            stack.push(new QueenMove(i, i));
        }
        assertTrue(stack.size() == 10, "MoveStack dynamic resize failed");
        assertTrue(stack.peek().equals(new QueenMove(9, 9)), "MoveStack peek failed");
        assertTrue(stack.pop().equals(new QueenMove(9, 9)), "MoveStack pop failed");
        assertTrue(stack.size() == 9, "MoveStack size after pop failed");
        passed++;

        Board board = new Board(4);
        assertTrue(board.placeQueen(0, 1), "Place queen (0,1) failed");
        assertTrue(!board.isSafe(0, 3), "Same-row conflict not detected");
        assertTrue(!board.isSafe(2, 1), "Same-col conflict not detected");
        assertTrue(!board.isSafe(2, 3), "Diagonal conflict not detected");
        assertTrue(board.isSafe(1, 3), "Safe square (1,3) falsely flagged");
        passed++;

        NQueenSolver solver = new NQueenSolver();
        int[] expectedCounts = {0, 1, 0, 0, 2, 10, 4, 40, 92};
        for (int n = 1; n <= 8; n++) {
            NQueenSolver.SolverResult rec = solver.solveOneRecursive(n, false);
            NQueenSolver.SolverResult iter = solver.solveOneIterativeWithStack(n, false);
            NQueenSolver.SolverResult all = solver.findAllSolutionsRecursive(n, 2);

            boolean shouldSolve = expectedCounts[n] > 0;
            assertTrue(rec.isSolved() == shouldSolve, "Recursive solved mismatch for N=" + n);
            assertTrue(iter.isSolved() == shouldSolve, "Iterative solved mismatch for N=" + n);
            assertTrue(all.getTotalSolutions() == expectedCounts[n],
                    "All-solutions count mismatch for N=" + n + ": got " + all.getTotalSolutions());

            if (shouldSolve) {
                assertTrue(rec.getFirstSolution().isSolved(), "Recursive solution invalid for N=" + n);
                assertTrue(iter.getFirstSolution().isSolved(), "Iterative solution invalid for N=" + n);
            }
        }
        passed++;

        Board partial = new Board(4);
        partial.placeQueen(0, 1);
        QueenMove hint = solver.getHint(partial);
        assertTrue(hint != null && partial.isSafe(hint.getRow(), hint.getCol()), "Hint failed for partial 4x4 board");
        Board completed = solver.completeFromBoard(partial);
        assertTrue(completed != null && completed.isSolved(), "Partial board auto-complete failed");
        passed++;

        System.out.println("ALL " + passed + " TEST GROUPS PASSED!");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("TEST FAILED: " + message);
        }
    }
}
