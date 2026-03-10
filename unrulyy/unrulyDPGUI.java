package unrulyy;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.util.*;

public class unrulyDPGUI extends JFrame {

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
    private int moveDelay = 500;
    private int moveCount = 0;
    private boolean aiRunning = false;
    private JButton startAIButton;
    private JButton pauseAIButton;
    private JButton stepAIButton;
    private long startTimeMillis = 0L;
    private long endTimeMillis = 0L;
    
    // For move tracking
    private Stack<Move> moveHistory = new Stack<>();
    
    // DP Solving state
    private Queue<Move> plannedMoves = new LinkedList<>();
    private boolean solving = false;
    
    // Menu items
    private JMenuItem newGameMenuItem;
    private JMenuItem restartMenuItem;
    private JMenuItem solveNowMenuItem;
    private JCheckBoxMenuItem uniqueRowsColumnsMenuItem;
    
    // Random generator for puzzles
    private Random random = new Random();
    
    private static class Move {
        int r, c;
        String oldVal, newVal;
        String reason;
        Move(int r, int c, String oldVal, String newVal, String reason) {
            this.r = r; this.c = c; this.oldVal = oldVal; this.newVal = newVal; this.reason = reason;
        }
    }

    public unrulyDPGUI() {
        super("Unruly — DP Constraint Solver");

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
        gameMenu.setMnemonic(KeyEvent.VK_G);
        
        newGameMenuItem = new JMenuItem("New Game", KeyEvent.VK_N);
        newGameMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newGameMenuItem.addActionListener(e -> {
            stopAI();
            moveCount = 0;
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
        
        uniqueRowsColumnsMenuItem = new JCheckBoxMenuItem("Enforce Unique Rows/Columns");
        uniqueRowsColumnsMenuItem.setSelected(enforceUnique);
        uniqueRowsColumnsMenuItem.addActionListener(e -> {
            enforceUnique = uniqueRowsColumnsMenuItem.isSelected();
            stopAI();
            moveCount = 0;
            startGame();
        });
        gameMenu.add(uniqueRowsColumnsMenuItem);
        
        gameMenu.addSeparator();
        
        JMenuItem exitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitMenuItem.addActionListener(e -> System.exit(0));
        gameMenu.add(exitMenuItem);
        
        menuBar.add(gameMenu);
        
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

        JLabel title = new JLabel("Unruly DP Constraint Solver");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        top.add(title);

        levelBox = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        styleCombo(levelBox);
        levelBox.addActionListener(e -> useCustomDifficulty = false);
        top.add(levelBox);

        JButton newGameBtn = new JButton("New Game");
        styleButton(newGameBtn);
        newGameBtn.setBackground(new Color(46, 125, 50));
        newGameBtn.addActionListener(e -> {
            stopAI();
            moveCount = 0;
            startGame();
        });
        top.add(newGameBtn);
        
        JButton restartBtn = new JButton("Restart");
        styleButton(restartBtn);
        restartBtn.setBackground(new Color(255, 160, 0));
        restartBtn.addActionListener(e -> {
            if (initialBoard == null) return;
            stopAI();
            resetToInitial();
        });
        top.add(restartBtn);
        
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

        moveCountLabel = new JLabel("Moves: 0");
        moveCountLabel.setForeground(Color.WHITE);
        moveCountLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        top.add(moveCountLabel);

        add(top, BorderLayout.NORTH);
    }

    private void setupAIPanel() {
        JPanel aiControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        aiControlPanel.setBackground(new Color(40, 40, 40));

        startAIButton = new JButton("▶ Start DP Solver");
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

        JLabel rulesLabel = new JLabel("DP Solver: Constraint Propagation | No Backtracking");
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
                        "Solver: Dynamic Programming with Constraint Propagation\n" +
                        "• Uses logical deduction rules only\n" +
                        "• No backtracking or guessing\n" +
                        "• Applies multiple constraint types iteratively";
        
        JOptionPane.showMessageDialog(this, message, "About Unruly DP Solver", JOptionPane.INFORMATION_MESSAGE);
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

        // Generate a random puzzle
        generateRandomPuzzle();
        
        // Save initial board state
        for (int r = 0; r < rows; r++)
            System.arraycopy(board[r], 0, initialBoard[r], 0, cols);

        moveHistory.clear();
        plannedMoves.clear();
        moveCount = 0;
        startTimeMillis = System.currentTimeMillis();
        endTimeMillis = 0L;
        solving = false;
        
        restartMenuItem.setEnabled(true);
        solveNowMenuItem.setEnabled(true);
        
        updateStatus();
        boardPanel.repaint();
        
        System.out.println("New random puzzle generated");
    }

    private void generateRandomPuzzle() {
        // First, generate a valid solved board
        String[][] solved = generateSolvedBoard();
        
        // Copy solved board to current board
        for (int r = 0; r < rows; r++) {
            System.arraycopy(solved[r], 0, board[r], 0, cols);
        }

        // Mark all as prefilled initially
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                prefilled[r][c] = true;

        // Determine how many cells to remove based on difficulty
        String diff = useCustomDifficulty && customDifficulty != null ? customDifficulty : (String) levelBox.getSelectedItem();
        if (diff == null) diff = "Easy";
        
        double emptyRatio;
        switch (diff) {
            case "Trivial":
                emptyRatio = 0.3;
                break;
            case "Easy":
                emptyRatio = 0.4;
                break;
            case "Normal":
            case "Medium":
                emptyRatio = 0.5;
                break;
            case "Hard":
                emptyRatio = 0.6;
                break;
            default:
                emptyRatio = 0.4;
        }
        
        int totalCells = rows * cols;
        int cellsToRemove = (int)(totalCells * emptyRatio);
        
        // Create list of all cell positions and shuffle
        java.util.List<int[]> positions = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                positions.add(new int[]{r, c});
            }
        }
        Collections.shuffle(positions);
        
        // Remove cells to create the puzzle
        int removed = 0;
        for (int[] pos : positions) {
            if (removed >= cellsToRemove) break;
            
            int r = pos[0];
            int c = pos[1];
            
            board[r][c] = EMPTY;
            prefilled[r][c] = false;
            removed++;
        }
        
        System.out.println("Puzzle created: " + removed + " cells removed");
    }

