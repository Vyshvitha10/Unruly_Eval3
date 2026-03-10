package unrulyy;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class UnrulyLauncher extends JFrame {
    
    private JList<String> modeList;
    private DefaultListModel<String> listModel;
    private JTextField widthField;
    private JTextField heightField;
    private JCheckBox uniqueCheck;
    private JComboBox<String> difficultyBox;
    private JTextArea descriptionArea;
    
    // Color themes for different modes
    private final Color[] modeColors = {
        new Color(70, 130, 180), // Steel Blue - User
        new Color(46, 125, 50),  // Green - Backtracking
        new Color(156, 39, 176), // Purple - DP
        new Color(255, 160, 0),  // Orange - D&C
        new Color(198, 40, 40)   // Red - Graphs
    };
    
    // Class names exactly as provided
    private final String[] classNames = {
        "unrulyUserGUI",
        "unrulyBacktrackingGUI", 
        "unrulyDPGUI",
        "unrulyDCGUI",
        "unrulyGraphsGUI"
    };
    
    public UnrulyLauncher() {
        super("🎮 Unruly Puzzle Suite - Launcher");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(245, 245, 250));
        
        // Add components
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER);
        mainPanel.add(createLaunchPanel(), BorderLayout.SOUTH);
        
        add(mainPanel);
        
        setSize(900, 650);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(25, 25, 35));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("UNRULY PUZZLE SUITE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel subtitleLabel = new JLabel("Choose Your Solving Mode");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(200, 200, 220));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        return headerPanel;
    }
    
    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        centerPanel.setBackground(new Color(245, 245, 250));
        
        centerPanel.add(createModeSelectionPanel());
        centerPanel.add(createConfigPanel());
        
        return centerPanel;
    }
    
    private JPanel createModeSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(200, 200, 220), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel title = new JLabel("📋 SELECT MODE");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(50, 50, 70));
        
        // Create mode list
        listModel = new DefaultListModel<>();
        String[] modes = {
            "👤 User Solving Mode",
            "🔄 Backtracking Mode", 
            "📊 Dynamic Programming Mode",
            "🔀 Divide & Conquer Mode",
            "📈 Graphs Mode"
        };
        
        for (String mode : modes) {
            listModel.addElement(mode);
        }
        
        modeList = new JList<>(listModel);
        modeList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        modeList.setFixedCellHeight(50);
        modeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modeList.setSelectedIndex(0);
        
        // Custom renderer
        modeList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
                setFont(new Font("Segoe UI", Font.BOLD, 14));
                
                if (!isSelected) {
                    setBackground(Color.WHITE);
                    setForeground(modeColors[index % modeColors.length]);
                } else {
                    setBackground(modeColors[index % modeColors.length]);
                    setForeground(Color.WHITE);
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(modeList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Description area
        descriptionArea = new JTextArea(5, 20);
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setBackground(new Color(245, 245, 250));
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder(new LineBorder(new Color(180, 180, 200)), "Description",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), new Color(100, 100, 120)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        modeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDescription();
            }
        });
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(descriptionArea, BorderLayout.SOUTH);
        
        updateDescription();
        
        return panel;
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(200, 200, 220), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Title
        JLabel title = new JLabel("⚙️ CONFIGURATION");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(50, 50, 70));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);
        
        // Board size
        JLabel widthLabel = new JLabel("Width (columns):");
        widthLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(widthLabel, gbc);
        
        widthField = new JTextField("8", 5);
        widthField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        widthField.setHorizontalAlignment(JTextField.CENTER);
        gbc.gridx = 1;
        panel.add(widthField, gbc);
        
        JLabel heightLabel = new JLabel("Height (rows):");
        heightLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(heightLabel, gbc);
        
        heightField = new JTextField("8", 5);
        heightField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        heightField.setHorizontalAlignment(JTextField.CENTER);
        gbc.gridx = 1;
        panel.add(heightField, gbc);
        
        // Difficulty
        JLabel diffLabel = new JLabel("Difficulty:");
        diffLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(diffLabel, gbc);
        
        difficultyBox = new JComboBox<>(new String[]{"Trivial", "Easy", "Normal", "Hard"});
        difficultyBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(difficultyBox, gbc);
        
        // Options
        uniqueCheck = new JCheckBox("Enforce unique rows and columns");
        uniqueCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        uniqueCheck.setSelected(true);
        uniqueCheck.setBackground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(uniqueCheck, gbc);
        
        // Preview panel (simple)
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
        previewPanel.setBackground(new Color(250, 250, 255));
        previewPanel.setPreferredSize(new Dimension(200, 100));
        
        JLabel previewLabel = new JLabel("", SwingConstants.CENTER);
        previewLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        previewPanel.add(previewLabel, BorderLayout.CENTER);
        
        modeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = modeList.getSelectedIndex();
                String[] previews = {"👤", "🔄", "📊", "🔀", "📈"};
                previewLabel.setText(previews[index]);
                previewLabel.setForeground(modeColors[index]);
            }
        });
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(previewPanel, gbc);
        
        // Set initial preview
        previewLabel.setText("👤");
        previewLabel.setForeground(modeColors[0]);
        
        return panel;
    }
    
    private JPanel createLaunchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15));
        panel.setBackground(new Color(245, 245, 250));
        
        JButton launchBtn = new JButton("🚀 LAUNCH GAME");
        launchBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        launchBtn.setBackground(new Color(46, 125, 50));
        launchBtn.setForeground(Color.WHITE);
        launchBtn.setFocusPainted(false);
        launchBtn.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40));
        launchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        launchBtn.addActionListener(e -> launchSelectedMode());
        
        JButton exitBtn = new JButton("❌ EXIT");
        exitBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        exitBtn.setBackground(new Color(198, 40, 40));
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setFocusPainted(false);
        exitBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        exitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exitBtn.addActionListener(e -> System.exit(0));
        
        panel.add(launchBtn);
        panel.add(exitBtn);
        
        return panel;
    }
    
    private void updateDescription() {
        String selected = modeList.getSelectedValue();
        if (selected == null) return;
        
        String desc = "";
        if (selected.contains("User")) {
            desc = "👤 Play and solve puzzles manually with mouse clicks.\n\n" +
                   "Features: Undo/Redo, Hints, Move tracking, Timer";
        } else if (selected.contains("Backtracking")) {
            desc = "🔄 Computer solves puzzles using recursive backtracking.\n\n" +
                   "Features: Step-by-step visualization, Search tree, Pruning";
        } else if (selected.contains("Dynamic Programming")) {
            desc = "📊 Efficient puzzle solving using DP with memoization.\n\n" +
                   "Features: Solution counting, Performance metrics, State space";
        } else if (selected.contains("Divide & Conquer")) {
            desc = "🔀 Solves puzzles by dividing board into smaller regions.\n\n" +
                   "Features: Region decomposition, Merge visualization";
        } else if (selected.contains("Graphs")) {
            desc = "📈 Represents puzzle as graph using graph algorithms.\n\n" +
                   "Features: BFS/DFS, Graph coloring, Connected components";
        }
        
        descriptionArea.setText(desc);
    }
    
    private void launchSelectedMode() {
        try {
            int width = Integer.parseInt(widthField.getText().trim());
            int height = Integer.parseInt(heightField.getText().trim());
            
            if (width < 4 || height < 4 || width > 30 || height > 30) {
                JOptionPane.showMessageDialog(this,
                    "⚠️ Width and height must be between 4 and 30.",
                    "Invalid Size",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int selectedIndex = modeList.getSelectedIndex();
            String className = classNames[selectedIndex];
            
            String difficulty = (String) difficultyBox.getSelectedItem();
            boolean enforceUnique = uniqueCheck.isSelected();
            
            // Launch the game directly - NO MODIFICATIONS TO YOUR GUI
            launchGame(className, width, height, difficulty, enforceUnique);
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please enter valid numbers for width and height.",
                "Invalid Input",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void launchGame(String className, int width, int height, 
                           String difficulty, boolean enforceUnique) {
        try {
            // Load the class
            Class<?> clazz = Class.forName(className);
            
            // Try constructors in order - EXACTLY as your GUIs expect them
            Object gameInstance = null;
            
            // First try: (int, int, String, boolean) - full constructor
            try {
                gameInstance = clazz.getConstructor(int.class, int.class, String.class, boolean.class)
                                   .newInstance(width, height, difficulty, enforceUnique);
            } 
            // Second try: (int, int) - size only constructor
            catch (NoSuchMethodException e1) {
                try {
                    gameInstance = clazz.getConstructor(int.class, int.class)
                                       .newInstance(width, height);
                }
                // Last try: default constructor
                catch (NoSuchMethodException e2) {
                    gameInstance = clazz.getDeclaredConstructor().newInstance();
                }
            }
            
            // Show the game window
            if (gameInstance instanceof JFrame) {
                JFrame gameFrame = (JFrame) gameInstance;
                gameFrame.setVisible(true);
                
                // Minimize launcher
                this.setState(JFrame.ICONIFIED);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Error: " + className + " is not a JFrame",
                    "Launch Error",
                    JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Failed to launch " + className + ".java\n" +
                "Error: " + ex.getMessage() + "\n\n" +
                "Make sure the compiled .class file is in the same directory.",
                "Launch Error",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UnrulyLauncher());
    }
}