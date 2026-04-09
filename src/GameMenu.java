import javafx.animation.ScaleTransition;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class GameMenu extends Application {
    private static Stage primaryStage;
    private static boolean launched = false;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        launched = true;
        Platform.setImplicitExit(false);
        showMainMenu(stage);
    }

    private void showMainMenu(Stage stage) {
        StackPane menuRoot = new StackPane();
        menuRoot.setStyle("-fx-background-color: black;"); // Letterbox framing
        Scene scene = new Scene(menuRoot, 960, 720);

        // --- ADAPTIVE SCALING FOR JAVAFX ---
        StackPane scaler = new StackPane();
        scaler.setPrefSize(960, 720);
        scaler.setMaxSize(960, 720);
        
        // Preserve 4:3 Aspect Ratio uniformly
        javafx.beans.binding.NumberBinding minScale = javafx.beans.binding.Bindings.min(
                scene.widthProperty().divide(960.0),
                scene.heightProperty().divide(720.0)
        );

        javafx.scene.transform.Scale scale = new javafx.scene.transform.Scale(1, 1, 0, 0);
        scale.xProperty().bind(minScale);
        scale.yProperty().bind(minScale);
        // Pivot from center so we can center the stack pane itself seamlessly
        scale.setPivotX(480);
        scale.setPivotY(360);
        scaler.getTransforms().add(scale);
        
        menuRoot.getChildren().add(scaler);

        // --- INTRO TEXT ---
        Text introText = new Text("PRESS ANY KEY");
        introText.setStyle("-fx-font-size: 32px; -fx-fill: white; -fx-font-weight: bold;");

        // --- BACKGROUND IMAGE ---
        ImageView introBg = new ImageView();
        try {
            introBg.setImage(new Image(getClass().getResourceAsStream("/res/title_bg.png")));
            introBg.setFitWidth(960);
            introBg.setFitHeight(720);
        } catch (Exception e) {
            System.out.println("Error loading title_bg.png: " + e.getMessage());
        }

        StackPane introPane = new StackPane(introBg, introText);
        introPane.setStyle("-fx-background-color: black;");

        // --- BLINK ANIMATION ---
        FadeTransition blink = new FadeTransition(Duration.seconds(1), introText);
        blink.setFromValue(1);
        blink.setToValue(0.2);
        blink.setCycleCount(FadeTransition.INDEFINITE);
        blink.setAutoReverse(true);
        blink.play();

        scaler.getChildren().add(introPane);

        scene.setOnKeyPressed(e -> {
            scaler.getChildren().clear();
            scaler.getChildren().add(createMenu(scaler, stage));
        });

        stage.setTitle("Dungeon Escape");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    public static void openMenu() {
        if (!launched) {
            new Thread(() -> Application.launch(GameMenu.class)).start();
        } else {
            Platform.runLater(() -> {
                if (primaryStage != null) {
                    new GameMenu().showMainMenu(primaryStage);
                }
            });
        }
    }

    private StackPane createMenu(StackPane root, Stage stage) {
        Text title = new Text("DUNGEON ESCAPE");
        title.setStyle("-fx-font-size: 36px; -fx-fill: white; -fx-font-weight: bold;");

        Button startBtn = new Button("START GAME");
        Button scoreboardBtn = new Button("SCOREBOARD");
        Button exitBtn = new Button("EXIT");

        String btnStyle = "-fx-background-color: #333; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 200px; -fx-cursor: hand;";
        startBtn.setStyle(btnStyle);
        scoreboardBtn.setStyle(btnStyle);
        exitBtn.setStyle(btnStyle);

        applyGlowEffect(startBtn);
        applyGlowEffect(scoreboardBtn);
        applyGlowEffect(exitBtn);

        VBox layout = new VBox(20, title, startBtn, scoreboardBtn, exitBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: transparent;");

        startBtn.setOnAction(e -> {
            root.getChildren().clear();
            root.getChildren().add(createModeSelection(root, stage));
        });

        scoreboardBtn.setOnAction(e -> {
            root.getChildren().clear();
            root.getChildren().add(createScoreboard(root, stage));
        });

        exitBtn.setOnAction(e -> {
            Platform.exit();
            System.exit(0);
        });

        ImageView menuBg = new ImageView();
        try {
            menuBg.setImage(new Image(getClass().getResourceAsStream("/res/menu_bg.png")));
            menuBg.setFitWidth(960);
            menuBg.setFitHeight(720);
        } catch (Exception e) {
            System.out.println("Error loading menu_bg.png: " + e.getMessage());
        }

        return new StackPane(menuBg, layout);
    }

    private StackPane createModeSelection(StackPane root, Stage stage) {
        Text title = new Text("CHOOSE YOUR MODE");
        title.setStyle("-fx-font-size: 36px; -fx-fill: white; -fx-font-weight: bold;");

        Button casualBtn = new Button("CASUAL MODE");
        Button escapeBtn = new Button("ESCAPE MODE");
        Button backBtn = new Button("BACK");

        String btnStyle = "-fx-background-color: #333; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 200px; -fx-cursor: hand;";
        casualBtn.setStyle(btnStyle);
        escapeBtn.setStyle(btnStyle);
        backBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-size: 14px; -fx-min-width: 100px; -fx-cursor: hand;");

        applyGlowEffect(casualBtn);
        applyGlowEffect(escapeBtn);
        applyGlowEffect(backBtn);

        VBox layout = new VBox(20, title, casualBtn, escapeBtn, backBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: transparent;");

        casualBtn.setOnAction(e -> {
            stage.close();
            new Thread(() -> Main.main(new String[]{"casual"})).start();
        });

        escapeBtn.setOnAction(e -> {
            stage.close();
            new Thread(() -> Main.main(new String[]{"escape"})).start();
        });

        backBtn.setOnAction(e -> {
            root.getChildren().clear();
            root.getChildren().add(createMenu(root, stage));
        });

        ImageView menuBg = new ImageView();
        try {
            menuBg.setImage(new Image(getClass().getResourceAsStream("/res/menu_bg.png")));
            menuBg.setFitWidth(960);
            menuBg.setFitHeight(720);
        } catch (Exception e) {
            System.out.println("Error loading menu_bg.png: " + e.getMessage());
        }

        return new StackPane(menuBg, layout);
    }

    private StackPane createScoreboard(StackPane root, Stage stage) {
        Text title = new Text("HIGH SCORES");
        title.setStyle("-fx-font-size: 32px; -fx-fill: red; -fx-font-weight: bold;");

        VBox casualList = new VBox(10);
        casualList.setAlignment(Pos.TOP_CENTER);
        Text casualTitle = new Text("CASUAL MODE");
        casualTitle.setStyle("-fx-font-size: 20px; -fx-fill: orange; -fx-font-weight: bold; -fx-underline: true;");
        casualList.getChildren().add(casualTitle);

        VBox escapeList = new VBox(10);
        escapeList.setAlignment(Pos.TOP_CENTER);
        Text escapeTitle = new Text("ESCAPE MODE");
        escapeTitle.setStyle("-fx-font-size: 20px; -fx-fill: cyan; -fx-font-weight: bold; -fx-underline: true;");
        escapeList.getChildren().add(escapeTitle);

        List<ScoreEntry> scores = ScoreManager.getTopScores();
        int casualRank = 1;
        int escapeRank = 1;

        for (ScoreEntry s : scores) {
            if (s.mode.equalsIgnoreCase("ESCAPE")) {
                String coinDetail = " | Coins: " + s.coins;
                Text scoreText = new Text(String.format("%d. %-8s - %d pts (Lvl %d) %s", escapeRank++, s.name, s.score, s.level, coinDetail));
                scoreText.setStyle("-fx-fill: white; -fx-font-family: 'Courier New'; -fx-font-size: 14px;");
                escapeList.getChildren().add(scoreText);
            } else {
                Text scoreText = new Text(String.format("%d. %-8s - %d pts (Lvl %d)", casualRank++, s.name, s.score, s.level));
                scoreText.setStyle("-fx-fill: white; -fx-font-family: 'Courier New'; -fx-font-size: 14px;");
                casualList.getChildren().add(scoreText);
            }
        }

        if (casualRank == 1) {
            Text empty = new Text("NO SCORES YET");
            empty.setStyle("-fx-fill: gray; -fx-font-size: 16px;");
            casualList.getChildren().add(empty);
        }
        if (escapeRank == 1) {
            Text empty = new Text("NO SCORES YET");
            empty.setStyle("-fx-fill: gray; -fx-font-size: 16px;");
            escapeList.getChildren().add(empty);
        }

        casualList.setPrefWidth(450);
        escapeList.setPrefWidth(450);

        javafx.scene.layout.HBox columns = new javafx.scene.layout.HBox(0, escapeList, casualList);
        columns.setAlignment(Pos.CENTER);

        Button backBtn = new Button("BACK TO MENU");
        backBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-size: 16px; -fx-margin-top: 20px; -fx-cursor: hand;");
        applyGlowEffect(backBtn);
        backBtn.setOnAction(e -> {
            root.getChildren().clear();
            root.getChildren().add(createMenu(root, stage));
        });

        VBox layout = new VBox(30, title, columns, backBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-padding: 50;");

        ImageView menuBg = new ImageView();
        try {
            menuBg.setImage(new Image(getClass().getResourceAsStream("/res/menu_bg.png")));
            menuBg.setFitWidth(960);
            menuBg.setFitHeight(720);
        } catch (Exception e) {
            System.out.println("Error loading menu_bg.png: " + e.getMessage());
        }

        return new StackPane(menuBg, layout);
    }

    private void applyGlowEffect(Button btn) {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.WHITE);
        glow.setRadius(20);
        glow.setSpread(0.2);

        btn.setOnMouseEntered(e -> btn.setEffect(glow));
        btn.setOnMouseExited(e -> btn.setEffect(null));
    }

    public static void main(String[] args) {
        openMenu();
    }
}