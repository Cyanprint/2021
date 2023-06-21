import javax.swing.JFrame;

public class Frame extends JFrame{
    private Panel panel;
    public Frame(int[] input) {
        panel = new Panel(input);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.add(panel);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    public Panel panel() {
        return panel;
    }
}