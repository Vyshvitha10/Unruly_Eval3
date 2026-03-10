package unrulyy;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class unrulyGraphsGUI extends JFrame {

    private static final String EMPTY=".";
    private static final String BLACK="B";
    private static final String WHITE="W";

    private int rows=8;
    private int cols=8;

    private String[][] board;
    private String[][] initialBoard;

    private BoardPanel boardPanel;
    private JComboBox<String> levelBox;
    private JLabel statusLabel;

    private long startTimeMillis;

    private javax.swing.Timer aiTimer;
    private JSlider speedSlider;

    private int moveCount=0;

    public unrulyGraphsGUI(){

        super("Unruly — AI Solver");

        JMenuBar bar=new JMenuBar();

        JMenu boardMenu=new JMenu("Board Size");
        bar.add(boardMenu);

        addTypeMenuItem(boardMenu,"4 x 4",4,4);
        addTypeMenuItem(boardMenu,"6 x 6",6,6);
        addTypeMenuItem(boardMenu,"8 x 8",8,8);
        addTypeMenuItem(boardMenu,"10 x 10",10,10);
        addTypeMenuItem(boardMenu,"12 x 12",12,12);
        addTypeMenuItem(boardMenu,"14 x 14",14,14);

        JMenuItem custom=new JMenuItem("Custom");
        custom.addActionListener(e->openCustomDialog());
        boardMenu.addSeparator();
        boardMenu.add(custom);

        setJMenuBar(bar);

        JPanel top=new JPanel(new FlowLayout(FlowLayout.CENTER,15,10));
        top.setForeground(Color.WHITE);
        JButton newBtn=new JButton("New Game");
        JButton restartBtn=new JButton("Restart");
        JButton solveBtn=new JButton("Solve");

        styleButton(newBtn);
        styleButton(restartBtn);
        styleButton(solveBtn);

        newBtn.addActionListener(e->startGame());
        restartBtn.addActionListener(e->startGame());
        solveBtn.addActionListener(e->solveInstantly());

        levelBox=new JComboBox<>(new String[]{"Trivial","Easy","Medium","Hard"});
        styleCombo(levelBox);

        speedSlider=new JSlider(50,1000,400);
        speedSlider.setBackground(new Color(40,40,40));
        speedSlider.addChangeListener(e->{
            if(aiTimer!=null)
                aiTimer.setDelay(speedSlider.getValue());
        });

        statusLabel=new JLabel("Ready");
        statusLabel.setForeground(Color.BLACK);

        
        top.add(levelBox);
        top.add(newBtn);
        top.add(restartBtn);
        top.add(solveBtn);
        top.add(statusLabel);

        add(top,BorderLayout.NORTH);

        JPanel aiPanel=new JPanel(new FlowLayout(FlowLayout.CENTER,10,5));
        aiPanel.setBackground(new Color(40,40,40));

        JButton startAI=new JButton("Start AI");
        JButton stopAI=new JButton("Pause AI");
        JButton stepAI=new JButton("Step AI");

        styleButton(startAI);
        styleButton(stopAI);
        styleButton(stepAI);

        startAI.addActionListener(e->startAI());
        stopAI.addActionListener(e->stopAI());
        stepAI.addActionListener(e->stepAI());

        aiPanel.add(startAI);
        aiPanel.add(stopAI);
        aiPanel.add(stepAI);

        aiPanel.add(new JLabel("Speed"));
        aiPanel.add(speedSlider);

        add(aiPanel,BorderLayout.SOUTH);

        boardPanel=new BoardPanel();
        add(boardPanel,BorderLayout.CENTER);

        setSize(750,780);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        startGame();
    }

    private void styleButton(JButton btn){

        btn.setFont(new Font("SansSerif",Font.BOLD,14));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6,14,6,14));

        String text = btn.getText();

        if(text.contains("New"))
            btn.setBackground(new Color(56,142,60));
        else if(text.contains("Restart"))
            btn.setBackground(new Color(255,152,0));
        else if(text.contains("Solve"))
            btn.setBackground(new Color(211,47,47));
        else if(text.contains("Start"))
            btn.setBackground(new Color(56,142,60));
        else if(text.contains("Pause") || text.contains("Stop"))
            btn.setBackground(new Color(211,47,47));
        else if(text.contains("Step"))
            btn.setBackground(new Color(255,152,0));
        else
            btn.setBackground(new Color(70,130,180));
    }

    private void styleCombo(JComboBox<String> box){
        box.setBackground(new Color(60,60,60));
        box.setForeground(Color.WHITE);
        box.setFont(new Font("SansSerif",Font.BOLD,14));
    }

    private void addTypeMenuItem(JMenu menu,String label,int r,int c){
        JMenuItem item=new JMenuItem(label);
        item.addActionListener(e->{ rows=r; cols=c; startGame(); });
        menu.add(item);
    }

    private void openCustomDialog(){

        JTextField widthField=new JTextField(String.valueOf(cols),5);
        JTextField heightField=new JTextField(String.valueOf(rows),5);

        JComboBox<String> difficultyBox=
                new JComboBox<>(new String[]{"Trivial","Easy","Medium","Hard"});

        JPanel panel=new JPanel(new GridBagLayout());
        GridBagConstraints gc=new GridBagConstraints();

        gc.insets=new Insets(5,5,5,5);
        gc.anchor=GridBagConstraints.WEST;

        gc.gridx=0; gc.gridy=0;
        panel.add(new JLabel("Width (columns)"),gc);

        gc.gridx=1;
        panel.add(widthField,gc);

        gc.gridx=0; gc.gridy=1;
        panel.add(new JLabel("Height (rows)"),gc);

        gc.gridx=1;
        panel.add(heightField,gc);

        gc.gridx=0; gc.gridy=3;
        panel.add(new JLabel("Difficulty"),gc);

        gc.gridx=1;
        panel.add(difficultyBox,gc);

        int result=JOptionPane.showConfirmDialog(
                this,panel,"Unruly configuration",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if(result==JOptionPane.OK_OPTION){

            try{

                int w=Integer.parseInt(widthField.getText());
                int h=Integer.parseInt(heightField.getText());

                if(w<4||h<4){
                    JOptionPane.showMessageDialog(this,"Minimum size 4x4");
                    return;
                }

                if(w%2!=0||h%2!=0){
                    JOptionPane.showMessageDialog(this,"Rows and columns must be EVEN");
                    return;
                }

                cols=w;
                rows=h;

                levelBox.setSelectedItem(difficultyBox.getSelectedItem());

                startGame();

            }catch(Exception ex){
                JOptionPane.showMessageDialog(this,"Invalid number");
            }
        }
    }

    private void startGame(){

        board=new String[rows][cols];
        initialBoard=new String[rows][cols];

        moveCount=0;

        for(int r=0;r<rows;r++)
            for(int c=0;c<cols;c++)
                board[r][c]=EMPTY;

        String[][] solved=generateSolvedBoard();

        double ratio=switch((String)levelBox.getSelectedItem()){
            case "Trivial"->0.55;
            case "Easy"->0.45;
            case "Medium"->0.35;
            default->0.25;
        };

        int total=rows*cols;
        int keep=(int)(total*ratio);

        Random rand=new Random();

        for(int r=0;r<rows;r++)
            System.arraycopy(solved[r],0,board[r],0,cols);

        int remove=total-keep;

        while(remove>0){

            int r=rand.nextInt(rows);
            int c=rand.nextInt(cols);

            if(!board[r][c].equals(EMPTY)){
                board[r][c]=EMPTY;
                remove--;
            }
        }

        startTimeMillis=System.currentTimeMillis();

        if(aiTimer!=null)
            aiTimer.stop();

        aiTimer=new javax.swing.Timer(speedSlider.getValue(),e->aiTick());

        updateStatus();
        boardPanel.repaint();
    }

    private void aiTick(){

        if(isBoardSolved()){
            aiTimer.stop();

            JOptionPane.showMessageDialog(
                    this,
                    "AI solved puzzle!\nMoves: "+moveCount+
                            "\nTime: "+getElapsedTimeText()
            );
            return;
        }

        computerPlayMove();
    }

    private void startAI(){ if(aiTimer!=null) aiTimer.start(); }
    private void stopAI(){ if(aiTimer!=null) aiTimer.stop(); }
    private void stepAI(){ aiTick(); }

    private void solveInstantly(){
        board=generateSolvedBoard();
        boardPanel.repaint();
    }

    private String[][] generateSolvedBoard(){

        String[][] solved=new String[rows][cols];

        for(int r=0;r<rows;r++)
            for(int c=0;c<cols;c++)
                solved[r][c]=EMPTY;

        solveCell(solved,0);

        return solved;
    }

    private boolean solveCell(String[][] grid,int pos){

        if(pos==rows*cols) return true;

        int r=pos/cols;
        int c=pos%cols;

        for(String color:new String[]{BLACK,WHITE}){

            if(isValidPlace(grid,r,c,color)){

                grid[r][c]=color;

                if(solveCell(grid,pos+1)) return true;

                grid[r][c]=EMPTY;
            }
        }

        return false;
    }

    private boolean isValidPlace(String[][] grid,int r,int c,String color){

        String old=grid[r][c];
        grid[r][c]=color;

        boolean ok=isValidRow(grid[r])&&isValidColumn(grid,c);

        grid[r][c]=old;

        return ok;
    }

    private boolean isValidRow(String[] row){

        for(int i=0;i<row.length-2;i++)
            if(!row[i].equals(EMPTY)
                    &&row[i].equals(row[i+1])
                    &&row[i].equals(row[i+2]))
                return false;

        return true;
    }

    private boolean isValidColumn(String[][] grid,int c){

        String[] col=new String[rows];

        for(int r=0;r<rows;r++)
            col[r]=grid[r][c];

        return isValidRow(col);
    }

    private boolean isBoardSolved(){

        for(int r=0;r<rows;r++)
            for(int c=0;c<cols;c++)
                if(board[r][c].equals(EMPTY))
                    return false;

        return true;
    }

    class Candidate{

        int r,c;
        int validCount;
        int compSize;

        Candidate(int r,int c,int v,int s){
            this.r=r;
            this.c=c;
            this.validCount=v;
            this.compSize=s;
        }
    }

    private void computerPlayMove(){

        List<Candidate> cand=new ArrayList<>();

        for(int r=0;r<rows;r++)
            for(int c=0;c<cols;c++)
                if(board[r][c].equals(EMPTY)){

                    int valid=0;

                    if(isValidPlace(board,r,c,BLACK))valid++;
                    if(isValidPlace(board,r,c,WHITE))valid++;

                    int comp=bfsComponentSize(r,c);

                    cand.add(new Candidate(r,c,valid,comp));
                }

        cand.removeIf(c->c.validCount==0);

        if(cand.isEmpty()) return;

        mergeSortCandidates(cand);

        Candidate pick=cand.get(0);

        if(isValidPlace(board,pick.r,pick.c,BLACK))
            board[pick.r][pick.c]=BLACK;
        else
            board[pick.r][pick.c]=WHITE;

        moveCount++;

        updateStatus();
        boardPanel.repaint();
    }

    private void mergeSortCandidates(List<Candidate> list){

        if(list.size()<=1) return;

        int mid=list.size()/2;

        List<Candidate> left=new ArrayList<>(list.subList(0,mid));
        List<Candidate> right=new ArrayList<>(list.subList(mid,list.size()));

        mergeSortCandidates(left);
        mergeSortCandidates(right);

        merge(list,left,right);
    }

    private void merge(List<Candidate> res,List<Candidate> left,List<Candidate> right){

        int i=0,j=0,k=0;

        while(i<left.size()&&j<right.size()){

            if(compare(left.get(i),right.get(j))<=0)
                res.set(k++,left.get(i++));
            else
                res.set(k++,right.get(j++));
        }

        while(i<left.size()) res.set(k++,left.get(i++));
        while(j<right.size()) res.set(k++,right.get(j++));
    }

    private int compare(Candidate a,Candidate b){

        if(a.validCount!=b.validCount)
            return a.validCount-b.validCount;

        if(a.compSize!=b.compSize)
            return a.compSize-b.compSize;

        return (a.r*cols+a.c)-(b.r*cols+b.c);
    }

    private int bfsComponentSize(int sr,int sc){

        boolean[][] vis=new boolean[rows][cols];
        Deque<int[]> dq=new ArrayDeque<>();

        dq.add(new int[]{sr,sc});
        vis[sr][sc]=true;

        int size=0;

        while(!dq.isEmpty()){

            int[] p=dq.poll();
            size++;

            int r=p[0],c=p[1];

            int[][] d={{1,0},{-1,0},{0,1},{0,-1}};

            for(int[] x:d){

                int nr=r+x[0];
                int nc=c+x[1];

                if(nr<0||nc<0||nr>=rows||nc>=cols)continue;
                if(vis[nr][nc])continue;
                if(!board[nr][nc].equals(EMPTY))continue;

                vis[nr][nc]=true;
                dq.add(new int[]{nr,nc});
            }
        }

        return size;
    }

    private void updateStatus(){

        if(isBoardSolved())
            statusLabel.setText("Solved | Moves: "+moveCount+" | Time "+getElapsedTimeText());
        else
            statusLabel.setText("AI solving | Moves: "+moveCount);
    }

    private String getElapsedTimeText(){

        long elapsed=System.currentTimeMillis()-startTimeMillis;

        long sec=elapsed/1000;
        long min=sec/60;
        sec%=60;

        return min+":"+String.format("%02d",sec);
    }

    class BoardPanel extends JPanel{
    	BoardPanel(){
    	    setBackground(new Color(30,30,30));
    	}

        protected void paintComponent(Graphics g){

            super.paintComponent(g);

            int w=getWidth()/cols;
            int h=getHeight()/rows;

            for(int r=0;r<rows;r++)
                for(int c=0;c<cols;c++){

                	g.setColor(new Color(50,50,50));
                	g.fillRect(c*w,r*h,w,h);

                    if(board[r][c].equals(BLACK)){
                        g.setColor(Color.BLACK);
                        g.fillRect(c*w+2,r*h+2,w-4,h-4);
                    }

                    if(board[r][c].equals(WHITE)){
                        g.setColor(Color.WHITE);
                        g.fillRect(c*w+2,r*h+2,w-4,h-4);
                    }

                    g.setColor(new Color(70,70,70));
                    g.drawRect(c*w,r*h,w,h);
                }
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(unrulyGraphsGUI::new);
    }
}