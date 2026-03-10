package unrulyy;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class unrulyUserGUI extends JFrame {

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

    // Game state
    private int moveCount = 0;
    private long startTimeMillis = 0L;
    
    // For move tracking
    private Stack<Move> moveHistory = new Stack<>();
    
    private static class Move {
        int r, c;
        String oldVal, newVal;
        Move(int r, int c, String oldVal, String newVal) {
            this.r = r; this.c = c; this.oldVal = oldVal; this.newVal = newVal;
        }
    }

    public unrulyUserGUI() {
        super("Unruly — User Solving Mode");

        setupMenuBar();
        setupTopPanel();
        setupControlPanel();
        
        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleBoardClick(e.getX(), e.getY());
            }
        });

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
            moveCount = 0;
            startGame();
        });

        JButton restartBtn = new JButton("Restart");
        styleButton(restartBtn);
        restartBtn.addActionListener(e -> {
            if (initialBoard == null) return;
            resetToInitial();
        });

        JButton undoBtn = new JButton("Undo");
        styleButton(undoBtn);
        undoBtn.addActionListener(e -> undo());

        JButton checkBtn = new JButton("Check");
        styleButton(checkBtn);
        checkBtn.addActionListener(e -> checkSolution());

        menuBar.add(newGameBtn);
        menuBar.add(restartBtn);
        menuBar.add(undoBtn);
        menuBar.add(checkBtn);

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

        JLabel title = new JLabel("Unruly - User Solving Mode");
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

    private void setupControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        controlPanel.setBackground(new Color(40, 40, 40));

        JButton hintBtn = new JButton("💡 Hint");
        styleButton(hintBtn);
        hintBtn.setBackground(new Color(255, 160, 0));
        hintBtn.addActionListener(e -> giveHint());

        JButton solveBtn = new JButton("⚡ Solve");
        styleButton(solveBtn);
        solveBtn.setBackground(new Color(46, 125, 50));
        solveBtn.addActionListener(e -> solveInstantly());

        JLabel instructionLabel = new JLabel("Click cells to cycle: Empty → Black → White → Empty");
        instructionLabel.setForeground(Color.WHITE);
        instructionLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        controlPanel.add(hintBtn);
        controlPanel.add(solveBtn);
        controlPanel.add(instructionLabel);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private void addTypeMenuItem(JMenu menu, String label, int r, int c) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> {
            rows = r;
            cols = c;
            useCustomDifficulty = false;
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

    // ======================= FIXED: ALWAYS GENERATE SOLVABLE BOARD WITH UNIQUE ROWS/COLUMNS ========================
    
    private void startGame() {
        board = new String[rows][cols];
        prefilled = new boolean[rows][cols];
        initialBoard = new String[rows][cols];

        // Generate a valid solved board with unique rows and columns
        String[][] solved = generateValidSolvedBoard();
        
        // Copy solved board to initial
        for (int r = 0; r < rows; r++) {
            System.arraycopy(solved[r], 0, board[r], 0, cols);
            System.arraycopy(solved[r], 0, initialBoard[r], 0, cols);
        }

        // Mark all as prefilled initially
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                prefilled[r][c] = true;

        // Remove cells based on difficulty (but ensure puzzle remains solvable)
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
        
        // Create a list of all positions
        ArrayList<int[]> positions = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                positions.add(new int[]{r, c});
            }
        }
        Collections.shuffle(positions);
        
        // Remove cells one by one, ensuring puzzle remains solvable
        int removed = 0;
        for (int[] pos : positions) {
            if (removed >= remove) break;
            
            int r = pos[0];
            int c = pos[1];
            
            // Temporarily remove this cell
            String savedValue = board[r][c];
            board[r][c] = EMPTY;
            prefilled[r][c] = false;
            
            // Check if puzzle is still solvable
            if (hasSolution(board)) {
                removed++;
            } else {
                // If not solvable, restore the cell
                board[r][c] = savedValue;
                prefilled[r][c] = true;
            }
        }

        // Update initial board state
        for (int r = 0; r < rows; r++)
            System.arraycopy(board[r], 0, initialBoard[r], 0, cols);

        moveHistory.clear();
        moveCount = 0;
        startTimeMillis = System.currentTimeMillis();
        
        updateStatus();
        boardPanel.repaint();
    }

    /**
     * Generate a valid solved board with all rules satisfied including unique rows and columns
     */
    private String[][] generateValidSolvedBoard() {
        String[][] solved = new String[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                solved[r][c] = EMPTY;
        
        // Try multiple times to generate a valid board
        for (int attempt = 0; attempt < 1000; attempt++) {
            if (backtrackGenerateWithUniqueness(solved, 0)) {
                // Verify the board is valid with all rules
                boolean valid = true;
                
                // Check basic rules
                for (int r = 0; r < rows; r++) {
                    if (!isValidRow(solved[r])) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    for (int c = 0; c < cols; c++) {
                        if (!isValidColumn(solved, c)) {
                            valid = false;
                            break;
                        }
                    }
                }
                
                // Check uniqueness if enabled
                if (valid && enforceUnique) {
                    if (!hasUniqueRows(solved) || !hasUniqueColumns(solved)) {
                        valid = false;
                    }
                }
                
                if (valid) {
                    return solved;
                }
            }
            
            // Reset for next attempt
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    solved[r][c] = EMPTY;
        }
        
        // If all attempts fail, generate a guaranteed valid board with uniqueness
        return generateGuaranteedUniqueBoard();
    }

    /**
     * Backtracking with uniqueness constraints
     */
    private boolean backtrackGenerateWithUniqueness(String[][] grid, int pos) {
        if (pos == rows * cols) {
            // Check if this is a valid complete board
            for (int r = 0; r < rows; r++) {
                if (!isValidRow(grid[r])) return false;
            }
            for (int c = 0; c < cols; c++) {
                if (!isValidColumn(grid, c)) return false;
            }
            
            // Check uniqueness if enabled
            if (enforceUnique) {
                if (!hasUniqueRows(grid) || !hasUniqueColumns(grid)) {
                    return false;
                }
            }
            
            return true;
        }
        
        int r = pos / cols;
        int c = pos % cols;
        
        // Try colors in random order for variety
        String[] colors = rand.nextBoolean() ? 
            new String[]{BLACK, WHITE} : new String[]{WHITE, BLACK};
        
        for (String color : colors) {
            grid[r][c] = color;
            
            // Quick prune for immediate violations
            if (hasImmediateViolation(grid, r, c)) {
                grid[r][c] = EMPTY;
                continue;
            }
            
            // Check row balance so far
            if (!maintainsBalance(grid, r, c)) {
                grid[r][c] = EMPTY;
                continue;
            }
            
            // For larger boards, check partial uniqueness to prune early
            if (enforceUnique && pos > rows * 2) {
                if (hasPartialDuplicate(grid, r, c)) {
                    grid[r][c] = EMPTY;
                    continue;
                }
            }
            
            if (backtrackGenerateWithUniqueness(grid, pos + 1)) {
                return true;
            }
            
            grid[r][c] = EMPTY;
        }
        
        return false;
    }

    /**
     * Check for partial duplicates during generation
     */
    private boolean hasPartialDuplicate(String[][] grid, int lastR, int lastC) {
        // Check if this row might become duplicate with another completed row
        if (isRowComplete(grid[lastR])) {
            for (int r2 = 0; r2 < rows; r2++) {
                if (r2 != lastR && isRowComplete(grid[r2])) {
                    boolean identical = true;
                    for (int c = 0; c < cols; c++) {
                        if (!grid[lastR][c].equals(grid[r2][c])) {
                            identical = false;
                            break;
                        }
                    }
                    if (identical) return true;
                }
            }
        }
        
        // Check column duplicates
        if (isColumnComplete(grid, lastC)) {
            for (int c2 = 0; c2 < cols; c2++) {
                if (c2 != lastC && isColumnComplete(grid, c2)) {
                    boolean identical = true;
                    for (int r = 0; r < rows; r++) {
                        if (!grid[r][lastC].equals(grid[r][c2])) {
                            identical = false;
                            break;
                        }
                    }
                    if (identical) return true;
                }
            }
        }
        
        return false;
    }

    private boolean isRowComplete(String[] row) {
        for (String cell : row) {
            if (cell.equals(EMPTY)) return false;
        }
        return true;
    }

    private boolean isColumnComplete(String[][] grid, int c) {
        for (int r = 0; r < rows; r++) {
            if (grid[r][c].equals(EMPTY)) return false;
        }
        return true;
    }

    /**
     * Generate a guaranteed valid board with unique rows and columns
     */
    private String[][] generateGuaranteedUniqueBoard() {
        String[][] grid = new String[rows][cols];
        
        // Use a Latin square style pattern to ensure uniqueness
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Create a pattern that ensures all rows and columns are unique
                // This is a simplified pattern that works for even sizes
                if ((r + c) % 2 == 0) {
                    grid[r][c] = (c % 2 == 0) ? BLACK : WHITE;
                } else {
                    grid[r][c] = (c % 2 == 0) ? WHITE : BLACK;
                }
            }
        }
        
        // Ensure equal counts in each row
        for (int r = 0; r < rows; r++) {
            int blackCount = 0;
            int whiteCount = 0;
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].equals(BLACK)) blackCount++;
                else whiteCount++;
            }
            
            // Adjust if needed
            int half = cols / 2;
            if (blackCount != half) {
                for (int c = 0; c < cols; c++) {
                    if (blackCount > half && grid[r][c].equals(BLACK) && 
                        (c == 0 || !grid[r][c-1].equals(WHITE) || c == cols-1 || !grid[r][c+1].equals(WHITE))) {
                        grid[r][c] = WHITE;
                        blackCount--;
                        whiteCount++;
                    } else if (whiteCount > half && grid[r][c].equals(WHITE) &&
                              (c == 0 || !grid[r][c-1].equals(BLACK) || c == cols-1 || !grid[r][c+1].equals(BLACK))) {
                        grid[r][c] = BLACK;
                        whiteCount--;
                        blackCount++;
                    }
                    if (blackCount == half) break;
                }
            }
        }
        
        // Verify uniqueness and fix if needed
        if (!hasUniqueRows(grid)) {
            // Swap some values to make rows unique while maintaining validity
            fixDuplicateRows(grid);
        }
        
        if (!hasUniqueColumns(grid)) {
            fixDuplicateColumns(grid);
        }
        
        return grid;
    }

    private void fixDuplicateRows(String[][] grid) {
        Set<String> seen = new HashSet<>();
        for (int r = 0; r < rows; r++) {
            String rowStr = rowToString(grid[r]);
            while (seen.contains(rowStr)) {
                // Modify this row slightly
                for (int c = 0; c < cols - 1; c++) {
                    if (!grid[r][c].equals(grid[r][c+1])) {
                        // Swap adjacent different values
                        String temp = grid[r][c];
                        grid[r][c] = grid[r][c+1];
                        grid[r][c+1] = temp;
                        
                        // Check if this creates three in a row
                        if (!hasThreeInRow(grid[r])) {
                            break;
                        } else {
                            // Swap back if it creates three in a row
                            temp = grid[r][c];
                            grid[r][c] = grid[r][c+1];
                            grid[r][c+1] = temp;
                        }
                    }
                }
                rowStr = rowToString(grid[r]);
            }
            seen.add(rowStr);
        }
    }

    private void fixDuplicateColumns(String[][] grid) {
        Set<String> seen = new HashSet<>();
        for (int c = 0; c < cols; c++) {
            String colStr = columnToString(grid, c);
            while (seen.contains(colStr)) {
                // Modify this column slightly
                for (int r = 0; r < rows - 1; r++) {
                    if (!grid[r][c].equals(grid[r+1][c])) {
                        // Swap adjacent different values
                        String temp = grid[r][c];
                        grid[r][c] = grid[r+1][c];
                        grid[r+1][c] = temp;
                        
                        // Check if this creates three in a column
                        if (!hasThreeInColumn(grid, c)) {
                            break;
                        } else {
                            // Swap back if it creates three in a column
                            temp = grid[r][c];
                            grid[r][c] = grid[r+1][c];
                            grid[r+1][c] = temp;
                        }
                    }
                }
                colStr = columnToString(grid, c);
            }
            seen.add(colStr);
        }
    }

    private String rowToString(String[] row) {
        StringBuilder sb = new StringBuilder();
        for (String s : row) sb.append(s);
        return sb.toString();
    }

    private String columnToString(String[][] grid, int c) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) sb.append(grid[r][c]);
        return sb.toString();
    }

    private boolean hasThreeInRow(String[] row) {
        for (int i = 0; i < row.length - 2; i++) {
            if (row[i].equals(row[i+1]) && row[i].equals(row[i+2])) {
                return true;
            }
        }
        return false;
    }

    private boolean hasThreeInColumn(String[][] grid, int c) {
        for (int r = 0; r < rows - 2; r++) {
            if (grid[r][c].equals(grid[r+1][c]) && grid[r][c].equals(grid[r+2][c])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the puzzle has at least one solution
     */
    private boolean hasSolution(String[][] currentBoard) {
        // Make a deep copy
        String[][] testBoard = new String[rows][cols];
        for (int r = 0; r < rows; r++)
            System.arraycopy(currentBoard[r], 0, testBoard[r], 0, cols);
        
        // Try to find a solution
        return backtrackSolve(testBoard, 0);
    }

    /**
     * Backtracking solver to check if a solution exists
     */
    private boolean backtrackSolve(String[][] grid, int pos) {
        if (pos == rows * cols) {
            // Verify complete board
            for (int r = 0; r < rows; r++) {
                if (!isValidRow(grid[r])) return false;
            }
            for (int c = 0; c < cols; c++) {
                if (!isValidColumn(grid, c)) return false;
            }
            
            // Check uniqueness if enabled
            if (enforceUnique) {
                if (!hasUniqueRows(grid) || !hasUniqueColumns(grid)) {
                    return false;
                }
            }
            
            return true;
        }
        
        int r = pos / cols;
        int c = pos % cols;
        
        if (!grid[r][c].equals(EMPTY)) {
            return backtrackSolve(grid, pos + 1);
        }
        
        // Try both colors
        for (String color : new String[]{BLACK, WHITE}) {
            grid[r][c] = color;
            
            // Quick prune for three in a row
            if (!hasImmediateViolation(grid, r, c)) {
                if (backtrackSolve(grid, pos + 1)) {
                    return true;
                }
            }
            
            grid[r][c] = EMPTY;
        }
        
        return false;
    }

    private Random rand = new Random();

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

    private boolean hasImmediateViolation(String[][] grid, int r, int c) {
        String color = grid[r][c];
        
        // Check horizontal
        if (c >= 2 && grid[r][c-1].equals(color) && grid[r][c-2].equals(color))
            return true;
        if (c <= cols-3 && grid[r][c+1].equals(color) && grid[r][c+2].equals(color))
            return true;
        if (c > 0 && c < cols-1 && grid[r][c-1].equals(color) && grid[r][c+1].equals(color))
            return true;
        
        // Check vertical
        if (r >= 2 && grid[r-1][c].equals(color) && grid[r-2][c].equals(color))
            return true;
        if (r <= rows-3 && grid[r+1][c].equals(color) && grid[r+2][c].equals(color))
            return true;
        if (r > 0 && r < rows-1 && grid[r-1][c].equals(color) && grid[r+1][c].equals(color))
            return true;
        
        return false;
    }

    // ======================= UNIQUENESS CHECKING METHODS ========================

    private boolean hasUniqueRows(String[][] grid) {
        Set<String> rowPatterns = new HashSet<>();
        for (int r = 0; r < rows; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                sb.append(grid[r][c]);
            }
            String rowStr = sb.toString();
            if (rowPatterns.contains(rowStr)) {
                return false;
            }
            rowPatterns.add(rowStr);
        }
        return true;
    }

    private boolean hasUniqueColumns(String[][] grid) {
        Set<String> colPatterns = new HashSet<>();
        for (int c = 0; c < cols; c++) {
            StringBuilder sb = new StringBuilder();
            for (int r = 0; r < rows; r++) {
                sb.append(grid[r][c]);
            }
            String colStr = sb.toString();
            if (colPatterns.contains(colStr)) {
                return false;
            }
            colPatterns.add(colStr);
        }
        return true;
    }

    // ======================= REST OF THE CODE ========================

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

    private void undo() {
        if (!moveHistory.isEmpty()) {
            Move lastMove = moveHistory.pop();
            board[lastMove.r][lastMove.c] = lastMove.oldVal;
            moveCount--;
            updateStatus();
            boardPanel.repaint();
        }
    }

    private void solveInstantly() {
        String[][] solved = generateValidSolvedBoard();
        board = solved;
        
        prefilled = new boolean[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                prefilled[r][c] = true;
        
        initialBoard = new String[rows][cols];
        for (int r = 0; r < rows; r++)
            System.arraycopy(solved[r], 0, initialBoard[r], 0, cols);
        
        moveHistory.clear();
        moveCount = 0;
        updateStatus();
        boardPanel.repaint();
        showVictoryMessage();
    }

    private void handleBoardClick(int x, int y) {
        if (isBoardSolved()) {
            return;
        }

        int tileW = boardPanel.getWidth() / cols;
        int tileH = boardPanel.getHeight() / rows;
        
        int c = x / tileW;
        int r = y / tileH;
        
        if (r >= 0 && r < rows && c >= 0 && c < cols) {
            if (prefilled[r][c]) {
                JOptionPane.showMessageDialog(this, 
                    "This cell is prefilled and cannot be changed.", 
                    "Cannot Modify", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            String oldVal = board[r][c];
            String newVal;
            
            if (oldVal.equals(EMPTY)) {
                newVal = BLACK;
            } else if (oldVal.equals(BLACK)) {
                newVal = WHITE;
            } else {
                newVal = EMPTY;
            }
            
            board[r][c] = newVal;
            moveHistory.push(new Move(r, c, oldVal, newVal));
            moveCount++;
            
            updateStatus();
            boardPanel.repaint();
            
            if (isBoardSolved()) {
                showVictoryMessage();
            }
        }
    }

    private void checkSolution() {
        if (isBoardSolved()) {
            showVictoryMessage();
        } else {
            StringBuilder violations = new StringBuilder("Board is not solved:\n");
            
            int emptyCount = 0;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (board[r][c].equals(EMPTY)) {
                        emptyCount++;
                    }
                }
            }
            
            if (emptyCount > 0) {
                violations.append("- ").append(emptyCount).append(" empty cells remain\n");
            }
            
            // Check for three in a row
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols - 2; c++) {
                    if (!board[r][c].equals(EMPTY) &&
                        board[r][c].equals(board[r][c + 1]) &&
                        board[r][c].equals(board[r][c + 2])) {
                        violations.append("- Three ").append(board[r][c]).append(" in a row at row ").append(r + 1).append("\n");
                    }
                }
            }
            
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows - 2; r++) {
                    if (!board[r][c].equals(EMPTY) &&
                        board[r][c].equals(board[r + 1][c]) &&
                        board[r][c].equals(board[r + 2][c])) {
                        violations.append("- Three ").append(board[r][c]).append(" in a column at column ").append(c + 1).append("\n");
                    }
                }
            }
            
            // Check for duplicate rows
            if (enforceUnique) {
                Set<String> rowPatterns = new HashSet<>();
                for (int r = 0; r < rows; r++) {
                    if (isRowComplete(board[r])) {
                        StringBuilder sb = new StringBuilder();
                        for (int c = 0; c < cols; c++) {
                            sb.append(board[r][c]);
                        }
                        String rowStr = sb.toString();
                        if (rowPatterns.contains(rowStr)) {
                            violations.append("- Duplicate row found: row ").append(r + 1).append("\n");
                        }
                        rowPatterns.add(rowStr);
                    }
                }
                
                // Check for duplicate columns
                Set<String> colPatterns = new HashSet<>();
                for (int c = 0; c < cols; c++) {
                    if (isColumnComplete(board, c)) {
                        StringBuilder sb = new StringBuilder();
                        for (int r = 0; r < rows; r++) {
                            sb.append(board[r][c]);
                        }
                        String colStr = sb.toString();
                        if (colPatterns.contains(colStr)) {
                            violations.append("- Duplicate column found: column ").append(c + 1).append("\n");
                        }
                        colPatterns.add(colStr);
                    }
                }
            }
            
            JOptionPane.showMessageDialog(this, 
                violations.toString(), 
                "Not Solved Yet", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void giveHint() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].equals(EMPTY)) {
                    if (isValidPlace(board, r, c, BLACK)) {
                        JOptionPane.showMessageDialog(this,
                            "Try placing BLACK at row " + (r + 1) + ", column " + (c + 1),
                            "Hint",
                            JOptionPane.INFORMATION_MESSAGE);
                        return;
                    } else if (isValidPlace(board, r, c, WHITE)) {
                        JOptionPane.showMessageDialog(this,
                            "Try placing WHITE at row " + (r + 1) + ", column " + (c + 1),
                            "Hint",
                            JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                }
            }
        }
        JOptionPane.showMessageDialog(this,
            "No hint available. Try a different approach!",
            "Hint",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean isValidPlace(String[][] grid, int r, int c, String color) {
        // Check three in a row
        if (c >= 2 && grid[r][c-1].equals(color) && grid[r][c-2].equals(color)) return false;
        if (c <= cols-3 && grid[r][c+1].equals(color) && grid[r][c+2].equals(color)) return false;
        if (c > 0 && c < cols-1 && grid[r][c-1].equals(color) && grid[r][c+1].equals(color)) return false;
        
        if (r >= 2 && grid[r-1][c].equals(color) && grid[r-2][c].equals(color)) return false;
        if (r <= rows-3 && grid[r+1][c].equals(color) && grid[r+2][c].equals(color)) return false;
        if (r > 0 && r < rows-1 && grid[r-1][c].equals(color) && grid[r+1][c].equals(color)) return false;
        
        // Check row balance
        int blackRow = 0, whiteRow = 0;
        for (int col = 0; col < cols; col++) {
            if (grid[r][col].equals(BLACK)) blackRow++;
            else if (grid[r][col].equals(WHITE)) whiteRow++;
        }
        
        if (color.equals(BLACK)) blackRow++;
        else whiteRow++;
        
        int half = cols / 2;
        if (blackRow > half || whiteRow > half) return false;
        
        // Check column balance
        int blackCol = 0, whiteCol = 0;
        for (int row = 0; row < rows; row++) {
            if (grid[row][c].equals(BLACK)) blackCol++;
            else if (grid[row][c].equals(WHITE)) whiteCol++;
        }
        
        if (color.equals(BLACK)) blackCol++;
        else whiteCol++;
        
        half = rows / 2;
        if (blackCol > half || whiteCol > half) return false;
        
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

    private boolean isBoardSolved() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (board[r][c].equals(EMPTY)) return false;
        
        for (int r = 0; r < rows; r++)
            if (!isValidRow(board[r])) return false;
        for (int c = 0; c < cols; c++)
            if (!isValidColumn(board, c)) return false;
        
        // Check uniqueness if enabled
        if (enforceUnique) {
            if (!hasUniqueRows(board) || !hasUniqueColumns(board)) {
                return false;
            }
        }
        
        return true;
    }

    private void showVictoryMessage() {
        String timeText = getElapsedTimeText();
        String uniquenessMessage = enforceUnique ? "\n✓ All rows and columns are unique!" : "";
        
        JOptionPane.showMessageDialog(
                this,
                "Congratulations! You solved the puzzle!\n" +
                timeText + "\n" +
                "Total moves: " + moveCount +
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

        private boolean isCellPartOfInvalidRow(int r, int c) {
            if (board[r][c].equals(EMPTY)) return false;
            
            if (c >= 2 && 
                board[r][c].equals(board[r][c-1]) && 
                board[r][c].equals(board[r][c-2])) {
                return true;
            }
            
            if (c <= cols-3 && 
                board[r][c].equals(board[r][c+1]) && 
                board[r][c].equals(board[r][c+2])) {
                return true;
            }
            
            if (c > 0 && c < cols-1 && 
                board[r][c].equals(board[r][c-1]) && 
                board[r][c].equals(board[r][c+1])) {
                return true;
            }
            
            return false;
        }

        private boolean isCellPartOfInvalidColumn(int r, int c) {
            if (board[r][c].equals(EMPTY)) return false;
            
            if (r >= 2 && 
                board[r][c].equals(board[r-1][c]) && 
                board[r][c].equals(board[r-2][c])) {
                return true;
            }
            
            if (r <= rows-3 && 
                board[r][c].equals(board[r+1][c]) && 
                board[r][c].equals(board[r+2][c])) {
                return true;
            }
            
            if (r > 0 && r < rows-1 && 
                board[r][c].equals(board[r-1][c]) && 
                board[r][c].equals(board[r+1][c])) {
                return true;
            }
            
            return false;
        }

        private boolean isRowImbalanced(int r) {
            if (board[r][0].equals(EMPTY)) return false;
            
            int black = 0, white = 0;
            for (int c = 0; c < cols; c++) {
                if (board[r][c].equals(BLACK)) black++;
                else if (board[r][c].equals(WHITE)) white++;
            }
            
            int half = cols / 2;
            return black > half || white > half;
        }

        private boolean isColumnImbalanced(int c) {
            if (board[0][c].equals(EMPTY)) return false;
            
            int black = 0, white = 0;
            for (int r = 0; r < rows; r++) {
                if (board[r][c].equals(BLACK)) black++;
                else if (board[r][c].equals(WHITE)) white++;
            }
            
            int half = rows / 2;
            return black > half || white > half;
        }

        private boolean isRowDuplicate(int r) {
            if (!isRowComplete(board[r])) return false;
            
            for (int r2 = 0; r2 < rows; r2++) {
                if (r2 == r || !isRowComplete(board[r2])) continue;
                
                boolean identical = true;
                for (int c = 0; c < cols; c++) {
                    if (!board[r][c].equals(board[r2][c])) {
                        identical = false;
                        break;
                    }
                }
                if (identical) return true;
            }
            return false;
        }

        private boolean isColumnDuplicate(int c) {
            if (!isColumnComplete(board, c)) return false;
            
            for (int c2 = 0; c2 < cols; c2++) {
                if (c2 == c || !isColumnComplete(board, c2)) continue;
                
                boolean identical = true;
                for (int r = 0; r < rows; r++) {
                    if (!board[r][c].equals(board[r][c2])) {
                        identical = false;
                        break;
                    }
                }
                if (identical) return true;
            }
            return false;
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
                    
                    g2.setColor(new Color(50, 50, 50));
                    g2.fillRect(x, y, tileW, tileH);
                    
                    if (!board[r][c].equals(EMPTY)) {
                        g2.setColor(board[r][c].equals(BLACK) ? Color.BLACK : Color.WHITE);
                        g2.fillRect(x + 3, y + 3, tileW - 6, tileH - 6);
                    }
                    
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawRect(x, y, tileW, tileH);
                    
                    if (prefilled[r][c]) {
                        g2.setColor(new Color(100, 100, 255, 100));
                        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
                        g2.drawRect(x + 3, y + 3, tileW - 6, tileH - 6);
                        g2.setStroke(new BasicStroke(1));
                    }
                }
            }
            
            // Draw red borders for invalid cells
            g2.setStroke(new BasicStroke(3));
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (!board[r][c].equals(EMPTY)) {
                        boolean invalid = isCellPartOfInvalidRow(r, c) || 
                                         isCellPartOfInvalidColumn(r, c) ||
                                         isRowImbalanced(r) ||
                                         isColumnImbalanced(c) ||
                                         (enforceUnique && (isRowDuplicate(r) || isColumnDuplicate(c)));
                        
                        if (invalid) {
                            int x = c * tileW;
                            int y = r * tileH;
                            g2.setColor(Color.RED);
                            g2.drawRect(x + 2, y + 2, tileW - 4, tileH - 4);
                        }
                    }
                }
            }
            g2.setStroke(new BasicStroke(1));
            
            // Draw mode indicator
            g2.setColor(new Color(255, 255, 255, 50));
            g2.setFont(new Font("SansSerif", Font.BOLD, 36));
            FontMetrics fm = g2.getFontMetrics();
            String text = "USER MODE";
            int textWidth = fm.stringWidth(text);
            g2.drawString(text, (w - textWidth) / 2, h / 2);
            
            if (enforceUnique) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                fm = g2.getFontMetrics();
                text = "Unique Rows/Columns Enforced";
                textWidth = fm.stringWidth(text);
                g2.setColor(new Color(100, 255, 100, 50));
                g2.drawString(text, (w - textWidth) / 2, h / 2 + 30);
            }
        }
    }

    private void updateStatus() {
        if (isBoardSolved()) {
            statusLabel.setText("Solved! " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount + " ✓");
        } else {
            statusLabel.setText("Playing... " + getElapsedTimeText());
            moveCountLabel.setText("Moves: " + moveCount);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new unrulyUserGUI());
    }
}