import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.io.*;

public class GamePanel extends JPanel {

    public static final int TILE = 25;
    public static final int COLS = 24;
    public static final int ROWS = 24;

    private final ArrayList<Point> snake = new ArrayList<>();
    private Point food;
    private int dx = 1, dy = 0;               // current direction
    private int pendingDx = 1, pendingDy = 0; // next direction (applied each tick)
    private int score = 0;
    private boolean gameOver = false;
    private final Random random = new Random();
    private int highScore = 0;
    private final String SCORE_FILE = "highscore.txt";

    public GamePanel() {
        setPreferredSize(new Dimension(COLS * TILE, ROWS * TILE));
        setBackground(new Color(104, 159, 90));
        setFocusable(true);

        resetGame();
        loadHighScore();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode());
            }
        });

        new Timer(180, e -> tick()).start();
    }

    // grab keyboard focus once the panel is on screen
    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    private void resetGame() {
        snake.clear();
        snake.add(new Point(7, 12));  // head
        snake.add(new Point(6, 12));
        snake.add(new Point(5, 12));  // tail
        dx = 1; dy = 0;
        pendingDx = 1; pendingDy = 0;
        score = 0;
        gameOver = false;
        spawnFood();
    }

    private void spawnFood() {
        Point p;
        do {
            p = new Point(random.nextInt(COLS), random.nextInt(ROWS));
        } while (snake.contains(p));
        food = p;
    }

    private boolean snakeContains(int x, int y)
    {
        for (int i = 0; i < snake.size() ; i++){
            Point p = snake.get(i);
            if (p.x == x && p.y == y){
                return true;
            }
        }
        return false;
    }

    private void handleKey(int key) {
        if (gameOver) {
            if (key == KeyEvent.VK_SPACE) { resetGame(); repaint(); }
            return;
        }
        // can't reverse straight back into the neck
        if ((key == KeyEvent.VK_UP    || key == KeyEvent.VK_W) && dy != 1)  { pendingDx = 0;  pendingDy = -1; }
        if ((key == KeyEvent.VK_DOWN  || key == KeyEvent.VK_S) && dy != -1) { pendingDx = 0;  pendingDy = 1; }
        if ((key == KeyEvent.VK_LEFT  || key == KeyEvent.VK_A) && dx != 1)  { pendingDx = -1; pendingDy = 0; }
        if ((key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) && dx != -1) { pendingDx = 1;  pendingDy = 0; }
    }

    private void tick() {
        if (gameOver) return;

        dx = pendingDx; dy = pendingDy;

        Point head = snake.get(0);
        Point newHead = new Point(head.x + dx, head.y + dy);

        boolean hitWall = newHead.x < 0 || newHead.x >= COLS || newHead.y < 0 || newHead.y >= ROWS;
        // the tail will move away this tick (unless we eat), so ignore it in self-collision
        boolean hitSelf = snakeContains(newHead.x, newHead.y);
        if (hitWall || hitSelf) {
            gameOver = true;
            if(score > highScore)
            {
                highScore = score;
                saveHighScore();
            }

            repaint();
            return;
        }

        snake.add(0, newHead);

        if (newHead.equals(food)) {
            score++;
            spawnFood();                      // ate: keep the tail -> grow
        } else {
            snake.remove(snake.size() - 1);   // normal move: drop the tail
        }

        repaint();
    }

    // --- high score file plumbing (Claude wrote this part) ---
    private void loadHighScore() {
        try {
            File f = new File(SCORE_FILE);
            if (f.exists()) {
                Scanner s = new Scanner(f);
                if (s.hasNextInt()) highScore = s.nextInt();
                s.close();
            }
        } catch (Exception e) {
            highScore = 0;
        }
    }

    private void saveHighScore() {
        try {
            PrintWriter w = new PrintWriter(SCORE_FILE);
            w.println(highScore);
            w.close();
        } catch (Exception e) {
            // if saving fails, just skip it
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // subtle grid lines
        g2.setColor(new Color(94, 146, 82));
        for (int c = 0; c <= COLS; c++) g2.drawLine(c * TILE, 0, c * TILE, ROWS * TILE);
        for (int r = 0; r <= ROWS; r++) g2.drawLine(0, r * TILE, COLS * TILE, r * TILE);

        // food
        g2.setColor(new Color(224, 82, 82));
        g2.fillRoundRect(food.x * TILE + 3, food.y * TILE + 3, TILE - 6, TILE - 6, 8, 8);

        // snake (head darker than body)
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);
            g2.setColor(i == 0 ? new Color(28, 90, 190) : new Color(70, 135, 225));
            g2.fillRoundRect(p.x * TILE + 1, p.y * TILE + 1, TILE - 2, TILE - 2, 8, 8);
        }

        // score
        g2.setColor(new Color(45, 52, 64));
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString("Score: " + score, 10, 20);
        g2.drawString("Best: " + highScore, 10, 40);

        // game over overlay
        if (gameOver) {
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 34));
            String msg = "Game Over";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2 - 10);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
            String sub = "Score " + score + "  —  press SPACE to restart";
            fm = g2.getFontMetrics();
            g2.drawString(sub, (getWidth() - fm.stringWidth(sub)) / 2, getHeight() / 2 + 25);
        }
    }
}