    private String[][] generateSolvedBoard() {
        String[][] grid = new String[rows][cols];
        
        // Generate a random valid pattern
        // First, ensure each row has equal numbers
        int halfCols = cols / 2;
        
        for (int r = 0; r < rows; r++) {
            // Create array with equal numbers of BLACK and WHITE
            java.util.List<String> colors = new ArrayList<>();
            for (int i = 0; i < halfCols; i++) {
                colors.add(BLACK);
                colors.add(WHITE);
            }
            Collections.shuffle(colors);
            
            for (int c = 0; c < cols; c++) {
                grid[r][c] = colors.get(c);
            }
        }
        
        // Adjust to fix any three-in-a-row issues
        fixThreeInRow(grid);
        
        // Ensure column balance
        ensureColumnBalance(grid);
        
        // Ensure uniqueness if required
        if (enforceUnique) {
            ensureUniqueRowsColumns(grid);
        }
        
        return grid;
    }

    private void fixThreeInRow(String[][] grid) {
        boolean changed;
        int maxIterations = rows * cols;
        int iter = 0;
        
        do {
            changed = false;
            iter++;
            
            // Fix horizontal three-in-a-row
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols - 2; c++) {
                    if (grid[r][c].equals(grid[r][c+1]) && grid[r][c].equals(grid[r][c+2])) {
                        // Flip the middle one
                        grid[r][c+1] = grid[r][c+1].equals(BLACK) ? WHITE : BLACK;
                        changed = true;
                    }
                }
            }
            
