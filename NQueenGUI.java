import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Full N-Queen Puzzle Game with Levels, Lives/Elimination, Countdown Timer,
 * Win Progression, Star Ratings, and Live DSA Backtracking Visualizer.
 *
 * Uses:
 * - Board (2D char[][] array)
 * - MoveStack (Custom LIFO Stack for Undo/Redo & Iterative Backtracking)
 * - NQueenSolver (Recursive & Iterative Stack Backtracking + Hint Engine)
 */
public class NQueenGUI extends JFrame {

    private static class LevelConfig {
        final int levelNumber;
        final String name;
        final int boardSize;
        final int maxLives;
        final int maxHints;
        final int timeSeconds;

        LevelConfig(int levelNumber, String name, int boardSize, int maxLives, int maxHints, int timeSeconds) {
            this.levelNumber = levelNumber;
            this.name = name;
            this.boardSize = boardSize;
            this.maxLives = maxLives;
            this.maxHints = maxHints;
            this.timeSeconds = timeSeconds;
        }

        @Override
        public String toString() {
            return "Level " + levelNumber + ": " + name + " (" + boardSize + "x" + boardSize + ")";
        }
    }

    private static final LevelConfig[] LEVELS = {
            new LevelConfig(1, "Apprentice", 4, 3, 2, 60),
            new LevelConfig(2, "Squire", 5, 3, 2, 75),
            new LevelConfig(3, "Knight", 6, 3, 2, 95),
            new LevelConfig(4, "Bishop", 7, 3, 2, 120),
            new LevelConfig(5, "Royal Master", 8, 3, 2, 150),
            new LevelConfig(6, "Grandmaster", 9, 3, 1, 190),
            new LevelConfig(7, "Legend", 10, 3, 1, 240)
    };

    private static final Color BG_DARK = new Color(20, 24, 33);
    private static final Color PANEL_DARK = new Color(30, 36, 48);
    private static final Color CARD_DARK = new Color(40, 48, 64);
    private static final Color TEXT_LIGHT = new Color(238, 242, 248);
    private static final Color TEXT_MUTED = new Color(156, 168, 188);
    private static final Color LIGHT_SQUARE = new Color(240, 217, 181);
    private static final Color DARK_SQUARE = new Color(181, 136, 99);
    private static final Color DANGER_TINT = new Color(220, 38, 38, 55);
    private static final Color HINT_COLOR = new Color(34, 197, 94, 185);
    private static final Color CONFLICT_COLOR = new Color(239, 68, 68, 200);
    private static final Color PLACE_FLASH = new Color(59, 130, 246, 165);
    private static final Color BACKTRACK_FLASH = new Color(245, 158, 11, 185);
    private static final Color ACCENT_BLUE = new Color(59, 130, 246);
    private static final Color ACCENT_GREEN = new Color(16, 185, 129);
    private static final Color ACCENT_PURPLE = new Color(139, 92, 246);
    private static final Color ACCENT_RED = new Color(220, 38, 38);
    private static final Color ACCENT_GOLD = new Color(245, 197, 66);
    private static final Color BTN_NEUTRAL = new Color(56, 65, 82);

    private final NQueenSolver solver = new NQueenSolver();

    // Game & Level State
    private int currentLevelIndex = 0;
    private int boardSize = LEVELS[0].boardSize;
    private int livesLeft = LEVELS[0].maxLives;
    private int hintsLeft = LEVELS[0].maxHints;
    private int hintsUsedThisLevel = 0;
    private int secondsLeft = LEVELS[0].timeSeconds;
    private int totalScore = 0;
    private boolean levelOver = false;
    private boolean showDangerZones = false;
    private boolean syncingSelector = false;

    private Board board;
    private MoveStack undoStack;
    private MoveStack redoStack;

    // UI Components
    private ChessBoardPanel boardPanel;
    private JComboBox<LevelConfig> levelSelector;
    private JSlider speedSlider;
    private JLabel levelBadgeLabel;
    private JLabel livesLabel;
    private JLabel timerLabel;
    private JLabel scoreLabel;
    private JLabel queensLabel;
    private JLabel statsLabel;
    private JLabel statusLabel;
    private JTextArea stackViewArea;
    private JButton undoBtn;
    private JButton redoBtn;
    private JButton hintBtn;
    private JButton stopAnimBtn;
    private JCheckBox dangerToggle;

