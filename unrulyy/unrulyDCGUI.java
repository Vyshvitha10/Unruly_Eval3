package unrulyy;
import javax.swing.*;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class unrulyDCGUI extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String EMPTY = ".";
    private static final String BLACK = "B";
    private static final String WHITE = "W";

    // rectangular board
    private int rows = 6;
    private int cols = 6;

    private String[][] board;
    private String[][] initialBoard;
    private boolean[][] prefilled;

    private JComboBox<String> levelBox;
    private BoardPanel boardPanel;
    private JLabel statusLabel;
    private JLabel moveCountLabel;

    private boolean enforceUnique = true;
    private String customDifficulty = null;
    private boolean useCustomDifficulty = false;

    // AI Control
    private Timer aiTimer;
    private Timer solveTimer;               // separate timer for "Solve now"
    private int moveDelay = 500;             // milliseconds between AI moves
    private int moveCount = 0;
    private boolean aiRunning = false;
    private JButton startAIButton;
    private JButton pauseAIButton;
    private JButton stepAIButton;
    private long startTimeMillis = 0L;

    // Computer move history (optional, for reference)
    private static class Move {
        int r, c;
        String oldVal, newVal;
        Move(int r, int c, String oldVal, String newVal) {
            this.r = r; this.c = c; this.oldVal = oldVal; this.newVal = newVal;
        }
    }
    private Stack<Move> moveHistory = new Stack<>();

    public unrulyDCGUI() {
        super("Unruly — Divide & Conquer AI");

        setupMenuBar();
        setupTopPanel();
        setupAIPanel();

        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        setSize(750, 850);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        startGame();
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        JMenu typeMenu = new JMenu("Type");
        menuBar.add(gameMenu);
        menuBar.add(typeMenu);

        JButton newGameBtn = new JButton("New game");
        styleButton(newGameBtn);
        newGameBtn.addActionListener(e -> {
            stopAI();
            moveCount = 0;
            startGame();
        });

        JButton restartBtn = new JButton("Restart");
        styleButton(restartBtn);
        restartBtn.addActionListener(e -> {
            if (initialBoard == null) return;
            stopAI();
            resetToInitial();
        });

        JButton solveBtn = new JButton("Solve now");
        styleButton(solveBtn);
        solveBtn.addActionListener(e -> {
            stopAI();                       // stop any running AI / solve timer
            solveInstantly();                // now uses moveDelay for visible steps
        });

        menuBar.add(newGameBtn);
        menuBar.add(restartBtn);
        menuBar.add(solveBtn);

        addTypeMenuItem(typeMenu, "8x8 Trivial", 8, 8);
        addTypeMenuItem(typeMenu, "8x8 Easy", 8, 8);
        addTypeMenuItem(typeMenu, "8x8 Normal", 8, 8);
        addTypeMenuItem(typeMenu, "10x10 Easy", 10, 10);
        addTypeMenuItem(typeMenu, "10x10 Normal", 10, 10);
        addTypeMenuItem(typeMenu, "14x14 Easy", 14, 14);
        addTypeMenuItem(typeMenu, "14x14 Normal", 14, 14);
        typeMenu.addSeparator();

        JMenuItem custom = new JMenuItem("Custom...");
        custom.addActionListener(e -> showCustomDialog());
        typeMenu.add(custom);

        setJMenuBar(menuBar);
    }

    private void setupTopPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        top.setBackground(new Color(40, 40, 40));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Unruly AI Auto-Player");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        top.add(title);

        levelBox = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        styleCombo(levelBox);
        levelBox.addActionListener(e -> useCustomDifficulty = false);
        top.add(levelBox);

        JButton startBtn = new JButton("New Game");
        styleButton(startBtn);
        startBtn.addActionListener(e -> {
            stopAI();
            moveCount = 0;
            startGame();
        });
        top.add(startBtn);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        top.add(statusLabel);

        moveCountLabel = new JLabel("Moves: 0");
        moveCountLabel.setForeground(Color.WHITE);
        moveCountLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        top.add(moveCountLabel);

        add(top, BorderLayout.NORTH);
    }

    private void setupAIPanel() {
        JPanel aiControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        aiControlPanel.setBackground(new Color(40, 40, 40));

        startAIButton = new JButton("▶ Start AI");
        styleButton(startAIButton);
        startAIButton.setBackground(new Color(46, 125, 50));
        startAIButton.addActionListener(e -> startAI());

        pauseAIButton = new JButton("⏸ Pause AI");
        styleButton(pauseAIButton);
        pauseAIButton.setBackground(new Color(198, 40, 40));
        pauseAIButton.setEnabled(false);
        pauseAIButton.addActionListener(e -> pauseAI());

        stepAIButton = new JButton("⏩ Step AI");
        styleButton(stepAIButton);
        stepAIButton.setBackground(new Color(255, 160, 0));
        stepAIButton.addActionListener(e -> stepAI());

        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 100, 2000, moveDelay);
        speedSlider.setBackground(new Color(40, 40, 40));
        speedSlider.setForeground(Color.WHITE);
        speedSlider.setMajorTickSpacing(500);
        speedSlider.setMinorTickSpacing(100);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.addChangeListener(e -> moveDelay = speedSlider.getValue());

        JLabel speedLabel = new JLabel("Speed (ms):");
        speedLabel.setForeground(Color.WHITE);

        aiControlPanel.add(startAIButton);
        aiControlPanel.add(pauseAIButton);
        aiControlPanel.add(stepAIButton);
        aiControlPanel.add(speedLabel);
        aiControlPanel.add(speedSlider);

        add(aiControlPanel, BorderLayout.SOUTH);
    }

    private void addTypeMenuItem(JMenu menu, String label, int r, int c) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> {
            rows = r;
            cols = c;
            useCustomDifficulty = false;
            stopAI();
            moveCount = 0;
            startGame();
        });
        menu.add(item);
    }

    private void showCustomDialog() {
        JTextField widthField = new JTextField(Integer.toString(cols), 6);
        JTextField heightField = new JTextField(Integer.toString(rows), 6);
        JCheckBox uniqueCheck = new JCheckBox("Unique rows and columns", enforceUnique);
        JComboBox<String> diffBox = new JComboBox<>(new String[]{"Trivial", "Easy", "Normal"});

        if (customDifficulty != null) diffBox.setSelectedItem(customDifficulty);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 6, 6, 6);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Width (columns)"), gbc);
        gbc.gridx = 1;
        panel.add(widthField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Height (rows)"), gbc);
        gbc.gridx = 1;
        panel.add(heightField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(uniqueCheck, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        panel.add(new JLabel("Difficulty"), gbc);
        gbc.gridx = 1;
        panel.add(diffBox, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Unruly configuration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            int w = Integer.parseInt(widthField.getText().trim());
            int h = Integer.parseInt(heightField.getText().trim());

            if (w < 4 || h < 4 || w > 40 || h > 40) {
                JOptionPane.showMessageDialog(this, "Width/height must be between 4 and 40.", "Invalid size", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if ((w % 2 != 0) || (h % 2 != 0)) {
                int ans = JOptionPane.showConfirmDialog(this, "Odd sizes may produce unsolvable puzzles. Continue anyway?", "Odd size", JOptionPane.YES_NO_OPTION);
                if (ans != JOptionPane.YES_OPTION) return;
            }

            cols = w;
            rows = h;
            enforceUnique = uniqueCheck.isSelected();
            customDifficulty = (String) diffBox.getSelectedItem();
            useCustomDifficulty = true;

            stopAI();
            moveCount = 0;
            startGame();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid integer for width/height.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void styleCombo(JComboBox<String> box) {
        box.setFont(new Font("SansSerif", Font.PLAIN, 16));
        box.setBackground(new Color(60, 60, 60));
        box.setForeground(Color.WHITE);
    }

    private void styleButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setBackground(new Color(70, 130, 180));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
    }

    // ======================= GAME INITIALIZATION ========================

    private void startGame() {
        board = new String[rows][cols];
        prefilled = new boolean[rows][cols];
        initialBoard = new String[rows][cols];

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                board[r][c] = EMPTY;

        String[][] solved = generateSolvedBoard();

        String diff = useCustomDifficulty && customDifficulty != null ? customDifficulty : (String) levelBox.getSelectedItem();

        // Use ratio ranges for variety
        double minRatio, maxRatio;
        switch (diff) {
            case "Trivial":
                minRatio = 0.50; maxRatio = 0.60; break;
            case "Easy":
                minRatio = 0.40; maxRatio = 0.50; break;
            case "Normal":
            case "Medium":
                minRatio = 0.30; maxRatio = 0.40; break;
            default:
                minRatio = 0.20; maxRatio = 0.30;
        }

        Random rand = new Random();
        double ratio = minRatio + (maxRatio - minRatio) * rand.nextDouble();
        int total = rows * cols;
        int keep = (int) (total * ratio);
        if (keep < 2) keep = 2;
        if (keep > total - 2) keep = total - 2;
        int remove = total - keep;

        for (int r = 0; r < rows; r++) {
            System.arraycopy(solved[r], 0, board[r], 0, cols);
            for (int c = 0; c < cols; c++)
                prefilled[r][c] = true;
        }

        while (remove > 0) {
            int r = rand.nextInt(rows);
            int c = rand.nextInt(cols);
            if (!board[r][c].equals(EMPTY)) {
                board[r][c] = EMPTY;
                prefilled[r][c] = false;
                remove--;
            }
        }

        for (int r = 0; r < rows; r++)
            System.arraycopy(board[r], 0, initialBoard[r], 0, cols);

        moveHistory.clear();
        moveCount = 0;
        startTimeMillis = System.currentTimeMillis();
        updateStatus();
        boardPanel.repaint();
    }

    private void resetToInitial() {
        board = new String[rows][cols];
        prefilled = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(initialBoard[r], 0, board[r], 0, cols);
            for (int c = 0; c < cols; c++)
                prefilled[r][c] = !board[r][c].equals(EMPTY);
        }
        moveHistory.clear();
        moveCount = 0;
        startTimeMillis = System.currentTimeMillis();
        updateStatus();
        boardPanel.repaint();
    }

    // ======================= SOLVE NOW (STEP BY STEP, VISIBLE) ========================
    private void solveInstantly() {
        stopAI(); // stop any running AI or previous solve timer
        if (isBoardSolved()) {
            showVictoryMessage();
            return;
        }
        // Use a timer with the current moveDelay (from slider) so steps are visible
        solveTimer = new Timer(moveDelay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isBoardSolved()) {
                    ((Timer) e.getSource()).stop();
                    showVictoryMessage();
                    return;
                }
                makeAIMove();
            }
        });
        solveTimer.setInitialDelay(0);
        solveTimer.start();
    }

    // ======================= AI CONTROL ========================

    private void startAI() {
        if (aiRunning) return;
        if (isBoardSolved()) {
            showVictoryMessage();
            return;
        }
        aiRunning = true;
        startAIButton.setEnabled(false);
        pauseAIButton.setEnabled(true);
        stepAIButton.setEnabled(false);

        aiTimer = new Timer(moveDelay, e -> {
            if (!aiRunning) return;
            if (isBoardSolved()) {
                stopAI();
                showVictoryMessage();
                return;
            }
            makeAIMove();
        });
        aiTimer.start();
        updateStatus();
    }

    private void pauseAI() {
        aiRunning = false;
        if (aiTimer != null) {
            aiTimer.stop();
        }
        startAIButton.setEnabled(true);
        pauseAIButton.setEnabled(false);
        stepAIButton.setEnabled(true);
        updateStatus();
    }

    private void stopAI() {
        aiRunning = false;
        if (aiTimer != null) {
            aiTimer.stop();
        }
        if (solveTimer != null) {
            solveTimer.stop();
        }
        startAIButton.setEnabled(true);
        pauseAIButton.setEnabled(false);
        stepAIButton.setEnabled(true);
    }

    private void stepAI() {
        if (isBoardSolved()) {
            showVictoryMessage();
            return;
        }
        makeAIMove();
    }

    // ======================= ROBUST MOVE SELECTION ========================

    /**
     * Make one AI move that is guaranteed to lead to a solution.
     */
    private void makeAIMove() {
        // Get all empty cells
        List<int[]> emptyCells = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].equals(EMPTY) && !prefilled[r][c]) {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }

        // Try each empty cell with both colors, in order of heuristic preference
        for (int[] cell : emptyCells) {
            int r = cell[0];
            int c = cell[1];
            // Determine preferred color order based on balance
            List<String> colors = getOrderedColorsHeuristic(r, c);
            for (String color : colors) {
                if (isValidPlace(board, r, c, color)) {
                    // Check if this move leads to a solution
                    if (isMovePartOfSolution(r, c, color)) {
                        // Make the move
                        String old = board[r][c];
                        board[r][c] = color;
                        moveHistory.push(new Move(r, c, old, color));
                        moveCount++;
                        boardPanel.repaint();
                        updateStatus();
                        if (isBoardSolved()) {
                            stopAI();
                            showVictoryMessage();
                        }
                        return;
                    }
                }
            }
        }

        // If no move found that leads to a solution, the board might be unsolvable.
        // Fallback: solve the entire board using backtracking now.
        if (solveRemaining(0)) {
            moveCount = Integer.MAX_VALUE; // indicate all filled at once
            boardPanel.repaint();
            updateStatus();
            if (isBoardSolved()) {
                stopAI();
                showVictoryMessage();
            }
        } else {
            // Truly stuck – this should not happen if puzzle was generated correctly
            stopAI();
            statusLabel.setText("AI stuck (unsolvable)!");
        }
    }

    /**
     * Check if making a move at (r,c) with color leads to a solvable board.
     */
    private boolean isMovePartOfSolution(int r, int c, String color) {
        // Create a deep copy of the board
        String[][] boardCopy = deepCopy(board);
        // Apply the move
        boardCopy[r][c] = color;
        // Run backtracking on the copy
        return solveRemainingOnBoard(boardCopy, 0);
    }

    /**
     * Backtracking solver that works on a given board (assumes prefilled cells are already set).
     */
    private boolean solveRemainingOnBoard(String[][] grid, int pos) {
        if (pos == rows * cols) {
            // Check all rules
            for (int r = 0; r < rows; r++)
                if (!isValidRow(grid[r])) return false;
            for (int c = 0; c < cols; c++)
                if (!isValidColumn(grid, c)) return false;
            if (enforceUnique) {
                if (!uniqueRows(grid) || !uniqueCols(grid)) return false;
            }
            return true;
        }
        int r = pos / cols;
        int c = pos % cols;
        if (!grid[r][c].equals(EMPTY)) {
            return solveRemainingOnBoard(grid, pos + 1);
        }
        for (String color : new String[]{BLACK, WHITE}) {
            if (isValidPlace(grid, r, c, color)) {
                grid[r][c] = color;
                if (solveRemainingOnBoard(grid, pos + 1)) return true;
                grid[r][c] = EMPTY;
            }
        }
        return false;
    }

    /**
     * Heuristic to order colors for a cell based on current row/column balance.
     */
    private List<String> getOrderedColorsHeuristic(int r, int c) {
        List<String> colors = new ArrayList<>();
        int blackRow = 0, whiteRow = 0;
        for (int col = 0; col < cols; col++) {
            if (board[r][col].equals(BLACK)) blackRow++;
            else if (board[r][col].equals(WHITE)) whiteRow++;
        }
        int blackCol = 0, whiteCol = 0;
        for (int row = 0; row < rows; row++) {
            if (board[row][c].equals(BLACK)) blackCol++;
            else if (board[row][c].equals(WHITE)) whiteCol++;
        }
        if (blackRow > whiteRow || blackCol > whiteCol) {
            colors.add(WHITE);
            colors.add(BLACK);
        } else if (whiteRow > blackRow || whiteCol > blackCol) {
            colors.add(BLACK);
            colors.add(WHITE);
        } else {
            colors.add(BLACK);
            colors.add(WHITE);
        }
        return colors;
    }

    // ======================= DIVIDE & CONQUER AI (still used for initial hint, but not critical now) ========================

    private class Quadrant {
        int r1, r2, c1, c2;
        int emptyCount;
        int priority;

        Quadrant(int r1, int r2, int c1, int c2, int priority) {
            this.r1 = r1;
            this.r2 = r2;
            this.c1 = c1;
            this.c2 = c2;
            this.priority = priority;
            this.emptyCount = countEmptyInRegion(r1, r2, c1, c2);
        }

        boolean isValid() {
            return r1 <= r2 && c1 <= c2 && r1 >= 0 && r2 < rows && c1 >= 0 && c2 < cols;
        }

        int getWidth() { return c2 - c1 + 1; }
        int getHeight() { return r2 - r1 + 1; }
    }

    private int countEmptyInRegion(int r1, int r2, int c1, int c2) {
        if (r1 > r2 || c1 > c2 || r1 < 0 || r2 >= rows || c1 < 0 || c2 >= cols) {
            return 0;
        }
        if (r1 == r2 && c1 == c2) {
            return (board[r1][c1].equals(EMPTY) && !prefilled[r1][c1]) ? 1 : 0;
        }
        int midRow = (r1 + r2) / 2;
        int midCol = (c1 + c2) / 2;
        int count = 0;
        count += countEmptyInRegion(r1, midRow, c1, midCol);
        if (midCol + 1 <= c2) {
            count += countEmptyInRegion(r1, midRow, midCol + 1, c2);
        }
        if (midRow + 1 <= r2) {
            count += countEmptyInRegion(midRow + 1, r2, c1, midCol);
        }
        if (midRow + 1 <= r2 && midCol + 1 <= c2) {
            count += countEmptyInRegion(midRow + 1, r2, midCol + 1, c2);
        }
        return count;
    }

    private List<Quadrant> getQuadrants(Quadrant region) {
        List<Quadrant> quadrants = new ArrayList<>();
        int midRow = (region.r1 + region.r2) / 2;
        int midCol = (region.c1 + region.c2) / 2;
        quadrants.add(new Quadrant(region.r1, midRow, region.c1, midCol, 1));
        quadrants.add(new Quadrant(region.r1, midRow, midCol + 1, region.c2, 2));
        quadrants.add(new Quadrant(midRow + 1, region.r2, region.c1, midCol, 3));
        quadrants.add(new Quadrant(midRow + 1, region.r2, midCol + 1, region.c2, 4));
        quadrants.removeIf(q -> !q.isValid());
        return quadrants;
    }

    private Quadrant chooseBestQuadrant(List<Quadrant> quadrants) {
        if (quadrants.isEmpty()) return null;
        Collections.sort(quadrants, (q1, q2) -> Integer.compare(q1.emptyCount, q2.emptyCount));
        List<Quadrant> bestQuadrants = new ArrayList<>();
        int minEmpty = quadrants.get(0).emptyCount;
        for (Quadrant q : quadrants) {
            if (q.emptyCount == minEmpty) {
                bestQuadrants.add(q);
            } else {
                break;
            }
        }
        if (bestQuadrants.size() > 1) {
            Collections.sort(bestQuadrants, (q1, q2) -> Integer.compare(q1.priority, q2.priority));
        }
        return bestQuadrants.get(0);
    }

    private int[] findBestMoveWithDivideConquer() {
        Quadrant wholeBoard = new Quadrant(0, rows - 1, 0, cols - 1, 1);
        return findBestCellInRegion(wholeBoard);
    }

    private int[] findBestCellInRegion(Quadrant region) {
        if (region.getWidth() == 1 && region.getHeight() == 1) {
            int r = region.r1;
            int c = region.c1;
            if (board[r][c].equals(EMPTY) && !prefilled[r][c]) {
                return new int[]{r, c};
            }
            return null;
        }
        List<Quadrant> quadrants = getQuadrants(region);
        quadrants.removeIf(q -> q.emptyCount == 0);
        if (quadrants.isEmpty()) {
            return null;
        }
        Quadrant bestQuadrant = chooseBestQuadrant(quadrants);
        return findBestCellInRegion(bestQuadrant);
    }

    private String chooseBestColorForCell(int r, int c) {
        List<String> validColors = new ArrayList<>();
        if (isValidPlace(board, r, c, BLACK) && isSafeComputerMove(r, c, BLACK)) {
            validColors.add(BLACK);
        }
        if (isValidPlace(board, r, c, WHITE) && isSafeComputerMove(r, c, WHITE)) {
            validColors.add(WHITE);
        }
        if (validColors.isEmpty()) {
            return null;
        }
        if (validColors.size() == 1) {
            return validColors.get(0);
        }
        // Both colors valid – use greedy heuristic
        String color1 = validColors.get(0);
        String color2 = validColors.get(1);
        int score1 = calculateCellScore(r, c, color1);
        int score2 = calculateCellScore(r, c, color2);
        if (score1 < score2) return color1;
        if (score2 < score1) return color2;
        return validColors.get(new Random().nextInt(validColors.size()));
    }

    private int calculateCellScore(int r, int c, String color) {
        int score = 0;
        int imbalance = imbalanceAfter(r, c, color);
        score += imbalance * 10;
        score += checkAdjacentThreats(r, c, color) * 5;
        int quadrantBalance = getQuadrantBalanceAfter(r, c, color);
        score -= quadrantBalance;
        return score;
    }

    private int checkAdjacentThreats(int r, int c, String color) {
        String opposite = color.equals(BLACK) ? WHITE : BLACK;
        int threats = 0;
        for (int dc = -2; dc <= 0; dc++) {
            int c1 = c + dc;
            int c2 = c + dc + 1;
            int c3 = c + dc + 2;
            if (c1 >= 0 && c2 >= 0 && c3 < cols) {
                int countOpposite = 0;
                if (c1 == c && color.equals(opposite)) countOpposite++;
                else if (c1 != c && board[r][c1].equals(opposite)) countOpposite++;
                if (c2 == c && color.equals(opposite)) countOpposite++;
                else if (c2 != c && board[r][c2].equals(opposite)) countOpposite++;
                if (c3 == c && color.equals(opposite)) countOpposite++;
                else if (c3 != c && board[r][c3].equals(opposite)) countOpposite++;
                if (countOpposite == 2) threats++;
            }
        }
        for (int dr = -2; dr <= 0; dr++) {
            int r1 = r + dr;
            int r2 = r + dr + 1;
            int r3 = r + dr + 2;
            if (r1 >= 0 && r2 >= 0 && r3 < rows) {
                int countOpposite = 0;
                if (r1 == r && color.equals(opposite)) countOpposite++;
                else if (r1 != r && board[r1][c].equals(opposite)) countOpposite++;
                if (r2 == r && color.equals(opposite)) countOpposite++;
                else if (r2 != r && board[r2][c].equals(opposite)) countOpposite++;
                if (r3 == r && color.equals(opposite)) countOpposite++;
                else if (r3 != r && board[r3][c].equals(opposite)) countOpposite++;
                if (countOpposite == 2) threats++;
            }
        }
        return threats;
    }

    private int getQuadrantBalanceAfter(int r, int c, String color) {
        int midRow = rows / 2;
        int midCol = cols / 2;
        int blackInQuadrant = 0;
        int whiteInQuadrant = 0;
        int r1, r2, c1, c2;
        if (r < midRow && c < midCol) {
            r1 = 0; r2 = midRow - 1; c1 = 0; c2 = midCol - 1;
        } else if (r < midRow && c >= midCol) {
            r1 = 0; r2 = midRow - 1; c1 = midCol; c2 = cols - 1;
        } else if (r >= midRow && c < midCol) {
            r1 = midRow; r2 = rows - 1; c1 = 0; c2 = midCol - 1;
        } else {
            r1 = midRow; r2 = rows - 1; c1 = midCol; c2 = cols - 1;
        }
        for (int rr = r1; rr <= r2; rr++) {
            for (int cc = c1; cc <= c2; cc++) {
                if (rr == r && cc == c) {
                    if (color.equals(BLACK)) blackInQuadrant++;
                    else whiteInQuadrant++;
                } else {
                    if (board[rr][cc].equals(BLACK)) blackInQuadrant++;
                    else if (board[rr][cc].equals(WHITE)) whiteInQuadrant++;
                }
            }
        }
        return -Math.abs(blackInQuadrant - whiteInQuadrant);
    }

    private int imbalanceAfter(int r, int c, String color) {
        int bRow = 0, wRow = 0;
        int bCol = 0, wCol = 0;
        for (int cc = 0; cc < cols; cc++) {
            String s = (cc == c) ? color : board[r][cc];
            if (s.equals(BLACK)) bRow++;
            else if (s.equals(WHITE)) wRow++;
        }
        for (int rr = 0; rr < rows; rr++) {
            String s = (rr == r) ? color : board[rr][c];
            if (s.equals(BLACK)) bCol++;
            else if (s.equals(WHITE)) wCol++;
        }
        return Math.abs(bRow - wRow) + Math.abs(bCol - wCol);
    }

    private int[] fallbackFindAnyMove() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].equals(EMPTY) && !prefilled[r][c]) {
                    if (isValidPlace(board, r, c, BLACK) || isValidPlace(board, r, c, WHITE)) {
                        return new int[]{r, c};
                    }
                }
            }
        }
        return null;
    }

    private String fallbackPickColor() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!board[r][c].equals(EMPTY) || prefilled[r][c]) continue;
                if (isValidPlace(board, r, c, BLACK) && isSafeComputerMove(r, c, BLACK)) {
                    return BLACK;
                }
                if (isValidPlace(board, r, c, WHITE) && isSafeComputerMove(r, c, WHITE)) {
                    return WHITE;
                }
            }
        }
        return null;
    }

    // ======================= BACKTRACKING FALLBACK (for entire board) ========================

    private boolean solveRemaining(int pos) {
        if (pos == rows * cols) {
            return true;
        }
        int r = pos / cols;
        int c = pos % cols;
        if (!board[r][c].equals(EMPTY) || prefilled[r][c]) {
            return solveRemaining(pos + 1);
        }
        for (String color : new String[]{BLACK, WHITE}) {
            if (isValidPlace(board, r, c, color)) {
                board[r][c] = color;
                if (solveRemaining(pos + 1)) return true;
                board[r][c] = EMPTY;
            }
        }
        return false;
    }

    // ======================= VALIDATION ========================

    private boolean isValidPlace(String[][] grid, int r, int c, String color) {
        // Check for three in a row horizontally
        if (c >= 2 && grid[r][c-1].equals(color) && grid[r][c-2].equals(color)) return false;
        if (c <= cols-3 && grid[r][c+1].equals(color) && grid[r][c+2].equals(color)) return false;
        if (c > 0 && c < cols-1 && grid[r][c-1].equals(color) && grid[r][c+1].equals(color)) return false;

        // Check vertically
        if (r >= 2 && grid[r-1][c].equals(color) && grid[r-2][c].equals(color)) return false;
        if (r <= rows-3 && grid[r+1][c].equals(color) && grid[r+2][c].equals(color)) return false;
        if (r > 0 && r < rows-1 && grid[r-1][c].equals(color) && grid[r+1][c].equals(color)) return false;

        // Row balance
        int blackRow = 0, whiteRow = 0;
        for (int col = 0; col < cols; col++) {
            if (grid[r][col].equals(BLACK)) blackRow++;
            else if (grid[r][col].equals(WHITE)) whiteRow++;
        }
        if (color.equals(BLACK)) blackRow++; else whiteRow++;
        int half = cols / 2;
        if (blackRow > half || whiteRow > half) return false;

        // Column balance
        int blackCol = 0, whiteCol = 0;
        for (int row = 0; row < rows; row++) {
            if (grid[row][c].equals(BLACK)) blackCol++;
            else if (grid[row][c].equals(WHITE)) whiteCol++;
        }
        if (color.equals(BLACK)) blackCol++; else whiteCol++;
        half = rows / 2;
        if (blackCol > half || whiteCol > half) return false;

        return true;
    }

    private boolean isSafeComputerMove(int r, int c, String color) {
        String old = board[r][c];
        board[r][c] = color;
        boolean ok = isBoardConsistent();
        board[r][c] = old;
        return ok;
    }

    private boolean isBoardConsistent() {
        for (int r = 0; r < rows; r++)
            if (!isValidRow(board[r])) return false;
        for (int c = 0; c < cols; c++)
            if (!isValidColumn(board, c)) return false;
        return true;
    }

    private boolean isValidRow(String[] row) {
        for (int i = 0; i < row.length - 2; i++) {
            if (!row[i].equals(EMPTY) &&
                row[i].equals(row[i + 1]) &&
                row[i].equals(row[i + 2])) {
                return false;
            }
        }
        int black = 0, white = 0, empty = 0;
        for (String s : row) {
            if (s.equals(BLACK)) black++;
            else if (s.equals(WHITE)) white++;
            else empty++;
        }
        int half = row.length / 2;
        if (black > half || white > half) return false;
        if (empty == 0 && black != white) return false;
        return true;
    }

    private boolean isValidColumn(String[][] grid, int c) {
        String[] col = new String[rows];
        for (int r = 0; r < rows; r++) col[r] = grid[r][c];
        return isValidRow(col);
    }

    private boolean uniqueRows(String[][] grid) {
        HashSet<String> set = new HashSet<>();
        for (int r = 0; r < rows; r++) {
            boolean full = true;
            for (int c = 0; c < cols; c++) if (grid[r][c].equals(EMPTY)) full = false;
            if (full) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < cols; c++) sb.append(grid[r][c]);
                if (!set.add(sb.toString())) return false;
            }
        }
        return true;
    }

    private boolean uniqueCols(String[][] grid) {
        HashSet<String> set = new HashSet<>();
        for (int c = 0; c < cols; c++) {
            boolean full = true; StringBuilder sb = new StringBuilder();
            for (int r = 0; r < rows; r++) {
                if (grid[r][c].equals(EMPTY)) full = false;
                sb.append(grid[r][c]);
            }
            if (full && !set.add(sb.toString())) return false;
        }
        return true;
    }

    private boolean isBoardSolved() {
        for (int r = 0; r < rows; r++)
            if (!isValidRow(board[r])) return false;
        for (int c = 0; c < cols; c++)
            if (!isValidColumn(board, c)) return false;
        if (enforceUnique) {
            if (!uniqueRows(board) || !uniqueCols(board)) return false;
        }
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (board[r][c].equals(EMPTY)) return false;
        return true;
    }

    private String[][] generateSolvedBoard() {
        String[][] solved = new String[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                solved[r][c] = EMPTY;

        // Use backtracking to generate a valid board
        if (backtrackGenerate(solved, 0)) {
            return solved;
        }
        // Fallback to alternating pattern
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                solved[r][c] = ((r + c) % 2 == 0) ? BLACK : WHITE;
        return solved;
    }

    private boolean backtrackGenerate(String[][] grid, int pos) {
        if (pos == rows * cols) {
            for (int r = 0; r < rows; r++)
                if (!isValidRow(grid[r])) return false;
            for (int c = 0; c < cols; c++)
                if (!isValidColumn(grid, c)) return false;
            if (enforceUnique) {
                if (!uniqueRows(grid) || !uniqueCols(grid)) return false;
            }
            return true;
        }
        int r = pos / cols;
        int c = pos % cols;
        for (String color : new String[]{BLACK, WHITE}) {
            grid[r][c] = color;
            if (hasImmediateViolation(grid, r, c)) {
                grid[r][c] = EMPTY;
                continue;
            }
            if (backtrackGenerate(grid, pos + 1)) {
                return true;
            }
            grid[r][c] = EMPTY;
        }
        return false;
    }

    private boolean hasImmediateViolation(String[][] grid, int r, int c) {
        if (c >= 2 && grid[r][c].equals(grid[r][c-1]) && grid[r][c].equals(grid[r][c-2]))
            return true;
        if (c <= cols-3 && grid[r][c].equals(grid[r][c+1]) && grid[r][c].equals(grid[r][c+2]))
            return true;
        if (r >= 2 && grid[r][c].equals(grid[r-1][c]) && grid[r][c].equals(grid[r-2][c]))
            return true;
        if (r <= rows-3 && grid[r][c].equals(grid[r+1][c]) && grid[r][c].equals(grid[r+2][c]))
            return true;
        return false;
    }

    // ======================= UI HELPERS ========================

    private void showVictoryMessage() {
        String timeText = getElapsedTimeText();
        JOptionPane.showMessageDialog(
                this,
                "AI successfully completed the puzzle!\n" +
                timeText + "\n" +
                "Total moves: " + (moveCount == Integer.MAX_VALUE ? "instant" : moveCount),
                "Puzzle Solved!",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private String getElapsedTimeText() {
        if (startTimeMillis == 0L) return "Time: 0:00";
        long elapsedMs = System.currentTimeMillis() - startTimeMillis;
        long totalSeconds = elapsedMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("Time: %d:%02d", minutes, seconds);
    }

    private void updateStatus() {
        if (isBoardSolved()) {
            statusLabel.setText("Solved! " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + (moveCount == Integer.MAX_VALUE ? "instant" : moveCount) + " ✓");
        } else if (aiRunning) {
            statusLabel.setText("AI thinking... " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount);
        } else {
            statusLabel.setText("AI paused " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount);
        }
    }

    // --------------------------- BOARD PANEL ------------------------------
    private class BoardPanel extends JPanel {
        public BoardPanel() {
            setBackground(new Color(30, 30, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(), h = getHeight();
            int tileW = Math.max(1, w / cols);
            int tileH = Math.max(1, h / rows);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int x = c * tileW;
                    int y = r * tileH;

                    // Cell background
                    g2.setColor(new Color(50, 50, 50));
                    g2.fillRect(x, y, tileW, tileH);

                    // Cell content
                    if (!board[r][c].equals(EMPTY)) {
                        g2.setColor(board[r][c].equals(BLACK) ? Color.BLACK : Color.WHITE);
                        g2.fillRect(x + 3, y + 3, tileW - 6, tileH - 6);
                    }

                    // Grid lines
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawRect(x, y, tileW, tileH);

                    // Prefilled indicator (dashed border)
                    if (prefilled[r][c]) {
                        g2.setColor(new Color(100, 100, 255, 100));
                        Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
                        g2.setStroke(dashed);
                        g2.drawRect(x + 3, y + 3, tileW - 6, tileH - 6);
                        g2.setStroke(new BasicStroke(1));
                    }
                }
            }

            // Draw AI mode watermark
            g2.setColor(new Color(255, 255, 255, 30));
            g2.setFont(new Font("SansSerif", Font.BOLD, 36));
            FontMetrics fm = g2.getFontMetrics();
            String text = "DIVIDE & CONQUER";
            int textWidth = fm.stringWidth(text);
            g2.drawString(text, (w - textWidth) / 2, h / 2);
        }
    }

    // Deep copy utility
    private String[][] deepCopy(String[][] src) {
        String[][] copy = new String[src.length][src[0].length];
        for (int r = 0; r < src.length; r++)
            System.arraycopy(src[r], 0, copy[r], 0, src[0].length);
        return copy;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new unrulyDCGUI());
    }
}