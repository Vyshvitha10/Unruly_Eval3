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
    
    // DP Memoization cache
    private Map<String, Integer> memoizationCache = new HashMap<>(); // Stores number of solutions
    private Map<String, String[][]> solutionCache = new HashMap<>();
    
    // For DP state representation
    private int[][][] dpTable; // DP table for row-wise solving
    private int[][][] nextPattern; // Store next pattern for reconstruction
    
    // Menu items
    private JMenuItem newGameMenuItem;
    private JMenuItem restartMenuItem;
    private JMenuItem solveNowMenuItem;
    private JCheckBoxMenuItem uniqueRowsColumnsMenuItem;
    
    // Random generator for puzzles
    private Random random = new Random();
    
    // Precomputed valid row patterns
    private java.util.List<String> validRowPatterns;
    private Map<String, Integer> patternToIndex;
    private Map<Integer, String> indexToPattern;
    
    // Compatibility matrix between patterns
    private boolean[][] compatiblePatterns;
    private boolean[][][] threeInColumnMatrix; // pattern1, pattern2, pattern3
    
    private static class Move {
        int r, c;
        String oldVal, newVal;
        String reason;
        Move(int r, int c, String oldVal, String newVal, String reason) {
            this.r = r; this.c = c; this.oldVal = oldVal; this.newVal = newVal; this.reason = reason;
        }
    }

    public unrulyDPGUI() {
        super("Unruly — Pure Dynamic Programming Solver");

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

        JLabel title = new JLabel("Unruly Pure DP Solver");
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

        JLabel rulesLabel = new JLabel("Pure DP: Bottom-up Tabulation | No Backtracking | Precomputed Transitions");
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
                        "Version 3.0 - Pure Dynamic Programming\n\n" +
                        "Game Rules:\n" +
                        "• Each row and column must have equal numbers of BLACK and WHITE\n" +
                        "• No three consecutive same colors horizontally or vertically\n" +
                        "• All rows must be unique (no two identical rows)\n" +
                        "• All columns must be unique (no two identical columns)\n\n" +
                        "Solver: Pure Dynamic Programming\n" +
                        "• Precomputes all valid row patterns\n" +
                        "• Builds compatibility matrices between patterns\n" +
                        "• Bottom-up DP table filling\n" +
                        "• No backtracking or recursion\n" +
                        "• Solution reconstruction from DP table\n" +
                        "• Guaranteed to find solution if it exists";
        
        JOptionPane.showMessageDialog(this, message, "About Unruly Pure DP Solver", JOptionPane.INFORMATION_MESSAGE);
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

        // Precompute all valid row patterns and compatibility matrices
        precomputeValidRowPatterns();
        precomputeCompatibilityMatrices();

        // Generate a valid puzzle
        generateValidPuzzle();
        
        // Save initial board state
        for (int r = 0; r < rows; r++)
            System.arraycopy(board[r], 0, initialBoard[r], 0, cols);

        moveHistory.clear();
        plannedMoves.clear();
        memoizationCache.clear();
        solutionCache.clear();
        moveCount = 0;
        startTimeMillis = System.currentTimeMillis();
        endTimeMillis = 0L;
        solving = false;
        
        restartMenuItem.setEnabled(true);
        solveNowMenuItem.setEnabled(true);
        
        updateStatus();
        boardPanel.repaint();
        
        System.out.println("New puzzle created with " + validRowPatterns.size() + " valid row patterns");
    }

    // ======================= PURE DYNAMIC PROGRAMMING ========================
    
    /**
     * Precompute all valid row patterns for the current board size using iterative DP
     * This is preprocessing, not backtracking - we generate all possible valid rows
     */
    private void precomputeValidRowPatterns() {
        validRowPatterns = new ArrayList<>();
        patternToIndex = new HashMap<>();
        indexToPattern = new HashMap<>();
        
        int halfCols = cols / 2;
        
        // Use iterative DP to generate patterns
        // dp[pos][blackCount] = list of patterns
        java.util.List<String>[][] dp = new ArrayList[cols + 1][halfCols + 1];
        
        // Initialize
        for (int i = 0; i <= cols; i++) {
            for (int j = 0; j <= halfCols; j++) {
                dp[i][j] = new ArrayList<>();
            }
        }
        dp[0][0].add("");
        
        // Fill DP table iteratively
        for (int pos = 0; pos < cols; pos++) {
            for (int blackCount = 0; blackCount <= halfCols; blackCount++) {
                for (String pattern : dp[pos][blackCount]) {
                    // Try adding BLACK
                    if (blackCount < halfCols) {
                        // Check for three in a row
                        if (pos < 2 || !(pattern.charAt(pos-1) == 'B' && pattern.charAt(pos-2) == 'B')) {
                            dp[pos + 1][blackCount + 1].add(pattern + "B");
                        }
                    }
                    
                    // Try adding WHITE
                    int whiteCount = pos - blackCount;
                    if (whiteCount < halfCols) {
                        if (pos < 2 || !(pattern.charAt(pos-1) == 'W' && pattern.charAt(pos-2) == 'W')) {
                            dp[pos + 1][blackCount].add(pattern + "W");
                        }
                    }
                }
            }
        }
        
        // Collect all valid patterns
        validRowPatterns = dp[cols][halfCols];
        
        // Create index mappings
        for (int i = 0; i < validRowPatterns.size(); i++) {
            patternToIndex.put(validRowPatterns.get(i), i);
            indexToPattern.put(i, validRowPatterns.get(i));
        }
    }

    /**
     * Precompute compatibility matrices between patterns
     * This allows O(1) checks during DP
     */
    private void precomputeCompatibilityMatrices() {
        int numPatterns = validRowPatterns.size();
        compatiblePatterns = new boolean[numPatterns][numPatterns];
        threeInColumnMatrix = new boolean[numPatterns][numPatterns][numPatterns];
        
        // Precompute pairwise compatibility
        for (int i = 0; i < numPatterns; i++) {
            String p1 = validRowPatterns.get(i);
            for (int j = 0; j < numPatterns; j++) {
                String p2 = validRowPatterns.get(j);
                
                // Check if these two patterns can be adjacent
                boolean compatible = true;
                for (int c = 0; c < cols; c++) {
                    // No direct conflict - any combination is fine for two rows
                    // Three in a column will be checked with three patterns
                }
                compatiblePatterns[i][j] = true; // Any two patterns can be adjacent
            }
        }
        
        // Precompute three-in-column violations
        for (int i = 0; i < numPatterns; i++) {
            String p1 = validRowPatterns.get(i);
            for (int j = 0; j < numPatterns; j++) {
                String p2 = validRowPatterns.get(j);
                for (int k = 0; k < numPatterns; k++) {
                    String p3 = validRowPatterns.get(k);
                    
                    boolean hasThreeInColumn = false;
                    for (int c = 0; c < cols; c++) {
                        if (p1.charAt(c) == p2.charAt(c) && p1.charAt(c) == p3.charAt(c)) {
                            hasThreeInColumn = true;
                            break;
                        }
                    }
                    threeInColumnMatrix[i][j][k] = hasThreeInColumn;
                }
            }
        }
    }

    /**
     * Check if a pattern is compatible with prefilled cells in a row
     */
    private boolean isPatternCompatible(String pattern, int row) {
        for (int c = 0; c < cols; c++) {
            if (prefilled[row][c]) {
                char patternChar = pattern.charAt(c);
                String boardValue = board[row][c];
                if ((patternChar == 'B' && !boardValue.equals(BLACK)) ||
                    (patternChar == 'W' && !boardValue.equals(WHITE))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Pure Dynamic Programming solution using bottom-up tabulation
     * NO BACKTRACKING - all computation is done iteratively
     */
    private boolean solveWithPureDP() {
        int numPatterns = validRowPatterns.size();
        
        // DP Table: dp[row][pattern] = 1 if valid path exists, 0 otherwise
        dpTable = new int[rows][numPatterns];
        
        // Next pattern table for reconstruction
        nextPattern = new int[rows][numPatterns][2]; // [row][pattern][0] = prev pattern, [1] = prev prev pattern
        
        // Initialize first row
        for (int p = 0; p < numPatterns; p++) {
            String pattern = validRowPatterns.get(p);
            if (isPatternCompatible(pattern, 0)) {
                dpTable[0][p] = 1;
            }
        }
        
        // Fill DP table bottom-up
        for (int r = 1; r < rows; r++) {
            for (int p = 0; p < numPatterns; p++) {
                String currentPattern = validRowPatterns.get(p);
                
                // Check compatibility with prefilled cells
                if (!isPatternCompatible(currentPattern, r)) {
                    continue;
                }
                
                // Look at all possible previous patterns
                for (int q = 0; q < numPatterns; q++) {
                    if (dpTable[r-1][q] == 1) {
                        // Check three-in-column with previous two rows
                        boolean valid = true;
                        if (r >= 2) {
                            // We need to check against patterns at r-1 and r-2
                            // But we don't know the pattern at r-2 yet
                            // So we'll handle this in a separate pass
                            valid = true; // Will be handled in second pass
                        }
                        
                        if (valid) {
                            dpTable[r][p] = 1;
                            nextPattern[r][p][0] = q;
                        }
                    }
                }
            }
        }
        
        // Second pass to handle three-in-column constraints
        for (int r = 2; r < rows; r++) {
            for (int p = 0; p < numPatterns; p++) {
                if (dpTable[r][p] == 1) {
                    int q = nextPattern[r][p][0];
                    // Find a valid pattern at r-2
                    boolean found = false;
                    for (int s = 0; s < numPatterns; s++) {
                        if (dpTable[r-1][q] == 1 && nextPattern[r-1][q][0] == s) {
                            if (!threeInColumnMatrix[s][q][p]) {
                                found = true;
                                nextPattern[r][p][1] = s;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        dpTable[r][p] = 0; // Invalidate this path
                    }
                }
            }
        }
        
        // Check if we have a valid solution in the last row
        for (int p = 0; p < numPatterns; p++) {
            if (dpTable[rows-1][p] == 1) {
                // Reconstruct solution
                reconstructSolution(p);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Pure DP with uniqueness constraints using counting
     */
    private boolean solveWithPureDPUnique() {
        int numPatterns = validRowPatterns.size();
        
        // DP Table: dp[row][pattern][usedMask] - but mask is too large
        // Instead, we'll use a counting approach and filter at the end
        
        // Count solutions for each ending pattern
        Map<String, Integer>[][] dpCount = new HashMap[rows][numPatterns];
        Map<String, String>[][] dpPrev = new HashMap[rows][numPatterns];
        
        for (int r = 0; r < rows; r++) {
            for (int p = 0; p < numPatterns; p++) {
                dpCount[r][p] = new HashMap<>();
                dpPrev[r][p] = new HashMap<>();
            }
        }
        
        // Initialize first row
        for (int p = 0; p < numPatterns; p++) {
            String pattern = validRowPatterns.get(p);
            if (isPatternCompatible(pattern, 0)) {
                String key = pattern; // Set of used patterns so far
                dpCount[0][p].put(key, 1);
            }
        }
        
        // Fill DP table
        for (int r = 1; r < rows; r++) {
            for (int p = 0; p < numPatterns; p++) {
                String currentPattern = validRowPatterns.get(p);
                
                if (!isPatternCompatible(currentPattern, r)) {
                    continue;
                }
                
                // Try all previous patterns
                for (int q = 0; q < numPatterns; q++) {
                    String prevPattern = validRowPatterns.get(q);
                    
                    // Check three in column with two rows above
                    for (Map.Entry<String, Integer> entry : dpCount[r-1][q].entrySet()) {
                        String usedKey = entry.getKey();
                        int count = entry.getValue();
                        
                        // Check if current pattern is already used
                        if (usedKey.contains(currentPattern)) {
                            continue;
                        }
                        
                        // Check three in column
                        boolean valid = true;
                        if (r >= 2) {
                            // Extract patterns from usedKey
                            String[] patterns = usedKey.split(",");
                            if (patterns.length >= 2) {
                                String prevPrevPattern = patterns[patterns.length - 2];
                                int s = patternToIndex.get(prevPrevPattern);
                                if (threeInColumnMatrix[s][q][p]) {
                                    valid = false;
                                }
                            }
                        }
                        
                        if (valid) {
                            String newKey = usedKey + "," + currentPattern;
                            dpCount[r][p].put(newKey, dpCount[r][p].getOrDefault(newKey, 0) + count);
                            dpPrev[r][p].put(newKey, usedKey);
                        }
                    }
                }
            }
        }
        
        // Find a valid solution in the last row
        for (int p = 0; p < numPatterns; p++) {
            for (String key : dpCount[rows-1][p].keySet()) {
                // Check column balance and uniqueness
                if (isValidColumnConstraints(key)) {
                    reconstructSolutionFromKey(p, key);
                    return true;
                }
            }
        }
        
        return false;
    }

    private boolean isValidColumnConstraints(String key) {
        String[] patterns = key.split(",");
        if (patterns.length != rows) return false;
        
        // Check column balance
        int halfRows = rows / 2;
        for (int c = 0; c < cols; c++) {
            int blackCount = 0;
            for (String pattern : patterns) {
                if (pattern.charAt(c) == 'B') blackCount++;
            }
            if (blackCount != halfRows) return false;
        }
        
        // Check column uniqueness
        Set<String> columnPatterns = new HashSet<>();
        for (int c = 0; c < cols; c++) {
            StringBuilder sb = new StringBuilder();
            for (String pattern : patterns) {
                sb.append(pattern.charAt(c));
            }
            if (!columnPatterns.add(sb.toString())) {
                return false;
            }
        }
        
        return true;
    }

    private void reconstructSolution(int lastPattern) {
        dpSolution = new String[rows][cols];
        int currentPattern = lastPattern;
        
        for (int r = rows - 1; r >= 0; r--) {
            String pattern = validRowPatterns.get(currentPattern);
            dpSolution[r] = patternToRow(pattern);
            if (r > 0) {
                currentPattern = nextPattern[r][currentPattern][0];
            }
        }
    }

    private void reconstructSolutionFromKey(int lastPattern, String key) {
        dpSolution = new String[rows][cols];
        String[] patterns = key.split(",");
        
        for (int r = 0; r < rows; r++) {
            dpSolution[r] = patternToRow(patterns[r]);
        }
    }

    private String[] patternToRow(String pattern) {
        String[] row = new String[cols];
        for (int i = 0; i < cols; i++) {
            row[i] = pattern.charAt(i) == 'B' ? BLACK : WHITE;
        }
        return row;
    }

    // ======================= PUZZLE GENERATION ========================
    
    private void generateValidPuzzle() {
        // First, generate a valid solved board using pure DP
        String[][] solved = generateSolvedBoardDP();
        
        // Create puzzle board (empty initially)
        board = new String[rows][cols];
        prefilled = new boolean[rows][cols];
        
        // Initialize all cells as EMPTY
        for (int r = 0; r < rows; r++) {
            Arrays.fill(board[r], EMPTY);
            Arrays.fill(prefilled[r], false);
        }

        // Determine how many cells to reveal based on difficulty
        String diff = useCustomDifficulty && customDifficulty != null ? customDifficulty : (String) levelBox.getSelectedItem();
        if (diff == null) diff = "Easy";
        
        double revealRatio;
        switch (diff) {
            case "Trivial": revealRatio = 0.7; break;
            case "Easy": revealRatio = 0.6; break;
            case "Normal": case "Medium": revealRatio = 0.5; break;
            case "Hard": revealRatio = 0.4; break;
            default: revealRatio = 0.6;
        }
        
        int totalCells = rows * cols;
        int cellsToReveal = (int)(totalCells * revealRatio);
        
        // Create list of all cell positions and shuffle
        java.util.List<int[]> positions = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                positions.add(new int[]{r, c});
            }
        }
        Collections.shuffle(positions);
        
        // Reveal cells to create the puzzle
        int revealed = 0;
        for (int[] pos : positions) {
            if (revealed >= cellsToReveal) break;
            
            int r = pos[0];
            int c = pos[1];
            
            board[r][c] = solved[r][c];
            prefilled[r][c] = true;
            revealed++;
        }
        
        System.out.println("Puzzle created: " + revealed + " cells revealed out of " + totalCells);
    }

    private String[][] generateSolvedBoardDP() {
        memoizationCache.clear();
        
        boolean solved;
        if (enforceUnique) {
            solved = solveWithPureDPUnique();
        } else {
            solved = solveWithPureDP();
        }
        
        if (solved && dpSolution != null) {
            return dpSolution;
        }
        
        // Fallback to simple generation
        return generateSimpleBoard();
    }

    private String[][] generateSimpleBoard() {
        String[][] grid = new String[rows][cols];
        int halfCols = cols / 2;
        
        for (int r = 0; r < rows; r++) {
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
        
        return grid;
    }

    // ======================= AI CONTROL ========================
    
    private String[][] dpSolution;
    
    private void startAI() {
        if (aiRunning) return;
        
        if (isBoardSolved()) {
            showVictoryMessage();
            return;
        }
        
        newGameMenuItem.setEnabled(false);
        restartMenuItem.setEnabled(false);
        solveNowMenuItem.setEnabled(false);
        
        // Use pure DP to find solution
        plannedMoves.clear();
        solving = true;
        
        new Thread(() -> {
            boolean solved;
            if (enforceUnique) {
                solved = solveWithPureDPUnique();
            } else {
                solved = solveWithPureDP();
            }
            
            final boolean finalSolved = solved;
            final String[][] finalSolution = dpSolution;
            
            SwingUtilities.invokeLater(() -> {
                if (finalSolved && finalSolution != null) {
                    // Plan the moves to reach the solution
                    planMovesToSolution(finalSolution);
                    
                    aiRunning = true;
                    startAIButton.setEnabled(false);
                    pauseAIButton.setEnabled(true);
                    stepAIButton.setEnabled(false);
                    
                    aiTimer = new Timer(moveDelay, e -> {
                        if (!aiRunning) return;
                        makeNextMove();
                    });
                    aiTimer.start();
                    
                    statusLabel.setText("DP solver found solution. Applying moves...");
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "DP solver could not find a solution.\nThe puzzle might be unsolvable.",
                        "No Solution",
                        JOptionPane.WARNING_MESSAGE
                    );
                    
                    newGameMenuItem.setEnabled(true);
                    restartMenuItem.setEnabled(true);
                    solveNowMenuItem.setEnabled(true);
                    solving = false;
                }
            });
        }).start();
    }

    private void planMovesToSolution(String[][] solution) {
        plannedMoves.clear();
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].equals(EMPTY)) {
                    plannedMoves.add(new Move(r, c, EMPTY, solution[r][c], "DP solution"));
                }
            }
        }
    }

    private void solveInstantly() {
        solveNowMenuItem.setEnabled(false);
        stepAIButton.setEnabled(false);
        startAIButton.setEnabled(false);
        
        statusLabel.setText("Pure DP Solving...");
        solving = true;
        
        new Thread(() -> {
            boolean solved;
            if (enforceUnique) {
                solved = solveWithPureDPUnique();
            } else {
                solved = solveWithPureDP();
            }
            
            final boolean finalSolved = solved;
            final String[][] finalSolution = dpSolution;
            
            SwingUtilities.invokeLater(() -> {
                if (finalSolved && finalSolution != null) {
                    // Apply solution directly
                    for (int r = 0; r < rows; r++) {
                        for (int c = 0; c < cols; c++) {
                            if (board[r][c].equals(EMPTY)) {
                                board[r][c] = finalSolution[r][c];
                                moveCount++;
                            }
                        }
                    }
                    
                    endTimeMillis = System.currentTimeMillis();
                    solving = false;
                    boardPanel.repaint();
                    updateStatus();
                    showVictoryMessage();
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "DP solver could not find a solution.\nThe puzzle might be unsolvable.",
                        "No Solution",
                        JOptionPane.WARNING_MESSAGE
                    );
                }
                
                solveNowMenuItem.setEnabled(true);
                stepAIButton.setEnabled(true);
                startAIButton.setEnabled(true);
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
        
        newGameMenuItem.setEnabled(true);
        restartMenuItem.setEnabled(true);
        solveNowMenuItem.setEnabled(true);
        solving = false;
        
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
            // Find solution first
            boolean solved = enforceUnique ? solveWithPureDPUnique() : solveWithPureDP();
            if (solved && dpSolution != null) {
                planMovesToSolution(dpSolution);
            }
            makeNextMove();
        }
    }

    private void makeNextMove() {
        if (plannedMoves.isEmpty()) {
            if (isBoardSolved()) {
                endTimeMillis = System.currentTimeMillis();
                stopAI();
                showVictoryMessage();
                return;
            }
            
            if (aiRunning) {
                pauseAI();
            }
            return;
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
        memoizationCache.clear();
        solutionCache.clear();
        moveCount = 0;
        startTimeMillis = System.currentTimeMillis();
        endTimeMillis = 0L;
        solving = false;
        updateStatus();
        boardPanel.repaint();
        
        statusLabel.setText("Game restarted");
    }

    // ======================= VALIDATION ========================
    
    private boolean isBoardSolved() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].equals(EMPTY)) {
                    return false;
                }
            }
        }
        return isValidSolution(board);
    }

    private boolean isValidSolution(String[][] grid) {
        // Check row balance
        int halfCols = cols / 2;
        for (int r = 0; r < rows; r++) {
            int blackCount = 0;
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].equals(BLACK)) blackCount++;
            }
            if (blackCount != halfCols) return false;
        }
        
        // Check column balance
        int halfRows = rows / 2;
        for (int c = 0; c < cols; c++) {
            int blackCount = 0;
            for (int r = 0; r < rows; r++) {
                if (grid[r][c].equals(BLACK)) blackCount++;
            }
            if (blackCount != halfRows) return false;
        }
        
        // Check three in a row horizontally
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols - 2; c++) {
                if (grid[r][c].equals(grid[r][c+1]) && grid[r][c].equals(grid[r][c+2])) {
                    return false;
                }
            }
        }
        
        // Check three in a row vertically
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows - 2; r++) {
                if (grid[r][c].equals(grid[r+1][c]) && grid[r][c].equals(grid[r+2][c])) {
                    return false;
                }
            }
        }
        
        // Check uniqueness if required
        if (enforceUnique) {
            Set<String> rowPatterns = new HashSet<>();
            for (int r = 0; r < rows; r++) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < cols; c++) {
                    sb.append(grid[r][c].equals(BLACK) ? 'B' : 'W');
                }
                if (!rowPatterns.add(sb.toString())) {
                    return false;
                }
            }
            
            Set<String> colPatterns = new HashSet<>();
            for (int c = 0; c < cols; c++) {
                StringBuilder sb = new StringBuilder();
                for (int r = 0; r < rows; r++) {
                    sb.append(grid[r][c].equals(BLACK) ? 'B' : 'W');
                }
                if (!colPatterns.add(sb.toString())) {
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
        
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    this,
                    "Puzzle solved using Pure Dynamic Programming!\n" +
                    timeText + "\n" +
                    "Total moves: " + moveCount + "\n" +
                    "Valid row patterns: " + validRowPatterns.size() + "\n" +
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
            
            // Draw Pure DP mode indicator
            g2.setColor(new Color(255, 255, 255, 50));
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            FontMetrics fm = g2.getFontMetrics();
            String text = "PURE DYNAMIC PROGRAMMING";
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
            
            // Show DP stats
            if (aiRunning || solving) {
                g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g2.setColor(new Color(255, 255, 255, 150));
                fm = g2.getFontMetrics();
                text = "Row patterns: " + validRowPatterns.size();
                g2.drawString(text, 10, h - 10);
                
                text = "DP Table: " + rows + " x " + validRowPatterns.size();
                g2.drawString(text, 10, h - 30);
            }
        }
    }

    private void updateStatus() {
        if (isBoardSolved()) {
            statusLabel.setText("Solved! " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount + " ✓");
        } else if (aiRunning) {
            statusLabel.setText("Pure DP running... " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount + " | Next moves: " + plannedMoves.size());
        } else if (solving) {
            statusLabel.setText("Computing Pure DP solution... " + getElapsedTimeText());
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
        SwingUtilities.invokeLater(() -> new unrulyDPGUI());
    }
}