    // Visual Highlights & Timers
    private QueenMove hintCell = null;
    private QueenMove flashCell = null;
    private QueenMove attackerCell = null;
    private Color flashColor = null;
    private Timer countdownTimer = null;
    private Timer conflictClearTimer = null;
    private Timer animationTimer = null;

    // Backtracking Animation State
    private List<NQueenSolver.Step> animationSteps = new ArrayList<>();
    private int animationIndex = 0;
    private long animPlacedCount = 0;
    private long animBacktrackCount = 0;
    private String activeAnimName = "";

    public NQueenGUI() {
        super("N-Queen Puzzle Game \u2014 Levels, Elimination & DSA Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 710));
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BG_DARK);

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createSidebarPanel(), BorderLayout.EAST);
        add(createStatusBar(), BorderLayout.SOUTH);

        startLevel(0);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startLevel(int levelIdx) {
        stopAnimation();
        stopCountdown();
        clearConflictFlash();

        this.currentLevelIndex = Math.max(0, Math.min(levelIdx, LEVELS.length - 1));
        LevelConfig cfg = LEVELS[currentLevelIndex];

        this.boardSize = cfg.boardSize;
        this.livesLeft = cfg.maxLives;
        this.hintsLeft = cfg.maxHints;
        this.hintsUsedThisLevel = 0;
        this.secondsLeft = cfg.timeSeconds;
        this.levelOver = false;
        this.board = new Board(boardSize);
        this.undoStack = new MoveStack(boardSize);
        this.redoStack = new MoveStack(boardSize);
        this.hintCell = null;
        this.animPlacedCount = 0;
        this.animBacktrackCount = 0;

        syncingSelector = true;
        if (levelSelector != null) {
            levelSelector.setSelectedIndex(currentLevelIndex);
        }
        syncingSelector = false;

        setStatus("Level " + cfg.levelNumber + " (" + cfg.name + "): Place " + boardSize
                + " safe queens before losing all " + cfg.maxLives + " lives or running out of time!");

        startCountdown();
        refreshUI();
    }

    private void startCountdown() {
        stopCountdown();
        countdownTimer = new Timer(1000, e -> {
            if (levelOver || (animationTimer != null && animationTimer.isRunning())) {
                return;
            }
            secondsLeft--;
            refreshHUD();
            if (secondsLeft <= 0) {
                secondsLeft = 0;
                triggerElimination("TIME'S UP! You ran out of time on Level " + LEVELS[currentLevelIndex].levelNumber + ".");
            }
        });
        countdownTimer.start();
    }

