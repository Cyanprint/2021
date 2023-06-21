import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Panel extends JPanel implements ActionListener {
    String[] icons = {
        "src/Resources/Orange Blob.png",
        "src/Resources/Red Blob.png", 
        "src/Resources/Tan Blob.png", 
        "src/Resources/Yellow Blob.png", 
        "src/Resources/Wall Texture.png"
    };

    static Image[] amphipods;
    int[] amphipodX;
    int[] amphipodY;
    Image[] walls;
    final int BURROW_DEPTH;
    final int PANEL_WIDTH = 1300;
    final int PANEL_HEIGHT;

    static int xVelocity = 2;
    static int yVelocity = 2;

    private Object lock = new Object();

    public Panel(int[] input) {
        BURROW_DEPTH = (input.length) / 4;
        PANEL_HEIGHT = (BURROW_DEPTH + 3) * 100;
        amphipods = new Image[BURROW_DEPTH * 4];
        amphipodX = new int[BURROW_DEPTH * 4];
        amphipodY = new int[BURROW_DEPTH * 4];
        walls = new Image[BURROW_DEPTH == 2 ? 38 : 48];

        this.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        this.setBackground(Color.BLACK);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < BURROW_DEPTH; j++) {
                int k = BURROW_DEPTH * i + j;
                amphipods[k] = new ImageIcon(icons[i]).getImage();
                amphipodX[k] = ((input[k] - 7) % 4) * 200 + 300;
                amphipodY[k] = ((input[k] - 7) / 4) * 100 + 200;
            }
        }
        
        for (int i = 0; i < walls.length; i++) {
            walls[i] = new ImageIcon(icons[4]).getImage();
        }
    }

    public void paint(Graphics g) {
        Graphics2D graphics = (Graphics2D) g;

        super.paint(g);

        // Draw the amphipods
        for (int i = 0; i < amphipods.length; i++) {
            graphics.drawImage(amphipods[i], amphipodX[i], amphipodY[i], null);
        }

        // Upper walls
        for (int i = 0; i < 13; i++) {
            graphics.drawImage(walls[i], 100 * i, 0, null);
        }
        // Hallway walls
        graphics.drawImage(walls[13], 0, 100, null);
        graphics.drawImage(walls[14], 1200, 100, null);
        // All the burrow walls
        for (int i = 15; i < 17; i++) {
            graphics.drawImage(walls[i], 100 * (i - 15), 200, null);
            graphics.drawImage(walls[i + 2], 100 * (i - 4), 200, null);
        }
        for (int i = 0; i < BURROW_DEPTH + 1; i++) {
            for (int j = 1; j < 6; j++) {
                graphics.drawImage(walls[18 + 5 * i + j], 200 * j, 100 * i + 200, null);
            }
        }
        for (int i = 0; i < 4; i++) {
            graphics.drawImage(walls[walls.length - 4], 200 * i + 300, 100 * (BURROW_DEPTH + 2), null);
        }
    }

    public void makeMove(Move move) {
        int i = move.amphipodID();
        if (move.to().y() > move.from().y()) {
            moveX(i, 100 * (move.to().x() - move.from().x()));
            moveY(i, 100 * (move.to().y() - move.from().y()));
        } else {
            moveY(i, 100 * (move.to().y() - move.from().y()));
            moveX(i, 100 * (move.to().x() - move.from().x()));
        }
    }

    public void moveX(int id, int xDistance) {
        synchronized (lock) {
            if (xDistance > 0) {
                for (int i = 0; i < xDistance; i += xVelocity) {
                    amphipodX[id] += xVelocity;
                    repaint();
                    waitTime(5);
                }
            } else {
                for (int i = 0; i > xDistance; i -= xVelocity) {
                    amphipodX[id] -= xVelocity;
                    repaint();
                    waitTime(5);
                }
            }
        }
    }

    public void moveY(int id, int yDistance) {
        synchronized (lock) {
            if (yDistance > 0) {
                for (int i = 0; i < yDistance; i += yVelocity) {
                    amphipodY[id] += yVelocity;
                    repaint();
                    waitTime(5);
                }
            } else {
                for (int i = 0; i > yDistance; i -= yVelocity) {
                    amphipodY[id] -= yVelocity;
                    repaint();
                    waitTime(5);
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    public static void waitTime(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}