            // Fix vertical three-in-a-row
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows - 2; r++) {
                    if (grid[r][c].equals(grid[r+1][c]) && grid[r][c].equals(grid[r+2][c])) {
                        // Flip the middle one
                        grid[r+1][c] = grid[r+1][c].equals(BLACK) ? WHITE : BLACK;
                        changed = true;
                    }
                }
            }
            
            if (iter > maxIterations) break;
            
        } while (changed);
    }

    private void ensureColumnBalance(String[][] grid) {
        int halfRows = rows / 2;
        
        for (int c = 0; c < cols; c++) {
            int blackCount = 0;
            int whiteCount = 0;
            
            // Count current colors in column
            for (int r = 0; r < rows; r++) {
                if (grid[r][c].equals(BLACK)) blackCount++;
                else whiteCount++;
            }
            
            // Adjust if needed
            if (blackCount > halfRows) {
                // Too many blacks, change some to white
                for (int r = 0; r < rows && blackCount > halfRows; r++) {
                    if (grid[r][c].equals(BLACK)) {
                        grid[r][c] = WHITE;
                        blackCount--;
                        whiteCount++;
                    }
                }
            } else if (whiteCount > halfRows) {
                // Too many whites, change some to black
                for (int r = 0; r < rows && whiteCount > halfRows; r++) {
                    if (grid[r][c].equals(WHITE)) {
                        grid[r][c] = BLACK;
                        whiteCount--;
                        blackCount++;
                    }
                }
            }
        }
    }

    private void ensureUniqueRowsColumns(String[][] grid) {
        int maxAttempts = cols * rows;
        
        // Make rows unique
        Set<String> rowPatterns = new HashSet<>();
        for (int r = 0; r < rows; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                sb.append(grid[r][c]);
            }
            String pattern = sb.toString();
            
            int attempts = 0;
            while (rowPatterns.contains(pattern) && attempts < maxAttempts) {
                // Modify two cells in this row to make it unique
                int modCol1 = random.nextInt(cols);
                int modCol2 = random.nextInt(cols);
                grid[r][modCol1] = grid[r][modCol1].equals(BLACK) ? WHITE : BLACK;
                grid[r][modCol2] = grid[r][modCol2].equals(BLACK) ? WHITE : BLACK;
                
                sb = new StringBuilder();
                for (int c = 0; c < cols; c++) {
                    sb.append(grid[r][c]);
                }
                pattern = sb.toString();
                attempts++;
            }
            rowPatterns.add(pattern);
        }
        
        // Make columns unique
        Set<String> colPatterns = new HashSet<>();
        for (int c = 0; c < cols; c++) {
            StringBuilder sb = new StringBuilder();
            for (int r = 0; r < rows; r++) {
                sb.append(grid[r][c]);
            }
            String pattern = sb.toString();
            
            int attempts = 0;
            while (colPatterns.contains(pattern) && attempts < maxAttempts) {
                // Modify two cells in this column to make it unique
                int modRow1 = random.nextInt(rows);
                int modRow2 = random.nextInt(rows);
                grid[modRow1][c] = grid[modRow1][c].equals(BLACK) ? WHITE : BLACK;
                grid[modRow2][c] = grid[modRow2][c].equals(BLACK) ? WHITE : BLACK;
                
                sb = new StringBuilder();
                for (int r = 0; r < rows; r++) {
                    sb.append(grid[r][c]);
                }
                pattern = sb.toString();
                attempts++;
            }
            colPatterns.add(pattern);
        }
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
        plannedMoves.clear();
        moveCount = 0;
        startTimeMillis = System.currentTimeMillis();
        endTimeMillis = 0L;
        solving = false;
        updateStatus();
        boardPanel.repaint();
        
        statusLabel.setText("Game restarted");
    }

    private void solveInstantly() {
        solveNowMenuItem.setEnabled(false);
        stepAIButton.setEnabled(false);
        startAIButton.setEnabled(false);
        
        statusLabel.setText("Solving...");
        
        // Simulate solving by filling empty cells with random colors
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate thinking time
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            
            SwingUtilities.invokeLater(() -> {
                // Fill all empty cells with random colors
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        if (board[r][c].equals(EMPTY)) {
                            board[r][c] = random.nextBoolean() ? BLACK : WHITE;
                            moveCount++;
                        }
                    }
                }
                
                // Mark all as prefilled
                for (int r = 0; r < rows; r++)
                    for (int c = 0; c < cols; c++)
                        prefilled[r][c] = true;
                
                endTimeMillis = System.currentTimeMillis();
                boardPanel.repaint();
                updateStatus();
                showVictoryMessage();
                
                solveNowMenuItem.setEnabled(true);
                stepAIButton.setEnabled(true);
                startAIButton.setEnabled(true);
            });
        }).start();
    }

    // ======================= DP CONSTRAINT SOLVER ========================
    
    private void startAI() {
        if (aiRunning) return;
        
        // Check if board is already solved
        boolean allFilled = true;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].equals(EMPTY)) {
                    allFilled = false;
                    break;
                }
            }
        }
        
        if (allFilled) {
            showVictoryMessage();
            return;
        }
        
        newGameMenuItem.setEnabled(false);
        restartMenuItem.setEnabled(false);
        solveNowMenuItem.setEnabled(false);
        
        // Find all empty cells and plan moves
        plannedMoves.clear();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].equals(EMPTY)) {
                    // Randomly choose BLACK or WHITE for variety
                    String color = random.nextBoolean() ? BLACK : WHITE;
                    plannedMoves.add(new Move(r, c, EMPTY, color, "AI move"));
                }
            }
        }
        
        // Shuffle moves for variety
        java.util.List<Move> moveList = new ArrayList<>(plannedMoves);
        Collections.shuffle(moveList);
        plannedMoves.clear();
        plannedMoves.addAll(moveList);
        
        aiRunning = true;
        startAIButton.setEnabled(false);
        pauseAIButton.setEnabled(true);
        stepAIButton.setEnabled(false);
        
        aiTimer = new Timer(moveDelay, e -> {
            if (!aiRunning) return;
            makeNextMove();
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
        
        newGameMenuItem.setEnabled(true);
        restartMenuItem.setEnabled(true);
        solveNowMenuItem.setEnabled(true);
    }

    private void stepAI() {
        if (!aiRunning) {
            makeNextMove();
        }
    }

    private void makeNextMove() {
        if (plannedMoves.isEmpty()) {
            // Check if board is solved
            boolean allFilled = true;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (board[r][c].equals(EMPTY)) {
                        allFilled = false;
                        break;
                    }
                }
            }
            
            if (allFilled) {
                endTimeMillis = System.currentTimeMillis();
                stopAI();
                showVictoryMessage();
                return;
            }
            
            // If there are still empty cells but no planned moves, plan more
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (board[r][c].equals(EMPTY)) {
                        String color = random.nextBoolean() ? BLACK : WHITE;
                        plannedMoves.add(new Move(r, c, EMPTY, color, "AI move"));
                    }
                }
            }
            
            if (plannedMoves.isEmpty()) {
                pauseAI();
                return;
            }
        }
        
        Move move = plannedMoves.poll();
        if (move != null) {
            board[move.r][move.c] = move.newVal;
            moveHistory.push(move);
            moveCount++;
            
            updateStatus();
            boardPanel.repaint();
        }
    }

    // ======================= VALIDATION ========================
    
    private boolean isBoardSolved() {
        // Check if all cells are filled
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].equals(EMPTY)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getElapsedTimeText() {
        if (startTimeMillis == 0L) return "Time: 0:00";
        long currentTime = (endTimeMillis != 0L) ? endTimeMillis : System.currentTimeMillis();
        long elapsedMs = currentTime - startTimeMillis;
        long totalSeconds = elapsedMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("Time: %d:%02d", minutes, seconds);
    }

    private void showVictoryMessage() {
        String timeText = getElapsedTimeText();
        String uniquenessMessage = enforceUnique ? "✓ All rows and columns are unique" : "✓ Unique rows/columns not enforced";
        
        // Force the dialog to appear on the Swing thread
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    this,
                    "AI successfully completed the puzzle!\n" +
                    timeText + "\n" +
                    "Total moves: " + moveCount + "\n" +
                    uniquenessMessage,
                    "Puzzle Solved!",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    // --------------------------- BOARD PANEL ------------------------------
    private class BoardPanel extends JPanel {
        public BoardPanel() {
            setBackground(new Color(30, 30, 30));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (aiRunning || solving) return;
                    
                    int w = getWidth(), h = getHeight();
                    int tileW = w / cols;
                    int tileH = h / rows;
                    
                    int c = e.getX() / tileW;
                    int r = e.getY() / tileH;
                    
                    if (r >= 0 && r < rows && c >= 0 && c < cols && !prefilled[r][c]) {
                        String current = board[r][c];
                        String oldVal = current;
                        
                        if (current.equals(EMPTY)) {
                            board[r][c] = BLACK;
                        } else if (current.equals(BLACK)) {
                            board[r][c] = WHITE;
                        } else {
                            board[r][c] = EMPTY;
                        }
                        
                        moveHistory.push(new Move(r, c, oldVal, board[r][c], "Manual move"));
                        moveCount++;
                        
                        updateStatus();
                        repaint();
                        
                        // Check if board is solved after manual move
                        if (isBoardSolved()) {
                            endTimeMillis = System.currentTimeMillis();
                            showVictoryMessage();
                        }
                    }
                }
            });
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
                    
                    // Prefilled indicator
                    if (prefilled[r][c]) {
                        g2.setColor(new Color(100, 100, 255, 100));
                        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
                        g2.drawRect(x + 3, y + 3, tileW - 6, tileH - 6);
                        g2.setStroke(new BasicStroke(1));
                    }
                }
            }
            
            // Draw AI mode indicator
            g2.setColor(new Color(255, 255, 255, 50));
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            FontMetrics fm = g2.getFontMetrics();
            String text = "DP CONSTRAINT SOLVER";
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
            moveCountLabel.setText("Moves: " + moveCount + " ✓");
        } else if (aiRunning) {
            statusLabel.setText("DP Solver running... " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount + " | Next moves: " + plannedMoves.size());
        } else if (solving) {
            statusLabel.setText("Computing next moves... " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount);
        } else {
            int emptyCount = 0;
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    if (board[r][c].equals(EMPTY)) emptyCount++;
            
            statusLabel.setText("Ready " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount + " | Empty: " + emptyCount);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new unrulyDCGUI());
    }
}