    private void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
    }

    private void clearConflictFlash() {
        if (conflictClearTimer != null) {
            conflictClearTimer.stop();
            conflictClearTimer = null;
        }
        flashCell = null;
        attackerCell = null;
        flashColor = null;
    }

    // -------------------------------------------------------------------------
    // UI CONSTRUCTION
    // -------------------------------------------------------------------------

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(PANEL_DARK);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Left: Level Title & Subtitle
        JPanel leftBox = new JPanel();
        leftBox.setLayout(new BoxLayout(leftBox, BoxLayout.Y_AXIS));
        leftBox.setOpaque(false);

        levelBadgeLabel = new JLabel("LEVEL 1 / 7 \u2014 Apprentice (4x4)");
        levelBadgeLabel.setForeground(ACCENT_GOLD);
        levelBadgeLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel subtitle = new JLabel("N-Queen Puzzle Game  \u2022  2D Array | Custom Stack | Recursion | Backtracking");
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));

        leftBox.add(levelBadgeLabel);
        leftBox.add(Box.createVerticalStrut(2));
        leftBox.add(subtitle);

        // Right: Lives, Timer, Score, Level Picker
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        hud.setOpaque(false);

        livesLabel = new JLabel("Lives: \u2665 \u2665 \u2665");
        livesLabel.setForeground(new Color(248, 113, 113));
        livesLabel.setFont(new Font("SansSerif", Font.BOLD, 15));

        timerLabel = new JLabel("Time: 01:00");
        timerLabel.setForeground(TEXT_LIGHT);
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 15));

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(ACCENT_GREEN);
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 15));

        levelSelector = new JComboBox<>(LEVELS);
        levelSelector.setFocusable(false);
        levelSelector.addActionListener(e -> {
            if (!syncingSelector) {
                int idx = levelSelector.getSelectedIndex();
                if (idx >= 0) {
                    startLevel(idx);
                }
            }
        });

        hud.add(livesLabel);
        hud.add(timerLabel);
        hud.add(scoreLabel);
        hud.add(levelSelector);

        header.add(leftBox, BorderLayout.WEST);
        header.add(hud, BorderLayout.EAST);
        return header;
    }

    private JPanel createCenterPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.setBorder(new EmptyBorder(16, 22, 16, 12));

        boardPanel = new ChessBoardPanel();
        wrapper.add(boardPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PANEL_DARK);
        sidebar.setPreferredSize(new Dimension(300, 620));
        sidebar.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Progress Card
        JPanel progressCard = new JPanel();
        progressCard.setLayout(new BoxLayout(progressCard, BoxLayout.Y_AXIS));
        progressCard.setBackground(CARD_DARK);
        progressCard.setBorder(new EmptyBorder(10, 12, 10, 12));
        progressCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        queensLabel = new JLabel("Queens Placed: 0 / 4");
        queensLabel.setForeground(TEXT_LIGHT);
        queensLabel.setFont(new Font("SansSerif", Font.BOLD, 15));

        statsLabel = new JLabel("Rule: 3 mistakes or 00:00 = Eliminated");
        statsLabel.setForeground(TEXT_MUTED);
        statsLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        progressCard.add(queensLabel);
        progressCard.add(Box.createVerticalStrut(4));
        progressCard.add(statsLabel);

        sidebar.add(progressCard);
        sidebar.add(Box.createVerticalStrut(14));

        // Game Controls
        sidebar.add(sectionLabel("PUZZLE CONTROLS"));
        sidebar.add(Box.createVerticalStrut(6));

        JPanel undoRedoRow = new JPanel(new GridLayout(1, 2, 8, 0));
        undoRedoRow.setOpaque(false);
        undoRedoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        undoRedoRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        undoBtn = createStyledButton("Undo (Pop)", BTN_NEUTRAL);
        undoBtn.addActionListener(e -> handleUndo());

        redoBtn = createStyledButton("Redo", BTN_NEUTRAL);
        redoBtn.addActionListener(e -> handleRedo());

        undoRedoRow.add(undoBtn);
        undoRedoRow.add(redoBtn);
        sidebar.add(undoRedoRow);
        sidebar.add(Box.createVerticalStrut(8));

        JPanel hintRetryRow = new JPanel(new GridLayout(1, 2, 8, 0));
        hintRetryRow.setOpaque(false);
        hintRetryRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        hintRetryRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        hintBtn = createStyledButton("Hint (2)", ACCENT_GREEN);
        hintBtn.addActionListener(e -> handleHint());

        JButton retryBtn = createStyledButton("Retry Level", ACCENT_RED);
        retryBtn.addActionListener(e -> startLevel(currentLevelIndex));

        hintRetryRow.add(hintBtn);
        hintRetryRow.add(retryBtn);
        sidebar.add(hintRetryRow);
        sidebar.add(Box.createVerticalStrut(8));

        dangerToggle = new JCheckBox("Highlight Attacked Squares (Helper)");
        dangerToggle.setOpaque(false);
        dangerToggle.setForeground(TEXT_LIGHT);
        dangerToggle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dangerToggle.setFocusPainted(false);
        dangerToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        dangerToggle.addActionListener(e -> {
            showDangerZones = dangerToggle.isSelected();
            boardPanel.repaint();
        });
        sidebar.add(dangerToggle);
        sidebar.add(Box.createVerticalStrut(14));

        // Backtracking Visualizer (DSA Demo)
        sidebar.add(sectionLabel("DSA BACKTRACKING DEMO"));
        sidebar.add(Box.createVerticalStrut(6));

        JButton animRecBtn = createStyledButton("Watch Recursive Backtracking", ACCENT_BLUE);
        animRecBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        animRecBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        animRecBtn.addActionListener(e -> startBacktrackingAnimation(true));
        sidebar.add(animRecBtn);
        sidebar.add(Box.createVerticalStrut(7));

        JButton animStackBtn = createStyledButton("Watch Custom Stack Backtracking", ACCENT_PURPLE);
        animStackBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        animStackBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        animStackBtn.addActionListener(e -> startBacktrackingAnimation(false));
        sidebar.add(animStackBtn);
        sidebar.add(Box.createVerticalStrut(7));

        stopAnimBtn = createStyledButton("Stop Animation", BTN_NEUTRAL);
        stopAnimBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        stopAnimBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        stopAnimBtn.setEnabled(false);
        stopAnimBtn.addActionListener(e -> {
            stopAnimation();
            setStatus("Animation stopped. Click 'Retry Level' to play this level again.");
            refreshUI();
        });
        sidebar.add(stopAnimBtn);
        sidebar.add(Box.createVerticalStrut(6));

        JLabel speedLbl = new JLabel("Animation Speed (Slow <-> Fast)");
        speedLbl.setForeground(TEXT_MUTED);
        speedLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        speedLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(speedLbl);

        speedSlider = new JSlider(15, 450, 120);
        speedSlider.setInverted(true);
        speedSlider.setOpaque(false);
        speedSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        speedSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        speedSlider.addChangeListener(e -> {
            if (animationTimer != null) {
                animationTimer.setDelay(speedSlider.getValue());
            }
        });
        sidebar.add(speedSlider);
        sidebar.add(Box.createVerticalStrut(12));

        // Live Custom MoveStack Viewer
        sidebar.add(sectionLabel("LIVE MOVESTACK (CUSTOM STACK)"));
        sidebar.add(Box.createVerticalStrut(6));

        stackViewArea = new JTextArea(6, 20);
        stackViewArea.setEditable(false);
        stackViewArea.setBackground(BG_DARK);
        stackViewArea.setForeground(new Color(147, 197, 253));
        stackViewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        stackViewArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(stackViewArea);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(55, 65, 81)));
        sidebar.add(scroll);

        return sidebar;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(15, 18, 25));
        bar.setBorder(new EmptyBorder(9, 20, 9, 20));

        statusLabel = new JLabel("Welcome to N-Queen Puzzle Game!");
        statusLabel.setForeground(TEXT_LIGHT);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_MUTED);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // -------------------------------------------------------------------------
    // CORE GAMEPLAY: PLACEMENT, ELIMINATION & WIN LOGIC
    // -------------------------------------------------------------------------

    private void handleSquareClick(int row, int col) {
        if (levelOver || (animationTimer != null && animationTimer.isRunning())) {
            return;
        }
        hintCell = null;
        clearConflictFlash();

        // Clicking an existing queen removes it
        if (board.hasQueen(row, col)) {
            board.removeQueen(row, col);
            QueenMove removed = new QueenMove(row, col);
            QueenMove[] existing = undoStack.toArray();
            undoStack.clear();
            for (QueenMove m : existing) {
                if (!m.equals(removed)) {
                    undoStack.push(m);
                }
            }
            redoStack.clear();
            setStatus("Removed queen from (" + row + ", " + col + ").");
            refreshUI();
            return;
        }

        // Check conflict -> lose a life!
        QueenMove attacker = board.findConflictingQueen(row, col);
        if (attacker != null) {
            livesLeft--;
            flashCell = new QueenMove(row, col);
            attackerCell = attacker;
            flashColor = CONFLICT_COLOR;

            String reason = board.getConflictReason(row, col);
            setStatus("DANGER! " + reason + " (-1 Life! Remaining: " + livesLeft + ")");
            refreshUI();

            if (livesLeft <= 0) {
                triggerElimination("ELIMINATED! You lost all lives after placing a queen under attack at ("
                        + row + ", " + col + ").");
            } else {
                conflictClearTimer = new Timer(950, ev -> {
                    clearConflictFlash();
                    boardPanel.repaint();
                });
                conflictClearTimer.setRepeats(false);
                conflictClearTimer.start();
            }
            return;
        }

        // Safe move -> place queen and push onto custom MoveStack
        board.placeQueen(row, col);
        undoStack.push(new QueenMove(row, col));
        redoStack.clear();
        setStatus("Safe placement at (" + row + ", " + col + ")! "
                + (boardSize - board.getQueenCount()) + " queen(s) left to win.");
        refreshUI();

        if (board.isSolved()) {
            triggerWin();
        }
    }

    private void triggerElimination(String reason) {
        levelOver = true;
        stopCountdown();
        refreshUI();
        setStatus(reason);

        Object[] options = {"Retry Level", "Watch Solution (Backtracking)", "Quit"};
        int choice = JOptionPane.showOptionDialog(
                this,
                reason + "\n\nCurrent Total Score: " + totalScore,
                "ELIMINATED \u2014 GAME OVER",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {
            startLevel(currentLevelIndex);
        } else if (choice == 1) {
            startBacktrackingAnimation(true);
        }
    }

    private void triggerWin() {
        levelOver = true;
        stopCountdown();

        LevelConfig cfg = LEVELS[currentLevelIndex];
        int stars = (livesLeft == cfg.maxLives && hintsUsedThisLevel == 0)
                ? 3
                : (livesLeft >= 2 ? 2 : 1);
        String starStr = stars == 3 ? "\u2605 \u2605 \u2605" : (stars == 2 ? "\u2605 \u2605 \u2606" : "\u2605 \u2606 \u2606");

        int levelPoints = Math.max(50, (boardSize * 100) + (livesLeft * 60) + (secondsLeft * 2) - (hintsUsedThisLevel * 40));
        totalScore += levelPoints;
        refreshUI();

        boolean isFinalLevel = currentLevelIndex == LEVELS.length - 1;
        setStatus("LEVEL " + cfg.levelNumber + " CLEARED! " + starStr + " (+" + levelPoints + " pts)");

        if (isFinalLevel) {
            Object[] opts = {"Play Again from Level 1", "Stay Here"};
            int pick = JOptionPane.showOptionDialog(
                    this,
                    "GRAND CHAMPION!\nYou conquered all " + LEVELS.length + " levels!\n\n"
                            + "Rating: " + starStr + "\n"
                            + "Level Score: +" + levelPoints + "\n"
                            + "Final Total Score: " + totalScore,
                    "ALL LEVELS CLEARED!",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    opts,
                    opts[0]);
            if (pick == 0) {
                totalScore = 0;
                startLevel(0);
            }
        } else {
            LevelConfig next = LEVELS[currentLevelIndex + 1];
            Object[] opts = {"Next Level (" + next.boardSize + "x" + next.boardSize + ") ->", "Replay Level"};
            int pick = JOptionPane.showOptionDialog(
                    this,
                    "LEVEL " + cfg.levelNumber + " CLEARED!\n\n"
                            + "Rating: " + starStr + "\n"
                            + "Lives Remaining: " + livesLeft + " / " + cfg.maxLives + "\n"
                            + "Time Remaining: " + formatTime(secondsLeft) + "\n"
                            + "Points Earned: +" + levelPoints + "\n"
                            + "Total Score: " + totalScore,
                    "VICTORY!",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    opts,
                    opts[0]);
            if (pick == 0) {
                startLevel(currentLevelIndex + 1);
            } else if (pick == 1) {
                startLevel(currentLevelIndex);
            }
        }
    }

    private void handleUndo() {
        if (levelOver || (animationTimer != null && animationTimer.isRunning()) || undoStack.isEmpty()) {
            return;
        }
        QueenMove last = undoStack.pop();
        board.removeQueen(last.getRow(), last.getCol());
        redoStack.push(last);
        hintCell = null;
        clearConflictFlash();
        setStatus("Undo: popped " + last + " from MoveStack.");
        refreshUI();
    }

    private void handleRedo() {
        if (levelOver || (animationTimer != null && animationTimer.isRunning()) || redoStack.isEmpty()) {
            return;
        }
        QueenMove move = redoStack.pop();
        if (!board.isSafe(move.getRow(), move.getCol())) {
            setStatus("Cannot redo " + move + ": square is now under attack.");
            refreshUI();
            return;
        }
        board.placeQueen(move.getRow(), move.getCol());
        undoStack.push(move);
        hintCell = null;
        setStatus("Redo: pushed " + move + " back onto MoveStack.");
        refreshUI();
    }

    private void handleHint() {
        if (levelOver || (animationTimer != null && animationTimer.isRunning())) {
            return;
        }
        if (hintsLeft <= 0) {
            setStatus("No hints left on this level!");
            return;
        }
        QueenMove hint = solver.getHint(board);
        if (hint == null) {
            setStatus("Dead end! No valid solution from your current queens. Use Undo!");
            hintCell = null;
        } else {
            hintsLeft--;
            hintsUsedThisLevel++;
            hintCell = hint;
            setStatus("Hint (Recursive Backtracking): Click the green square at " + hint + "!");
        }
        refreshUI();
    }

    // -------------------------------------------------------------------------
    // DSA BACKTRACKING ANIMATION (RECURSIVE & ITERATIVE STACK)
    // -------------------------------------------------------------------------

    private void startBacktrackingAnimation(boolean recursive) {
        stopAnimation();
        stopCountdown();
        clearConflictFlash();

        levelOver = true; // Watching solver doesn't grant level-win points
        board = new Board(boardSize);
        undoStack = new MoveStack(boardSize);
        redoStack = new MoveStack(boardSize);
        hintCell = null;

        activeAnimName = recursive ? "Recursive Backtracking" : "Custom Stack Backtracking";
        animationSteps = recursive
                ? solver.traceRecursiveSteps(boardSize, 2500)
                : solver.traceIterativeStackSteps(boardSize, 2500);
        animationIndex = 0;
        animPlacedCount = 0;
        animBacktrackCount = 0;

        stopAnimBtn.setEnabled(true);
        setStatus("Animating " + activeAnimName + " on " + boardSize + "x" + boardSize + "...");

        animationTimer = new Timer(speedSlider.getValue(), e -> {
            if (animationIndex >= animationSteps.size()) {
                stopAnimation();
                flashCell = null;
                setStatus(activeAnimName + " solved " + boardSize + "x" + boardSize
                        + " (" + animPlacedCount + " placements, " + animBacktrackCount
                        + " backtracks). Click 'Retry Level' to play!");
                refreshUI();
                return;
            }

            NQueenSolver.Step step = animationSteps.get(animationIndex++);
            if (step.getAction() == NQueenSolver.Step.Action.PLACE) {
                board.placeQueen(step.getRow(), step.getCol());
                undoStack.push(new QueenMove(step.getRow(), step.getCol()));
                animPlacedCount++;
                flashCell = new QueenMove(step.getRow(), step.getCol());
                flashColor = PLACE_FLASH;
                setStatus("[" + activeAnimName + "] Place Q at (" + step.getRow() + ", " + step.getCol() + ")");
            } else {
                board.removeQueen(step.getRow(), step.getCol());
                if (!undoStack.isEmpty()) {
                    undoStack.pop();
                }
                animBacktrackCount++;
                flashCell = new QueenMove(step.getRow(), step.getCol());
                flashColor = BACKTRACK_FLASH;
                setStatus("[" + activeAnimName + "] Backtrack! Pop & remove Q from ("
                        + step.getRow() + ", " + step.getCol() + ")");
            }
            refreshUI();
        });
        animationTimer.start();
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
        if (stopAnimBtn != null) {
            stopAnimBtn.setEnabled(false);
        }
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private String formatTime(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void refreshHUD() {
        LevelConfig cfg = LEVELS[currentLevelIndex];
        levelBadgeLabel.setText("LEVEL " + cfg.levelNumber + " / " + LEVELS.length
                + " \u2014 " + cfg.name + " (" + cfg.boardSize + "x" + cfg.boardSize + ")");

        StringBuilder hearts = new StringBuilder("Lives: ");
        for (int i = 0; i < cfg.maxLives; i++) {
            hearts.append(i < livesLeft ? "\u2665 " : "\u2661 ");
        }
        livesLabel.setText(hearts.toString().trim());
        livesLabel.setForeground(livesLeft <= 1 ? ACCENT_RED : new Color(248, 113, 113));

        timerLabel.setText("Time: " + formatTime(secondsLeft));
        timerLabel.setForeground(secondsLeft <= 15 ? ACCENT_RED : TEXT_LIGHT);

        scoreLabel.setText("Score: " + totalScore);
    }

    private void refreshUI() {
        refreshHUD();
        queensLabel.setText("Queens Placed: " + board.getQueenCount() + " / " + boardSize);
        if (animationTimer != null && animationTimer.isRunning()) {
            statsLabel.setText("Placements: " + animPlacedCount + "  |  Backtracks: " + animBacktrackCount);
        } else {
            statsLabel.setText("3 mistakes or 00:00 = Eliminated");
        }

        boolean animating = animationTimer != null && animationTimer.isRunning();
        undoBtn.setEnabled(!levelOver && !animating && !undoStack.isEmpty());
        redoBtn.setEnabled(!levelOver && !animating && !redoStack.isEmpty());
        hintBtn.setText("Hint (" + hintsLeft + ")");
        hintBtn.setEnabled(!levelOver && !animating && hintsLeft > 0);

        QueenMove[] moves = undoStack.toArray();
        if (moves.length == 0) {
            stackViewArea.setText("(empty MoveStack)\n\nPlace queens on the\nboard to push moves\nonto the stack.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = moves.length - 1; i >= 0; i--) {
                String tag = (i == moves.length - 1) ? "TOP -> " : "       ";
                sb.append(tag).append("[").append(i).append("] ").append(moves[i]).append("\n");
            }
            stackViewArea.setText(sb.toString());
            stackViewArea.setCaretPosition(0);
        }

        boardPanel.repaint();
    }

    // -------------------------------------------------------------------------
    // CUSTOM CHESSBOARD CANVAS WITH VECTOR QUEENS & RED ATTACK LINE
    // -------------------------------------------------------------------------

    private class ChessBoardPanel extends JPanel {
        private static final int LABEL_MARGIN = 26;

        ChessBoardPanel() {
            setBackground(BG_DARK);
            setPreferredSize(new Dimension(560, 560));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int sq = getSquareSize();
                    int boardPx = sq * boardSize;
                    int originX = (getWidth() - boardPx) / 2 + LABEL_MARGIN / 2;
                    int originY = (getHeight() - boardPx) / 2 + LABEL_MARGIN / 2;

                    int col = (e.getX() - originX) / sq;
                    int row = (e.getY() - originY) / sq;
                    if (e.getX() >= originX && e.getY() >= originY
                            && row >= 0 && row < boardSize && col >= 0 && col < boardSize) {
                        handleSquareClick(row, col);
                    }
                }
            });
        }

        private int getSquareSize() {
            int usable = Math.min(getWidth(), getHeight()) - LABEL_MARGIN - 16;
            return Math.max(28, usable / boardSize);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (board == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int sq = getSquareSize();
            int boardPx = sq * boardSize;
            int originX = (getWidth() - boardPx) / 2 + LABEL_MARGIN / 2;
            int originY = (getHeight() - boardPx) / 2 + LABEL_MARGIN / 2;

            // Coordinate labels
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < boardSize; i++) {
                String label = String.valueOf(i);
                int colX = originX + i * sq + (sq - fm.stringWidth(label)) / 2;
                g2.drawString(label, colX, originY - 8);

                int rowY = originY + i * sq + (sq + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(label, originX - fm.stringWidth(label) - 10, rowY);
            }

            // Board outer frame
            g2.setColor(new Color(10, 12, 18));
            g2.fillRoundRect(originX - 5, originY - 5, boardPx + 10, boardPx + 10, 10, 10);

            // Squares & pieces
            for (int r = 0; r < boardSize; r++) {
                for (int c = 0; c < boardSize; c++) {
                    int x = originX + c * sq;
                    int y = originY + r * sq;

                    boolean isLight = (r + c) % 2 == 0;
                    g2.setColor(isLight ? LIGHT_SQUARE : DARK_SQUARE);
                    g2.fillRect(x, y, sq, sq);

                    if (showDangerZones && !board.hasQueen(r, c) && !board.isSafe(r, c)) {
                        g2.setColor(DANGER_TINT);
                        g2.fillRect(x, y, sq, sq);
                    }

                    if (hintCell != null && hintCell.getRow() == r && hintCell.getCol() == c) {
                        g2.setColor(HINT_COLOR);
                        g2.fillRect(x, y, sq, sq);
                    }

                    if (flashCell != null && flashCell.getRow() == r && flashCell.getCol() == c && flashColor != null) {
                        g2.setColor(flashColor);
                        g2.fillRect(x, y, sq, sq);
                    }

                    if (attackerCell != null && attackerCell.getRow() == r && attackerCell.getCol() == c) {
                        g2.setColor(CONFLICT_COLOR);
                        g2.fillRect(x, y, sq, sq);
                    }

                    if (board.hasQueen(r, c)) {
                        drawQueenPiece(g2, x, y, sq);
                    }
                }
            }

            // Draw glowing red attack line when a player loses a life
            if (attackerCell != null && flashCell != null) {
                int x1 = originX + attackerCell.getCol() * sq + sq / 2;
                int y1 = originY + attackerCell.getRow() * sq + sq / 2;
                int x2 = originX + flashCell.getCol() * sq + sq / 2;
                int y2 = originY + flashCell.getRow() * sq + sq / 2;
                g2.setColor(new Color(239, 68, 68, 220));
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x1, y1, x2, y2);
            }

            g2.dispose();
        }

        private void drawQueenPiece(Graphics2D g2, int x, int y, int sq) {
            int pad = Math.max(6, sq / 6);
            int left = x + pad;
            int right = x + sq - pad;
            int top = y + pad + sq / 10;
            int bottom = y + sq - pad;
            int width = right - left;
            int height = bottom - top;
            int midX = x + sq / 2;

            Polygon crown = new Polygon();
            crown.addPoint(left + width / 8, bottom - height / 5);
            crown.addPoint(left, top + height / 5);
            crown.addPoint(left + width / 4, top + height / 2);
            crown.addPoint(midX, top);
            crown.addPoint(right - width / 4, top + height / 2);
            crown.addPoint(right, top + height / 5);
            crown.addPoint(right - width / 8, bottom - height / 5);

            g2.setColor(new Color(0, 0, 0, 70));
            g2.translate(2, 2);
            g2.fillPolygon(crown);
            g2.fillRoundRect(left + width / 10, bottom - height / 5, width - width / 5, height / 5, 6, 6);
            g2.translate(-2, -2);

            g2.setColor(new Color(28, 34, 48));
            g2.fillPolygon(crown);
            g2.fillRoundRect(left + width / 10, bottom - height / 5, width - width / 5, height / 5, 6, 6);

            g2.setColor(ACCENT_GOLD);
            g2.setStroke(new BasicStroke(Math.max(1.5f, sq / 32f)));
            g2.drawPolygon(crown);
            g2.drawRoundRect(left + width / 10, bottom - height / 5, width - width / 5, height / 5, 6, 6);

            int dotR = Math.max(2, sq / 16);
            g2.fillOval(left - dotR, top + height / 5 - dotR, dotR * 2, dotR * 2);
            g2.fillOval(midX - dotR, top - dotR, dotR * 2, dotR * 2);
            g2.fillOval(right - dotR, top + height / 5 - dotR, dotR * 2, dotR * 2);
        }
    }
}
