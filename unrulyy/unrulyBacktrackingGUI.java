package unrulyy;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class unrulyBacktrackingGUI extends JFrame {

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

    private boolean enforceUnique = true; // This enables unique rows/columns rule
    private String customDifficulty = null;
    private boolean useCustomDifficulty = false;

    // AI Control
    private Timer aiTimer;
    private int moveDelay = 500;
    private int moveCount = 0;
    private int totalAttemptedMoves = 0; // NEW: Track all backtracking attempts
    private boolean aiRunning = false;
    private JButton startAIButton;
    private JButton pauseAIButton;
    private JButton stepAIButton;
    private long startTimeMillis = 0L;
    
    // For move tracking
    private Stack<Move> moveHistory = new Stack<>();
    
    // Backtracking state
    private boolean solving = false;
    private String[][] solutionBoard = null;
    
    // Menu items
    private JMenuItem newGameMenuItem;
    private JMenuItem restartMenuItem;
    private JMenuItem solveNowMenuItem;
    private JCheckBoxMenuItem uniqueRowsColumnsMenuItem;
    
    private static class Move {
        int r, c;
        String oldVal, newVal;
        Move(int r, int c, String oldVal, String newVal) {
            this.r = r; this.c = c; this.oldVal = oldVal; this.newVal = newVal;
        }
    }

    public unrulyBacktrackingGUI() {
        super("Unruly — Pure Backtracking AI");

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
        
        // Game Menu
        JMenu gameMenu = new JMenu("Game");
        gameMenu.setMnemonic(KeyEvent.VK_G);
        
        newGameMenuItem = new JMenuItem("New Game", KeyEvent.VK_N);
        newGameMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newGameMenuItem.addActionListener(e -> {
            stopAI();
            moveCount = 0;
            totalAttemptedMoves = 0; // Reset total attempts
            startGame();
        });
        gameMenu.add(newGameMenuItem);
        
        restartMenuItem = new JMenuItem("Restart", KeyEvent.VK_R);
        restartMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        restartMenuItem.addActionListener(e -> {
            if (initialBoard == null) return;
            stopAI();
            resetToInitial();
        });
        gameMenu.add(restartMenuItem);
        
        solveNowMenuItem = new JMenuItem("Solve Now", KeyEvent.VK_S);
        solveNowMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        solveNowMenuItem.addActionListener(e -> {
            stopAI();
            solveInstantly();
        });
        gameMenu.add(solveNowMenuItem);
        
        gameMenu.addSeparator();
        
        // Add Unique Rows/Columns toggle
        uniqueRowsColumnsMenuItem = new JCheckBoxMenuItem("Enforce Unique Rows/Columns");
        uniqueRowsColumnsMenuItem.setSelected(enforceUnique);
        uniqueRowsColumnsMenuItem.addActionListener(e -> {
            enforceUnique = uniqueRowsColumnsMenuItem.isSelected();
            // Restart game with new setting
            stopAI();
            moveCount = 0;
            totalAttemptedMoves = 0;
            startGame();
        });
        gameMenu.add(uniqueRowsColumnsMenuItem);
        
        gameMenu.addSeparator();
        
        JMenuItem exitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitMenuItem.addActionListener(e -> System.exit(0));
        gameMenu.add(exitMenuItem);
        
        menuBar.add(gameMenu);
        
        // Type Menu
        JMenu typeMenu = new JMenu("Type");
        typeMenu.setMnemonic(KeyEvent.VK_T);
        
        addTypeMenuItem(typeMenu, "6x6 Easy", 6, 6);
        addTypeMenuItem(typeMenu, "6x6 Normal", 6, 6);
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

        menuBar.add(typeMenu);
        
        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        
        JMenuItem aboutMenuItem = new JMenuItem("About", KeyEvent.VK_A);
        aboutMenuItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutMenuItem);
        
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }

    private void setupTopPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        top.setBackground(new Color(40, 40, 40));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Unruly Pure Backtracking AI");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        top.add(title);

        levelBox = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        styleCombo(levelBox);
        levelBox.addActionListener(e -> useCustomDifficulty = false);
        top.add(levelBox);

        // Add New Game button
        JButton newGameBtn = new JButton("New Game");
        styleButton(newGameBtn);
        newGameBtn.setBackground(new Color(46, 125, 50));
        newGameBtn.addActionListener(e -> {
            stopAI();
            moveCount = 0;
            totalAttemptedMoves = 0;
            startGame();
        });
        top.add(newGameBtn);
        
        // Add Restart button
        JButton restartBtn = new JButton("Restart");
        styleButton(restartBtn);
        restartBtn.setBackground(new Color(255, 160, 0));
        restartBtn.addActionListener(e -> {
            if (initialBoard == null) return;
            stopAI();
            resetToInitial();
        });
        top.add(restartBtn);
        
        // Add Solve Now button
        JButton solveNowBtn = new JButton("Solve Now");
        styleButton(solveNowBtn);
        solveNowBtn.setBackground(new Color(198, 40, 40));
        solveNowBtn.addActionListener(e -> {
            stopAI();
            solveInstantly();
        });
        top.add(solveNowBtn);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        top.add(statusLabel);

        moveCountLabel = new JLabel("Moves: 0 (Attempts: 0)");
        moveCountLabel.setForeground(Color.WHITE);
        moveCountLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        top.add(moveCountLabel);

        add(top, BorderLayout.NORTH);
    }

    private void setupAIPanel() {
        JPanel aiControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        aiControlPanel.setBackground(new Color(40, 40, 40));

        startAIButton = new JButton("▶ Start Backtracking");
        styleButton(startAIButton);
        startAIButton.setBackground(new Color(46, 125, 50));
        startAIButton.addActionListener(e -> startAI());

        pauseAIButton = new JButton("⏸ Pause");
        styleButton(pauseAIButton);
        pauseAIButton.setBackground(new Color(198, 40, 40));
        pauseAIButton.setEnabled(false);
        pauseAIButton.addActionListener(e -> pauseAI());

        stepAIButton = new JButton("⏩ Step");
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

        // Add rule indicator
        JLabel rulesLabel = new JLabel("Rules: Equal B/W | No 3-in-row | Unique rows/cols");
        rulesLabel.setForeground(new Color(255, 200, 100));
        rulesLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        aiControlPanel.add(startAIButton);
        aiControlPanel.add(pauseAIButton);
        aiControlPanel.add(stepAIButton);
        aiControlPanel.add(speedLabel);
        aiControlPanel.add(speedSlider);
        aiControlPanel.add(rulesLabel);

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
            totalAttemptedMoves = 0;
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
            uniqueRowsColumnsMenuItem.setSelected(enforceUnique);
            customDifficulty = (String) diffBox.getSelectedItem();
            useCustomDifficulty = true;

            stopAI();
            moveCount = 0;
            totalAttemptedMoves = 0;
            startGame();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid integer for width/height.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAboutDialog() {
        String message = "Unruly Puzzle Solver\n" +
                        "Version 1.0\n\n" +
                        "Game Rules:\n" +
                        "• Each row and column must have equal numbers of BLACK and WHITE\n" +
                        "• No three consecutive same colors horizontally or vertically\n" +
                        "• All rows must be unique (no two identical rows)\n" +
                        "• All columns must be unique (no two identical columns)\n\n" +
                        "Controls:\n" +
                        "• New Game: Start a new puzzle\n" +
                        "• Restart: Reset to initial state\n" +
                        "• Solve Now: Find solution immediately\n" +
                        "• Start AI: Begin automated solving\n" +
                        "• Step: Make one move at a time\n\n" +
                        "Move Tracking:\n" +
                        "• Moves: Cells changed on the board (solution path)\n" +
                        "• Attempts: Total backtracking attempts during solving";
        
        JOptionPane.showMessageDialog(this, message, "About Unruly AI", JOptionPane.INFORMATION_MESSAGE);
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

    private void startGame() {
        board = new String[rows][cols];
        prefilled = new boolean[rows][cols];
        initialBoard = new String[rows][cols];

        // Initialize empty board
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                board[r][c] = EMPTY;

        // Generate solved board using pure backtracking with ALL rules
        String[][] solved = generateSolvedBoardWithBacktracking();
        
        // Copy solved board to initial
        for (int r = 0; r < rows; r++) {
            System.arraycopy(solved[r], 0, board[r], 0, cols);
            System.arraycopy(solved[r], 0, initialBoard[r], 0, cols);
        }

        // Mark all as prefilled initially
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                prefilled[r][c] = true;

        // Remove cells based on difficulty
        String diff = useCustomDifficulty && customDifficulty != null ? customDifficulty : (String) levelBox.getSelectedItem();
        
        double minRatio, maxRatio;
        switch (diff) {
            case "Trivial":
                minRatio = 0.50;
                maxRatio = 0.60;
                break;
            case "Easy":
                minRatio = 0.40;
                maxRatio = 0.50;
                break;
            case "Normal":
            case "Medium":
                minRatio = 0.30;
                maxRatio = 0.40;
                break;
            default: // Hard
                minRatio = 0.20;
                maxRatio = 0.30;
        }
        
        Random rand = new Random();
        double ratio = minRatio + (maxRatio - minRatio) * rand.nextDouble();
        
        int total = rows * cols;
        int keep = (int) (total * ratio);
        int remove = total - keep;
        
        if (keep < 2) keep = 2;
        if (keep > total - 2) keep = total - 2;
        remove = total - keep;
        
        // Remove random cells
        while (remove > 0) {
            int r = rand.nextInt(rows);
            int c = rand.nextInt(cols);
            if (!board[r][c].equals(EMPTY)) {
                board[r][c] = EMPTY;
                prefilled[r][c] = false;
                remove--;
            }
        }

        // Update initial board state
        for (int r = 0; r < rows; r++)
            System.arraycopy(board[r], 0, initialBoard[r], 0, cols);

        moveHistory.clear();
        moveCount = 0;
        totalAttemptedMoves = 0; // Reset total attempts
        startTimeMillis = System.currentTimeMillis();
        solutionBoard = null;
        solving = false;
        
        // Enable/disable menu items
        restartMenuItem.setEnabled(true);
        solveNowMenuItem.setEnabled(true);
        
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
        totalAttemptedMoves = 0; // Reset total attempts
        startTimeMillis = System.currentTimeMillis();
        solutionBoard = null;
        solving = false;
        updateStatus();
        boardPanel.repaint();
        
        // Show confirmation
        statusLabel.setText("Game restarted");
    }

    private void solveInstantly() {
        // Reset counters
        moveCount = 0;
        totalAttemptedMoves = 0;
        
        // Disable solve button during solving
        solveNowMenuItem.setEnabled(false);
        stepAIButton.setEnabled(false);
        startAIButton.setEnabled(false);
        
        statusLabel.setText("Solving...");
        
        // Use pure backtracking to find solution in background
        String[][] currentBoard = new String[rows][cols];
        for (int r = 0; r < rows; r++)
            System.arraycopy(board[r], 0, currentBoard[r], 0, cols);
        
        // Run backtracking in separate thread
        new Thread(() -> {
            boolean found = backtrackSolve(currentBoard, 0);
            
            SwingUtilities.invokeLater(() -> {
                if (found) {
                    board = currentBoard;
                    
                    // Mark all cells as filled
                    prefilled = new boolean[rows][cols];
                    for (int r = 0; r < rows; r++)
                        for (int c = 0; c < cols; c++)
                            prefilled[r][c] = true;
                    
                    initialBoard = new String[rows][cols];
                    for (int r = 0; r < rows; r++)
                        System.arraycopy(currentBoard[r], 0, initialBoard[r], 0, cols);
                    
                    moveHistory.clear();
                    boardPanel.repaint();
                    
                    // Show total attempted moves in victory message
                    String message = "Solution found!\n" +
                                    "Total attempted moves during backtracking: " + totalAttemptedMoves + "\n" +
                                    "Final board moves applied: " + moveCount;
                    JOptionPane.showMessageDialog(unrulyBacktrackingGUI.this, 
                        message, 
                        "Solving Complete", 
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    showVictoryMessage();
                } else {
                    JOptionPane.showMessageDialog(unrulyBacktrackingGUI.this, 
                        "No solution found! The puzzle might be unsolvable.\n" +
                        "Total attempted moves: " + totalAttemptedMoves, 
                        "Solving Failed", 
                        JOptionPane.ERROR_MESSAGE);
                }
                
                // Re-enable buttons
                solveNowMenuItem.setEnabled(true);
                stepAIButton.setEnabled(true);
                startAIButton.setEnabled(true);
                updateStatus();
            });
        }).start();
    }

    // ======================= PURE BACKTRACKING AI ========================
    
    private void startAI() {
        if (aiRunning) return;
        if (isBoardSolved()) {
            showVictoryMessage();
            return;
        }
        
        // Reset move counters
        moveCount = 0;
        totalAttemptedMoves = 0;
        
        // Disable menu items during AI operation
        newGameMenuItem.setEnabled(false);
        restartMenuItem.setEnabled(false);
        solveNowMenuItem.setEnabled(false);
        
        // Precompute solution using pure backtracking
        solutionBoard = new String[rows][cols];
        for (int r = 0; r < rows; r++)
            System.arraycopy(board[r], 0, solutionBoard[r], 0, cols);
        
        solving = true;
        updateStatus();
        
        // Start backtracking in a separate thread to avoid UI freezing
        new Thread(() -> {
            boolean found = backtrackSolve(solutionBoard, 0);
            
            SwingUtilities.invokeLater(() -> {
                solving = false;
                if (found) {
                    aiRunning = true;
                    startAIButton.setEnabled(false);
                    pauseAIButton.setEnabled(true);
                    stepAIButton.setEnabled(false);
                    
                    aiTimer = new Timer(moveDelay, e -> {
                        if (!aiRunning) return;
                        makeNextMoveFromSolution();
                    });
                    aiTimer.start();
                    updateStatus();
                    
                    // Show total attempted moves in console
                    System.out.println("Total attempted moves during backtracking: " + totalAttemptedMoves);
                } else {
                    JOptionPane.showMessageDialog(unrulyBacktrackingGUI.this, 
                        "No solution exists for this puzzle!\n" +
                        "Total attempted moves: " + totalAttemptedMoves, 
                        "Unsolvable", 
                        JOptionPane.ERROR_MESSAGE);
                    solutionBoard = null;
                    
                    // Re-enable menu items
                    newGameMenuItem.setEnabled(true);
                    restartMenuItem.setEnabled(true);
                    solveNowMenuItem.setEnabled(true);
                    
                    updateStatus();
                }
            });
        }).start();
    }

    private void pauseAI() {
        aiRunning = false;
        if (aiTimer != null) {
            aiTimer.stop();
        }
        startAIButton.setEnabled(true);
        pauseAIButton.setEnabled(false);
        stepAIButton.setEnabled(true);
        
        // Re-enable menu items
        newGameMenuItem.setEnabled(true);
        restartMenuItem.setEnabled(true);
        solveNowMenuItem.setEnabled(true);
        
        updateStatus();
    }

    private void stopAI() {
        aiRunning = false;
        solving = false;
        if (aiTimer != null) {
            aiTimer.stop();
        }
        startAIButton.setEnabled(true);
        pauseAIButton.setEnabled(false);
        stepAIButton.setEnabled(true);
        
        // Re-enable menu items
        newGameMenuItem.setEnabled(true);
        restartMenuItem.setEnabled(true);
        solveNowMenuItem.setEnabled(true);
    }

    private void stepAI() {
        if (isBoardSolved()) {
            showVictoryMessage();
            return;
        }
        
        // Reset counters for new solving session
        moveCount = 0;
        totalAttemptedMoves = 0;
        
        // Disable step button during solving
        stepAIButton.setEnabled(false);
        
        if (solutionBoard == null) {
            // Compute solution on first step
            solutionBoard = new String[rows][cols];
            for (int r = 0; r < rows; r++)
                System.arraycopy(board[r], 0, solutionBoard[r], 0, cols);
            
            solving = true;
            updateStatus();
            
            new Thread(() -> {
                boolean found = backtrackSolve(solutionBoard, 0);
                SwingUtilities.invokeLater(() -> {
                    solving = false;
                    stepAIButton.setEnabled(true);
                    if (found) {
                        makeNextMoveFromSolution();
                        
                        // Show total attempted moves
                        System.out.println("Total attempted moves during backtracking: " + totalAttemptedMoves);
                    } else {
                        JOptionPane.showMessageDialog(unrulyBacktrackingGUI.this, 
                            "No solution exists!\n" +
                            "Total attempted moves: " + totalAttemptedMoves, 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                        solutionBoard = null;
                    }
                    updateStatus();
                });
            }).start();
        } else {
            makeNextMoveFromSolution();
            stepAIButton.setEnabled(true);
        }
    }

    private void makeNextMoveFromSolution() {
        if (solutionBoard == null) return;
        
        // Find first cell where current board differs from solution
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].equals(EMPTY) && !solutionBoard[r][c].equals(EMPTY)) {
                    String oldVal = board[r][c];
                    String newVal = solutionBoard[r][c];
                    
                    board[r][c] = newVal;
                    moveHistory.push(new Move(r, c, oldVal, newVal));
                    moveCount++;
                    
                    updateStatus();
                    boardPanel.repaint();
                    
                    if (isBoardSolved()) {
                        stopAI();
                        showVictoryMessage();
                    }
                    return;
                }
            }
        }
    }

    /**
     * Pure recursive backtracking solver - includes UNIQUE ROWS/COLUMNS rule
     * MODIFIED: Counts all attempted moves
     */
    private boolean backtrackSolve(String[][] grid, int pos) {
        if (pos == rows * cols) {
            // Check if board is completely valid with ALL rules
            return isBoardFullyValid(grid);
        }
        
        int r = pos / cols;
        int c = pos % cols;
        
        // Skip if cell is already filled
        if (!grid[r][c].equals(EMPTY)) {
            return backtrackSolve(grid, pos + 1);
        }
        
        // Try BLACK first, then WHITE
        String[] colors = {BLACK, WHITE};
        
        for (String color : colors) {
            // Count this as an attempted move
            totalAttemptedMoves++;
            
            grid[r][c] = color;
            
            // Quick check for immediate violations (three in a row)
            if (!hasImmediateViolation(grid, r, c)) {
                // Also check if this placement maintains row/column balance
                if (maintainsBalance(grid, r, c)) {
                    if (backtrackSolve(grid, pos + 1)) {
                        return true;
                    }
                }
            }
            
            grid[r][c] = EMPTY;
        }
        
        return false;
    }

    /**
     * Check if current placement maintains row and column balance
     */
    private boolean maintainsBalance(String[][] grid, int r, int c) {
        // Check row balance
        int blackRow = 0, whiteRow = 0;
        for (int col = 0; col < cols; col++) {
            if (grid[r][col].equals(BLACK)) blackRow++;
            else if (grid[r][col].equals(WHITE)) whiteRow++;
        }
        int half = cols / 2;
        if (blackRow > half || whiteRow > half) return false;
        
        // Check column balance
        int blackCol = 0, whiteCol = 0;
        for (int row = 0; row < rows; row++) {
            if (grid[row][c].equals(BLACK)) blackCol++;
            else if (grid[row][c].equals(WHITE)) whiteCol++;
        }
        half = rows / 2;
        if (blackCol > half || whiteCol > half) return false;
        
        return true;
    }

    /**
     * Generate solved board using pure backtracking with ALL rules
     * MODIFIED: Counts generation attempts
     */
    private String[][] generateSolvedBoardWithBacktracking() {
        String[][] grid = new String[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c] = EMPTY;
        
        if (backtrackGenerate(grid, 0)) {
            return grid;
        }
        
        // Fallback to alternating pattern if backtracking fails
        // Note: This fallback may not satisfy unique rows/columns rule
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = ((r + c) % 2 == 0) ? BLACK : WHITE;
            }
        }
        
        return grid;
    }

    /**
     * Backtracking to generate a valid board with ALL rules
     * MODIFIED: Counts all attempted moves during generation
     */
    private boolean backtrackGenerate(String[][] grid, int pos) {
        if (pos == rows * cols) {
            // Check if board is valid with all rules
            if (!isBoardFullyValid(grid)) return false;
            
            // If unique rows/columns is enabled, verify that
            if (enforceUnique) {
                return hasUniqueRowsAndColumns(grid);
            }
            return true;
        }
        
        int r = pos / cols;
        int c = pos % cols;
        
        // Try both colors
        for (String color : new String[]{BLACK, WHITE}) {
            // Count generation attempts too
            totalAttemptedMoves++;
            
            grid[r][c] = color;
            
            if (!hasImmediateViolation(grid, r, c) && maintainsBalance(grid, r, c)) {
                if (backtrackGenerate(grid, pos + 1)) {
                    return true;
                }
            }
            
            grid[r][c] = EMPTY;
        }
        
        return false;
    }

    /**
     * Check for three in a row immediately after placement
     */
    private boolean hasImmediateViolation(String[][] grid, int r, int c) {
        String color = grid[r][c];
        if (color.equals(EMPTY)) return false;
        
        // Check horizontal three in a row
        if (c >= 2 && grid[r][c-1].equals(color) && grid[r][c-2].equals(color))
            return true;
        if (c <= cols-3 && grid[r][c+1].equals(color) && grid[r][c+2].equals(color))
            return true;
        if (c > 0 && c < cols-1 && grid[r][c-1].equals(color) && grid[r][c+1].equals(color))
            return true;
        
        // Check vertical three in a row
        if (r >= 2 && grid[r-1][c].equals(color) && grid[r-2][c].equals(color))
            return true;
        if (r <= rows-3 && grid[r+1][c].equals(color) && grid[r+2][c].equals(color))
            return true;
        if (r > 0 && r < rows-1 && grid[r-1][c].equals(color) && grid[r+1][c].equals(color))
            return true;
        
        return false;
    }

    // ======================= VALIDATION ========================
    
    private boolean isRowValid(String[] row) {
        // Check for three in a row
        for (int i = 0; i < row.length - 2; i++) {
            if (!row[i].equals(EMPTY) &&
                row[i].equals(row[i + 1]) &&
                row[i].equals(row[i + 2])) {
                return false;
            }
        }

        // Check balance if row is full
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

    private boolean isColumnValid(String[][] grid, int c) {
        String[] col = new String[rows];
        for (int r = 0; r < rows; r++) col[r] = grid[r][c];
        return isRowValid(col);
    }

    /**
     * Check if all rows are unique
     */
    private boolean hasUniqueRows(String[][] grid) {
        Set<String> rowPatterns = new HashSet<>();
        
        for (int r = 0; r < rows; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                sb.append(grid[r][c]);
            }
            String rowStr = sb.toString();
            if (rowPatterns.contains(rowStr)) {
                return false; // Duplicate row found
            }
            rowPatterns.add(rowStr);
        }
        
        return true;
    }

    /**
     * Check if all columns are unique
     */
    private boolean hasUniqueColumns(String[][] grid) {
        Set<String> colPatterns = new HashSet<>();
        
        for (int c = 0; c < cols; c++) {
            StringBuilder sb = new StringBuilder();
            for (int r = 0; r < rows; r++) {
                sb.append(grid[r][c]);
            }
            String colStr = sb.toString();
            if (colPatterns.contains(colStr)) {
                return false; // Duplicate column found
            }
            colPatterns.add(colStr);
        }
        
        return true;
    }

    /**
     * Check if both rows and columns are unique
     */
    private boolean hasUniqueRowsAndColumns(String[][] grid) {
        return hasUniqueRows(grid) && hasUniqueColumns(grid);
    }

    private boolean isBoardFullyValid(String[][] grid) {
        // Check all rows and columns for basic rules
        for (int r = 0; r < rows; r++) {
            if (!isRowValid(grid[r])) return false;
        }
        for (int c = 0; c < cols; c++) {
            if (!isColumnValid(grid, c)) return false;
        }
        
        // Check for uniqueness if enabled
        if (enforceUnique) {
            if (!hasUniqueRowsAndColumns(grid)) return false;
        }
        
        return true;
    }

    private boolean isBoardSolved() {
        // Check if all cells are filled
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (board[r][c].equals(EMPTY)) return false;
        
        return isBoardFullyValid(board);
    }

    private void showVictoryMessage() {
        String timeText = getElapsedTimeText();
        String uniquenessMessage = enforceUnique ? "✓ All rows and columns are unique" : "✓ Unique rows/columns not enforced";
        
        JOptionPane.showMessageDialog(
                this,
                "AI successfully completed the puzzle!\n" +
                timeText + "\n" +
                "Moves applied to board: " + moveCount + "\n" +
                "Total attempted moves during solving: " + totalAttemptedMoves + "\n" +
                uniquenessMessage,
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

            // Draw grid and cells
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
                    
                    // Prefilled indicator
                    if (prefilled[r][c]) {
                        g2.setColor(new Color(100, 100, 255, 100));
                        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
                        g2.drawRect(x + 3, y + 3, tileW - 6, tileH - 6);
                        g2.setStroke(new BasicStroke(1));
                    }
                }
            }
            
            // Draw AI mode indicator and rule status
            g2.setColor(new Color(255, 255, 255, 50));
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            FontMetrics fm = g2.getFontMetrics();
            String text = "PURE BACKTRACKING";
            int textWidth = fm.stringWidth(text);
            g2.drawString(text, (w - textWidth) / 2, h / 2 - 20);
            
            if (enforceUnique) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                fm = g2.getFontMetrics();
                text = "Unique Rows/Columns Enforced";
                textWidth = fm.stringWidth(text);
                g2.setColor(new Color(100, 255, 100, 50));
                g2.drawString(text, (w - textWidth) / 2, h / 2 + 20);
            }
        }
    }

    private void updateStatus() {
        if (isBoardSolved()) {
            statusLabel.setText("Solved! " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount + " (Attempts: " + totalAttemptedMoves + ") ✓");
        } else if (aiRunning) {
            statusLabel.setText("Backtracking AI running... " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount + " (Attempts: " + totalAttemptedMoves + ")");
        } else if (solving) {
            statusLabel.setText("Computing solution... " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount + " (Attempts: " + totalAttemptedMoves + ")");
        } else {
            statusLabel.setText("AI paused " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount + " (Attempts: " + totalAttemptedMoves + ")");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new unrulyBacktrackingGUI());
    }
}
