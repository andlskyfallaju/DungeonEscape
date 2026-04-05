import javax.swing.JFrame;
import javafx.application.Platform;
import javafx.stage.Stage;

public class Main {
    public static JFrame window;

    public static void main(String[] args) {
        String mode = (args != null && args.length > 0) ? args[0] : "casual";

        window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(true); // Enabled adaptive scaling
        window.setTitle("Dungeon Escape [" + mode.toUpperCase() + "]");
        
        GamePanel gamePanel = new GamePanel(mode);
        window.add(gamePanel);

        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        gamePanel.requestFocusInWindow();

        gamePanel.startGameThread();
    }

    public static void quitToMenu() {
        if (window != null) {
            window.dispose();
        }
        // Launch JavaFX Menu
        Platform.runLater(() -> {
            try {
                new GameMenu().start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}