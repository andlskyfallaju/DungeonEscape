import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GamePanel extends JPanel implements Runnable {
    KeyHandler keyH = new KeyHandler();
    Player player = new Player(this, keyH);
    Map map = new Map(this);
    int score = 0;
    int level = 1;
    int lives = 5;

    public enum GameMode { CASUAL, ESCAPE }
    public GameMode currentMode = GameMode.CASUAL;
    private List<Point> coins = new CopyOnWriteArrayList<>();
    private List<Point> powerups = new CopyOnWriteArrayList<>();
    public boolean exitLocked = false;
    public long slowTimer = 0; // Duration of slow-down effect in frames

    public List<Arrow> arrows = new CopyOnWriteArrayList<>();
    public List<ExplosionTracker> explosionTrackers = new CopyOnWriteArrayList<>();
    public List<Crystal> crystals = new CopyOnWriteArrayList<>();
    public List<ArcherBoss> archerBosses = new CopyOnWriteArrayList<>();
    public boolean isBossLevel = false;
    public int invincibilityFrames = 0;
    public int staggerTimer = 0; // Level 10 Chase mechanic

    // Level 10 Endgame Phase
    public int level10Phase = 0; // 0: Normal, 1: Arena, 2: Chase
    public double cameraX = 0;
    public double voidWallX = -300;
    public Rectangle finalPortal = null;
    public int portalWinTimer = 0;
    public int voidStopTimer = 0;

    // Snapshot for "Same Level" restart logic
    int[][] initialMap;
    int initialPlayerX, initialPlayerY;
    Point[] initialEnemyPositions;
    private int coinsCollected = 0;
    private boolean scoreSaved = false;

    // Progression & Checkpoints
    private int checkpointLevel = 1;
    private int checkpointScore = 0;
    private int checkpointCoins = 0;
    public int checkpointLevel10Phase = 0;
    private int levelsClearedInRow = 0;
    private int triesRemaining = 3;
    private boolean isTryLost = false;
    private int previousCrystalCount = 0;

    final int tileSize = 48;
    int maxScreenCol = 20;
    int maxScreenRow = 15;
    boolean gameOver = false;
    boolean gameWon = false;
    Enemy[] enemies = new Enemy[3];
    boolean gameStarted = false;
    long startTime;
    int countdown = 3;
    public int ghostFreezeTimer = 0;
    public boolean godMode = false;
    public int frameCount = 0;
    public boolean paused = false;
    public boolean showTutorial = false;
    private int tutorialPhase = 0; // 1: Escape Mode Intro, 2: Boss Fight, 3: Casual Mode Intro
    private boolean level1TutorialSeen = false;
    private boolean bossTutorialSeen = false;
    private boolean casualTutorialSeen = false;

    private String hudMessage = "";
    private int hudMessageTimer = 0;

    public BufferedImage splashImage, coinImg, powerupImg, lockedDoorImg;
    public BufferedImage voidWallImg, finalPortalImg, crystalImg, bossImg;

    // Polish Features
    public int shakeTimer = 0;
    public int shakeIntensity = 0;
    public int totalRetries = 0;
    public long gameStartTime;
    public long gameEndTime;
    private String bossTaunt = "";
    private int bossTauntTimer = 0;

    int screenWidth = tileSize * maxScreenCol;
    int screenHeight = tileSize * maxScreenRow;

    Thread gameThread;
    private boolean isLifeLost = false;
    private Rectangle menuBtnRect = new Rectangle(0, 0, 200, 40);
    private Rectangle pauseMenuBtnRect = new Rectangle(0, 0, 200, 40);

    public GamePanel() {
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setLayout(null);

        // --- MOUSE LISTENER FOR MANUAL BUTTON ---
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (gameOver || gameWon || isLifeLost) {
                    int logicalWidth = 20 * tileSize;
                    int logicalHeight = 15 * tileSize;
                    double scaleX = (double)getWidth() / logicalWidth;
                    double scaleY = (double)getHeight() / logicalHeight;
                    double scaleFactor = Math.min(scaleX, scaleY);

                    int offsetX = (int)(getWidth() - logicalWidth * scaleFactor) / 2;
                    int offsetY = (int)(getHeight() - logicalHeight * scaleFactor) / 2;

                    int lx = (int)((e.getX() - offsetX) / scaleFactor);
                    int ly = (int)((e.getY() - offsetY) / scaleFactor);
                    int mouseX = (int)((e.getX() - offsetX) / scaleFactor);
                    int mouseY = (int)((e.getY() - offsetY) / scaleFactor);

                    if (menuBtnRect.contains(mouseX, mouseY)) {
                        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(GamePanel.this);
                        if (frame != null) frame.dispose();
                        GameMenu.openMenu();
                    }
                }

                if (paused && !showTutorial) {
                    int logicalWidth = 20 * tileSize;
                    int logicalHeight = 15 * tileSize;
                    double scaleX = (double)getWidth() / logicalWidth;
                    double scaleY = (double)getHeight() / logicalHeight;
                    double scaleFactor = Math.min(scaleX, scaleY);

                    int offsetX = (int)(getWidth() - logicalWidth * scaleFactor) / 2;
                    int offsetY = (int)(getHeight() - logicalHeight * scaleFactor) / 2;

                    int mouseX = (int)((e.getX() - offsetX) / scaleFactor);
                    int mouseY = (int)((e.getY() - offsetY) / scaleFactor);

                    if (pauseMenuBtnRect.contains(mouseX, mouseY)) {
                        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(GamePanel.this);
                        if (frame != null) frame.dispose();
                        GameMenu.openMenu();
                    }
                }
            }
        });

        startTime = System.currentTimeMillis();

        try {
            splashImage = ImageIO.read(getClass().getResourceAsStream("/res/splash.png"));
            coinImg = ImageIO.read(getClass().getResourceAsStream("/res/coin.png"));
            powerupImg = ImageIO.read(getClass().getResourceAsStream("/res/powerup_slow.png"));
            lockedDoorImg = ImageIO.read(getClass().getResourceAsStream("/res/door_locked.png"));
            voidWallImg = ImageIO.read(getClass().getResourceAsStream("/res/void_wall.png"));
            finalPortalImg = ImageIO.read(getClass().getResourceAsStream("/res/final_portal.png"));
            crystalImg = ImageIO.read(getClass().getResourceAsStream("/res/crystal.png"));
            bossImg = ImageIO.read(getClass().getResourceAsStream("/res/archer_boss.png"));
        } catch (Exception e) {
            System.out.println("Error loading game assets: " + e.getMessage());
        }

        // Initialize Level 1
        generateLevel();
    }

    public GamePanel(String modeStr) {
        this(); // Initialize common components
        this.currentMode = (modeStr != null && modeStr.equalsIgnoreCase("escape")) ? GameMode.ESCAPE : GameMode.CASUAL;
        generateLevel(); // Re-generate level with the selected mode logic
    }

    private void generateLevel() {
        // --- LOGICAL GRID LOCK ---
        maxScreenCol = 20;
        maxScreenRow = 15;
        if (level == 1) {
            screenResize();
        }

        // --- NEW: Status Check (Moved to top for checkpoint accuracy) ---
        isBossLevel = (currentMode == GameMode.ESCAPE && (level % 5 == 0));
        level10Phase = (currentMode == GameMode.ESCAPE && level == 10) ? 1 : 0;

        // --- Entrance Checkpoint (Escape Mode Only) ---
        if (currentMode == GameMode.ESCAPE && (isBossLevel || level == 10)) {
            saveCheckpointState();
            checkpointLevel = level;
            checkpointLevel10Phase = level10Phase;
        }

        // Reset state for items
        coins.clear();
        powerups.clear();
        arrows.clear();
        explosionTrackers.clear();
        crystals.clear();
        archerBosses.clear();

        cameraX = 0;
        voidWallX = -500;
        finalPortal = null;
        portalWinTimer = 0;

        invincibilityFrames = 0;
        slowTimer = 0;
        staggerTimer = 0;
        exitLocked = (currentMode == GameMode.ESCAPE);
        isLifeLost = false;

        if (isBossLevel) {
            if (level == 10) {
                showTutorial = true;
                tutorialPhase = 4;
            } else if (!bossTutorialSeen) {
                showTutorial = true;
                tutorialPhase = 2;
                bossTutorialSeen = true;
            }
        }

        if (isBossLevel || level10Phase > 0) {
            map.loadArenaMap();
        } else {
            map.loadMap(level);
        }

        Point p = map.getRandomFreeTile();
        player.x = p.x * tileSize;
        player.y = p.y * tileSize;

        Point e;
        if (isBossLevel || level10Phase > 0) {
            e = map.placeExitFarFromPlayer(p.x, p.y);

            if (level == 10) {
                ArcherBoss b1 = new ArcherBoss(this, (maxScreenCol/4) * tileSize, (maxScreenRow/2) * tileSize);
                ArcherBoss b2 = new ArcherBoss(this, (3*maxScreenCol/4) * tileSize, (maxScreenRow/2) * tileSize);
                b1.hp = 10; b2.hp = 10;
                archerBosses.add(b1);
                archerBosses.add(b2);
            } else {
                ArcherBoss b = new ArcherBoss(this, (maxScreenCol/2) * tileSize, (maxScreenRow/2) * tileSize);
                archerBosses.add(b);
            }

            int crystalGoal = 5;
            previousCrystalCount = crystalGoal;

            enemies = new Enemy[0]; // No standard ghosts

            for (int i = 0; i < crystalGoal; i++) {
                Point pt = map.getRandomFreeTileFarFromAll(crystals, 5);
                if (pt != null) {
                    crystals.add(new Crystal(this, pt.x, pt.y));
                }
            }
        } else {
            e = map.placeExitFarFromPlayer(p.x, p.y);
            map.ensurePathExists(p.x, p.y, e.x, e.y);

            // --- SPAWN ITEMS (ESCAPE MODE) ---
            if (currentMode == GameMode.ESCAPE) {
                int coinCount = (level < 5) ? 3 : 4 + (level - 5) / 2;
                for (int i = 0; i < coinCount; i++) {
                    coins.add(map.getRandomFreeTile());
                }
                powerups.add(map.getRandomFreeTile());
            }

            int enemyCount = Math.min(12, 2 + (level / 2));
            enemies = new Enemy[enemyCount];
            for(int i = 0; i < enemies.length; i++) {
                Point ep = map.getRandomFreeTileFarFrom(p, 5);
                enemies[i] = new Enemy(this, ep.x * tileSize, ep.y * tileSize);
            }
        }

        saveLevelState(); // Snapshot for potential deaths

        // --- TUTORIAL TRIGGERS ---
        if (currentMode == GameMode.ESCAPE) {
            if (level == 1 && !level1TutorialSeen) {
                showTutorial = true;
                tutorialPhase = 1;
                level1TutorialSeen = true;
            } else if (isBossLevel && !bossTutorialSeen) {
                showTutorial = true;
                tutorialPhase = 2;
                bossTutorialSeen = true;
            }
        } else if (currentMode == GameMode.CASUAL) {
            if (level == 1 && !casualTutorialSeen) {
                showTutorial = true;
                tutorialPhase = 3;
                casualTutorialSeen = true;
            }
        }

        // Reset countdown logic
        startTime = System.currentTimeMillis();
        gameStarted = false;
        countdown = 3;
        ghostFreezeTimer = 0;
    }

    private void saveLevelState() {
        // Save Map
        initialMap = new int[maxScreenRow][maxScreenCol];
        for(int r = 0; r < maxScreenRow; r++) {
            System.arraycopy(map.map[r], 0, initialMap[r], 0, maxScreenCol);
        }
        // Save Player
        initialPlayerX = player.x;
        initialPlayerY = player.y;
        // Save Enemies
        initialEnemyPositions = new Point[enemies.length];
        for(int i = 0; i < enemies.length; i++) {
            initialEnemyPositions[i] = new Point(enemies[i].x, enemies[i].y);
        }
    }

    private void loadLevelState() {
        // Restore Map
        for(int r = 0; r < maxScreenRow; r++) {
            System.arraycopy(initialMap[r], 0, map.map[r], 0, maxScreenCol);
        }
        // Restore Player
        player.x = initialPlayerX;
        player.y = initialPlayerY;
        // Restore Enemies
        for(int i = 0; i < enemies.length; i++) {
            enemies[i].x = initialEnemyPositions[i].x;
            enemies[i].y = initialEnemyPositions[i].y;
            enemies[i].path.clear();
        }

        // Restore Boss
        arrows.clear();
        for (ArcherBoss ab : archerBosses) {
            ab.x = (maxScreenCol/2) * tileSize;
            ab.y = (maxScreenRow/2) * tileSize;
            ab.cooldown = 0;
        }

        startTime = System.currentTimeMillis();
        gameStarted = false;
        countdown = 3;
        ghostFreezeTimer = 0;

        // Items persist on death or we could re-spawn them.
        // For simplicity, let's just unlock the exit if no coins left.
        if (currentMode == GameMode.ESCAPE && coins.isEmpty()) {
            exitLocked = false;
        }
    }

    public boolean checkPlayerHit() {
        if (godMode) return false;
        if (invincibilityFrames > 0) return false;

        Rectangle playerRect = player.getBounds();

        // Ghost Check
        for(int i = 0; i < enemies.length; i++) {
            if (enemies[i] != null) {
                Rectangle enemyRect = enemies[i].getBounds();
                if(playerRect.intersects(enemyRect)) {
                    return true;
                }
            }
        }

        // Boss Check
        for (ArcherBoss ab : archerBosses) {
            if (playerRect.intersects(ab.getBounds())) {
                return true;
            }
        }

        // Arrow Check
        for (int i = 0; i < arrows.size(); i++) {
            if (arrows.get(i).active && playerRect.intersects(arrows.get(i).getBounds())) {
                arrows.get(i).active = false;
                return true;
            }
        }

        return false;
    }

    public void bossHitPlayer() {
        if (godMode) return;
        if (invincibilityFrames <= 0) {
            lives--;
            invincibilityFrames = 180; // 3 seconds invulnerability
            if (lives <= 0) {
                terminateGame();
            }

            // Trigger screenshake on hit
            shakeTimer = 10;
            shakeIntensity = 8;

            // Flash bosses
            for (ArcherBoss ab : archerBosses) {
                ab.triggerFlash();
            }
        }
    }

    public void bossDefeated() {
        archerBosses.clear();
        arrows.clear();
        explosionTrackers.clear();
        exitLocked = false; // Door unlocks!
        score += 500;

        // Update checkpoint for NEXT level
        saveCheckpointState();
        checkpointLevel = level + 1;

        hudMessage = "CHECKPOINT REACHED!";
        hudMessageTimer = 180; // 3 seconds
    }

    private void saveCheckpointState() {
        checkpointLevel = level;
        checkpointScore = score;
        checkpointCoins = coinsCollected;
        checkpointLevel10Phase = level10Phase;
    }

    private void terminateGame() {
        if (currentMode == GameMode.ESCAPE && triesRemaining > 1) {
            triesRemaining--;
            isTryLost = true;
            return;
        }

        gameOver = true;
        triesRemaining = 0;
        showSaveScorePrompt();
    }

    private void showSaveScorePrompt() {
        if (!scoreSaved) {
            scoreSaved = true;
            String finalScore = String.valueOf(score);
            String finalLevel = String.valueOf(level);
            String finalMode = currentMode.toString().toUpperCase();
            String finalCoins = String.valueOf(coinsCollected);

            // Show prompt on UI thread
            SwingUtilities.invokeLater(() -> {
                String name = JOptionPane.showInputDialog(this, (gameWon ? "DUNGEON ESCAPED!" : "GAME OVER!") + " Final Score: " + finalScore + "\nEnter your name:", "Save Score", JOptionPane.PLAIN_MESSAGE);
                if (name != null && !name.trim().isEmpty()) {
                    ScoreManager.saveScore(name, Integer.parseInt(finalScore), Integer.parseInt(finalLevel), finalMode, Integer.parseInt(finalCoins));
                }
            });
        }
    }

    public boolean playerReachedExit() {
        if (exitLocked) return false;

        Rectangle playerBounds = player.getBounds();
        for(int r = 0; r < maxScreenRow; r++) {
            for(int c = 0; c < maxScreenCol; c++) {
                if(map.map[r][c] == 2) {
                    int exitX = c * tileSize;
                    int exitY = r * tileSize;
                    int shrink = tileSize / 4;
                    Rectangle exitHitbox = new Rectangle(exitX + shrink, exitY + shrink, tileSize / 2, tileSize / 2);
                    if(playerBounds.intersects(exitHitbox)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void screenResize() {
        screenWidth = tileSize * maxScreenCol;
        screenHeight = tileSize * maxScreenRow;
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.revalidate();
        java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
        if(window != null) {
            window.pack();
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
        gameStartTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while(gameThread != null) {
            update();
            repaint();
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        frameCount++;
        // Toggle Pause
        if (keyH.escPressed) {
            paused = !paused;
            keyH.escPressed = false;
        }

        if (paused) return;
        if (isTryLost || isLifeLost) {
            if (keyH.restartPressed) {
                keyH.restartPressed = false;
                restartGame();
            }
            return;
        }

        // Handle Invincibility
        if (invincibilityFrames > 0) {
            invincibilityFrames--;
        }

        // Handle Slow Power-up timer
        if (slowTimer > 0) {
            slowTimer--;
        }

        if(gameOver || gameWon) {
            if(keyH.restartPressed) {
                keyH.restartPressed = false;
                restartGame();
            }
            return;
        }

        if (showTutorial) {
            if (keyH.enterPressed) {
                showTutorial = false;
                keyH.enterPressed = false;
                startTime = System.currentTimeMillis(); // Reset countdown
            }
            return;
        }

        if (hudMessageTimer > 0) hudMessageTimer--;
        if (staggerTimer > 0) staggerTimer--;
        if (ghostFreezeTimer > 0) ghostFreezeTimer--;

        // --- GOD MODE TOGGLE ---
        if (keyH.gPressed) {
            keyH.gPressed = false;
            godMode = true; // Stays true until full restart
            hudMessage = "GOD MODE ENABLED!";
            hudMessageTimer = 120;
        }

        // --- DEBUG CHEAT: SKIP TO LEVEL 10 CHASE ---
        if (keyH.lPressed) {
            keyH.lPressed = false;
            level = 10;
            level10Phase = 2;
            archerBosses.clear();
            crystals.clear();
            coins.clear();
            enemies = new Enemy[0];
            map.loadChaseMap();
            player.x = 100;
            player.y = (maxScreenRow / 2) * tileSize;
            voidWallX = -400;
            cameraX = 0;
            triesRemaining = 1;
            lives = 5;
            showTutorial = true;
            tutorialPhase = 5;
            saveCheckpointState();
            hudMessage = "RUN! THE VOID IS COMING!";
            hudMessageTimer = 180;
            return;
        }

        // --- DEBUG CHEAT: SKIP TO LEVEL 9 ---
        if (keyH.pPressed) {
            keyH.pPressed = false;
            level = 9;
            generateLevel();
            return;
        }

        // --- DEBUG CHEAT: SKIP TO LEVEL 5 ---
        if (keyH.oPressed) {
            keyH.oPressed = false;
            level = 5;
            generateLevel();
            return;
        }

        if(!gameStarted) {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            countdown = 3 - (int)elapsed;
            if(countdown <= 0) {
                gameStarted = true;
                if (level > 5) {
                    ghostFreezeTimer = 120; // 2 seconds ghost freeze
                }
            }
            return;
        }

        if (level10Phase == 1) {
            updateLevel10Arena();
        } else if (level10Phase == 2) {
            updateLevel10Chase();
            return; // Chase has unique movement/collision
        }

        player.update();

        // --- CHECK ITEM COLLISIONS ---
        Rectangle pRect = player.getBounds();

        // Coins
        for (int i = 0; i < coins.size(); i++) {
            Point c = coins.get(i);
            Rectangle cRect = new Rectangle(c.x * tileSize, c.y * tileSize, tileSize, tileSize);
            if (pRect.intersects(cRect)) {
                coins.remove(i);
                score += 50;
                coinsCollected++;
                i--;
                if (coins.isEmpty()) exitLocked = false;
            }
        }

        // Power-ups
        for (int i = 0; i < powerups.size(); i++) {
            Point pu = powerups.get(i);
            Rectangle puRect = new Rectangle(pu.x * tileSize, pu.y * tileSize, tileSize, tileSize);
            if (pRect.intersects(puRect)) {
                powerups.remove(i);
                int seconds = (level < 5) ? (int)(Math.random() * 3) + 1 : (int)(Math.random() * 4) + 3;
                slowTimer = seconds * 60;
                i--;
            }
        }

        for (ArcherBoss ab : archerBosses) {
            ab.update(player.x, player.y);
        }

        for (int i = 0; i < crystals.size(); i++) {
            Crystal c = crystals.get(i);
            if (!c.active) {
                crystals.remove(i);
                i--;
            }
        }

        for (int i = 0; i < archerBosses.size(); i++) {
            ArcherBoss ab = archerBosses.get(i);
            if (ab.hp <= 0) {
                archerBosses.remove(i);
                i--;
            }
        }

        for (int i = 0; i < arrows.size(); i++) {
            Arrow a = arrows.get(i);
            a.update();
            if (i < arrows.size() && arrows.get(i) == a && !a.active) {
                arrows.remove(i);
                i--;
            }
        }

        for (int i = 0; i < explosionTrackers.size(); i++) {
            ExplosionTracker et = explosionTrackers.get(i);
            et.update(player.x, player.y);
            // Defeating the boss clears this list, we verify it wasn't destroyed mid-iteration
            if (i < explosionTrackers.size() && explosionTrackers.get(i) == et && et.phase == 3) {
                explosionTrackers.remove(i);
                i--;
            }
        }

        for(Enemy e : enemies) {
            if(e != null) e.update(player.x, player.y);
        }

        if(invincibilityFrames <= 0 && checkPlayerHit()) {
            if (isBossLevel) {
                bossHitPlayer(); // special instant no-reset damage
            } else {
                lives--;
                invincibilityFrames = 60; 
                levelsClearedInRow = 0; // Reset streak on death
                if(lives <= 0) {
                    terminateGame();
                } else {
                    isLifeLost = true;
                    keyH.restartPressed = false; // reset flag
                }
            }
        }

        if(!(currentMode == GameMode.ESCAPE && level == 10) && playerReachedExit()) {
            level++;
            score += 100;

            // Life Regen Logic
            levelsClearedInRow++;
            if (levelsClearedInRow >= 3) {
                if (lives < 5) {
                    lives++;
                    hudMessage = "LIFE RECOVERED!";
                } else {
                    score += 200; // Bonus for full health
                    hudMessage = "LEVEL STREAK BONUS!";
                }
                levelsClearedInRow = 0;
                hudMessageTimer = 120;
            }

            generateLevel();
        }
    }

    private void restartGame() {
        if (isTryLost) {
            // Restore from Checkpoint
            level = checkpointLevel;
            level10Phase = checkpointLevel10Phase;
            score = checkpointScore;
            coinsCollected = checkpointCoins;
            lives = 5;
            isTryLost = false;
            isLifeLost = false;
            gameOver = false;

            // --- SAFETY NET ---
            slowTimer = 300; // 5 seconds
            invincibilityFrames = 180; // 3 seconds

            if (level10Phase == 2) {
                map.loadChaseMap(); // Re-load the unique chase map
                player.x = 100;
                player.y = (maxScreenRow / 2) * tileSize;
                voidWallX = -400;
                cameraX = 0;
            } else {
                generateLevel();
            }
            return;
        } else if (isLifeLost) {
            isLifeLost = false;
            loadLevelState();
            return;
        }

        score = 0;
        checkpointLevel = 1;
        level = 1;
        level10Phase = 0;
        checkpointScore = 0;
        checkpointCoins = 0;
        triesRemaining = 3;
        lives = 5;
        coinsCollected = 0;
        scoreSaved = false;
        gameOver = false;
        gameWon = false;
        isTryLost = false;
        isLifeLost = false;
        levelsClearedInRow = 0;
        godMode = false;
        generateLevel();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // --- SCREEN SHAKE ---
        if (shakeTimer > 0 && !gameWon) {
            java.util.Random rand = new java.util.Random();
            int sx = rand.nextInt(shakeIntensity * 2 + 1) - shakeIntensity;
            int sy = rand.nextInt(shakeIntensity * 2 + 1) - shakeIntensity;
            g2.translate(sx, sy);
            shakeTimer--;
        } else if (gameWon) {
            shakeTimer = 0;
        }

        // --- ADAPTIVE SCALING ---
        int logicalWidth = 20 * tileSize;
        int logicalHeight = 15 * tileSize;
        double scaleX = (double)getWidth() / logicalWidth;
        double scaleY = (double)getHeight() / logicalHeight;
        double scaleFactor = Math.min(scaleX, scaleY);

        // Centering
        int offsetX = (int)(getWidth() - logicalWidth * scaleFactor) / 2;
        int offsetY = (int)(getHeight() - logicalHeight * scaleFactor) / 2;
        g2.translate(offsetX, offsetY);
        g2.scale(scaleFactor, scaleFactor);

        // --- SPLASH BACKGROUND ---
        if (!gameStarted && splashImage != null && !paused) {
            g2.drawImage(splashImage, 0, 0, screenWidth, screenHeight, null);
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }

        // --- WORLD RENDERING (Camera Offset) ---
        g2.translate(-(int)cameraX, 0);

        map.draw(g2);

        // --- DRAW ITEMS ---
        if (currentMode == GameMode.ESCAPE) {
            for (Point c : coins) {
                if (coinImg != null) g2.drawImage(coinImg, c.x * tileSize, c.y * tileSize, tileSize, tileSize, null);
                else { g2.setColor(Color.yellow); g2.fillOval(c.x * tileSize + 8, c.y * tileSize + 8, 16, 16); }
            }
            for (Crystal c : crystals) {
                c.draw(g2);
            }
            for (Point pu : powerups) {
                if (powerupImg != null) g2.drawImage(powerupImg, pu.x * tileSize, pu.y * tileSize, tileSize, tileSize, null);
                else { g2.setColor(Color.cyan); g2.fillRect(pu.x * tileSize + 8, pu.y * tileSize + 8, 16, 16); }
            }
        }

        for (ArcherBoss ab : archerBosses) {
            ab.draw(g2);
        }

        for (int i = 0; i < arrows.size(); i++) {
            arrows.get(i).draw(g2);
        }

        for (int i = 0; i < explosionTrackers.size(); i++) {
            explosionTrackers.get(i).draw(g2);
        }

        // Only draw player if not blinking during invincibility
        if (invincibilityFrames == 0 || invincibilityFrames % 10 < 5) {
            player.draw(g2);
        }

        for(Enemy e : enemies) {
            if(e != null) e.draw(g2);
        }

        if (level10Phase == 2) {
            drawFinalPortal(g2);
            drawVoidWall(g2);
        }

        // --- HUD RENDERING (Static) ---
        g2.translate((int)cameraX, 0);

        // --- HUD STUFF ---
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("Level: " + level, 20, 30);
        g2.drawString("Score: " + score, 20, 60);

        if (currentMode == GameMode.ESCAPE) {
            g2.setColor(coins.isEmpty() ? Color.green : Color.orange);
            g2.drawString("COINS LEFT: " + coins.size(), 20, 90);
        }

        // Hearts
        g2.setColor(Color.red);
        int heartX = logicalWidth - 110;
        int heartY = 10;
        for(int i = 0; i < 5; i++) {
            int x = heartX + (i * 20);
            if (i < (5 - lives)) {
                g2.drawOval(x, heartY, 8, 8);
                g2.drawOval(x + 6, heartY, 8, 8);
            } else {
                g2.fillOval(x, heartY, 8, 8);
                g2.fillOval(x + 6, heartY, 8, 8);
                int[] xPoints = {x, x + 7, x + 14};
                int[] yPoints = {heartY + 5, heartY + 15, heartY + 5};
                g2.fillPolygon(xPoints, yPoints, 3);
            }
        }

        // Tries
        if (currentMode == GameMode.ESCAPE) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("TRIES: " + triesRemaining, heartX, heartY + 30);
        }

        // --- BOSS HUD ---
        if (!archerBosses.isEmpty()) {
            ArcherBoss primaryBoss = archerBosses.get(0);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.drawString("ARCHER BOSS", logicalWidth / 2 - 60, logicalHeight - 40);

            int hw = 200;
            int hh = 20;
            int hx = logicalWidth / 2 - hw / 2;
            int hy = logicalHeight - 30;

            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(hx, hy, hw, hh); // empty health bg

            g2.setColor(new Color(138, 43, 226)); // Boss Purple
            int filled = (hw * primaryBoss.hp) / 10; // Level 10 bosses have 10 HP
            if (level != 10) filled = (hw * primaryBoss.hp) / 5;
            g2.fillRect(hx, hy, filled, hh);

            g2.setColor(Color.WHITE);
            g2.drawRect(hx, hy, hw, hh);
        }

        // --- OVERLAYS ---
        if (showTutorial) {
            drawTutorialOverlay(g2, logicalWidth, logicalHeight);
        } else if (isTryLost) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, logicalWidth, logicalHeight);

            g2.setFont(new Font("Arial", Font.BOLD, 42));
            g2.setColor(Color.ORANGE);
            String mainText = "TRY EXHAUSTED!";
            int centerX = (logicalWidth - g2.getFontMetrics().stringWidth(mainText)) / 2;
            g2.drawString(mainText, centerX, logicalHeight / 2 - 40);

            g2.setFont(new Font("Arial", Font.PLAIN, 24));
            g2.setColor(Color.WHITE);
            String subText = "TRIES REMAINING: " + triesRemaining;
            int subX = (logicalWidth - g2.getFontMetrics().stringWidth(subText)) / 2;
            g2.drawString(subText, subX, logicalHeight / 2 + 10);

            String prompt = "PRESS [R] TO RESTART FROM CHECKPOINT";
            int px = (logicalWidth - g2.getFontMetrics().stringWidth(prompt)) / 2;
            g2.drawString(prompt, px, logicalHeight / 2 + 60);
        } else if (gameWon) {
            // ===== CINEMATIC VICTORY SCREEN (UPGRADED & FORMATTED) =====
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, logicalWidth, logicalHeight);

            int midX = logicalWidth / 2;
            int midY = logicalHeight / 2;

            // --- CINEMATIC RADIAL GRADIENT ---
            float[] dist = {0.0f, 0.7f, 1.0f};
            Color[] colors = {new Color(255, 240, 180, 200), new Color(100, 70, 20, 100), Color.BLACK};
            RadialGradientPaint rgp = new RadialGradientPaint(new Point(midX, midY - 80), 500, dist, colors);
            g2.setPaint(rgp);
            g2.fillRect(0, 0, logicalWidth, logicalHeight);

            // --- FLOATING LIGHT PARTICLES ---
            java.util.Random prand = new java.util.Random(12345);
            for (int i = 0; i < 40; i++) {
                long t = System.currentTimeMillis();
                int px = prand.nextInt(logicalWidth);
                int py = (int)((prand.nextInt(logicalHeight) + (t / 20.0)) % logicalHeight);
                int size = prand.nextInt(4) + 1;
                g2.setColor(new Color(255, 255, 200, 80));
                g2.fillOval(px, py, size, size);
            }

            // --- DETAILED DUNGEON GATE (Moved Up) ---
            int gateTop = 150;
            int gateHeight = 320;
            g2.setColor(new Color(40, 40, 40));
            g2.fillRect(midX - 110, gateTop, 40, gateHeight); // Left
            g2.fillRect(midX + 70, gateTop, 40, gateHeight);  // Right
            g2.fillRect(midX - 110, gateTop - 20, 220, 30); // Arch

            // Blinding Light Through Gate
            float pulse = (float)(Math.sin(System.currentTimeMillis() / 400.0) * 0.15 + 0.85);
            g2.setColor(new Color(255, 245, 200, (int)(200 * pulse)));
            g2.fillRect(midX - 70, gateTop + 10, 140, gateHeight - 10);

            // --- PLAYER CHARACTER ---
            if (player.down != null) {
                int pSize = (int)(tileSize * 2.2);
                g2.drawImage(player.down, midX - pSize / 2, gateTop + gateHeight - pSize + 10, pSize, pSize, null);
            }

            // --- "YOU ESCAPED" TITLE ---
            g2.setFont(new Font("Serif", Font.BOLD, 72));
            String titleText = "YOU ESCAPED";
            int titleW = g2.getFontMetrics().stringWidth(titleText);
            // Stronger Shadow
            g2.setColor(new Color(0, 0, 0, 200));
            g2.drawString(titleText, midX - titleW / 2 + 4, 110 + 4);
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(titleText, midX - titleW / 2, 110);

            // --- STATS (Shifted Down) ---
            int statsStartY = 520;
            g2.setFont(new Font("Monospaced", Font.BOLD, 24));
            long finalTime = (gameEndTime - gameStartTime) / 1000;
            String timeStr = String.format("TIME: %02dm %02ds", finalTime / 60, finalTime % 60);
            String deathStr = "DEATHS: " + totalRetries;
            String scoreStr = "FINAL SCORE: " + score;

            String[] allStats = {timeStr, deathStr, scoreStr};
            for (int i = 0; i < allStats.length; i++) {
                int ty = statsStartY + (i * 35);
                int tw = g2.getFontMetrics().stringWidth(allStats[i]);
                // Text Shadow for contrast
                g2.setColor(new Color(0, 0, 0, 200));
                g2.drawString(allStats[i], midX - tw / 2 + 2, ty + 2);
                g2.setColor(Color.WHITE);
                g2.drawString(allStats[i], midX - tw / 2, ty);
            }

            // --- RESTART PROMPT ---
            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            String restartTxt = "PRESS [R] TO PLAY AGAIN";
            int rw = g2.getFontMetrics().stringWidth(restartTxt);
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(restartTxt, midX - rw / 2 + 1, 640 + 1);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString(restartTxt, midX - rw / 2, 640);

            // --- MENU BUTTON ---
            menuBtnRect.x = (logicalWidth - 220) / 2;
            menuBtnRect.y = 665;
            menuBtnRect.width = 220;
            menuBtnRect.height = 40;
            g2.setColor(new Color(30, 30, 30));
            g2.fillRoundRect(menuBtnRect.x, menuBtnRect.y, menuBtnRect.width, menuBtnRect.height, 15, 15);
            g2.setColor(new Color(255, 215, 0));
            g2.drawRoundRect(menuBtnRect.x, menuBtnRect.y, menuBtnRect.width, menuBtnRect.height, 15, 15);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            String btnT = "RETURN TO MENU";
            g2.drawString(btnT, menuBtnRect.x + (menuBtnRect.width - g2.getFontMetrics().stringWidth(btnT)) / 2, menuBtnRect.y + 26);
        }
 else if(gameOver || isLifeLost) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, logicalWidth, logicalHeight);

            g2.setFont(new Font("Arial", Font.BOLD, 48));
            String mainText = "";
            if (gameOver) {
                g2.setColor(Color.red);
                mainText = "GAME OVER";
            } else if (isLifeLost) {
                g2.setColor(Color.orange);
                mainText = "LIFE LOST";
            }

            int centerX = (logicalWidth - g2.getFontMetrics().stringWidth(mainText)) / 2;
            g2.drawString(mainText, centerX, logicalHeight / 2 - 50);

            // Subtext
            g2.setFont(new Font("Arial", Font.PLAIN, 24));
            g2.setColor(Color.white);
            String sub = isLifeLost ? "PRESS R TO RESTART" : "PRESS R TO PLAY AGAIN";
            int subX = (logicalWidth - g2.getFontMetrics().stringWidth(sub)) / 2;
            g2.drawString(sub, subX, logicalHeight / 2);

            // Manual RETURN TO MENU button
            menuBtnRect.x = (logicalWidth - 200) / 2;
            menuBtnRect.y = logicalHeight / 2 + 80;
            g2.setColor(new Color(50, 50, 50));
            g2.fill(menuBtnRect);
            g2.setColor(Color.white);
            g2.draw(menuBtnRect);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            String btnText2 = "RETURN TO MENU";
            int tx2 = menuBtnRect.x + (menuBtnRect.width - g2.getFontMetrics().stringWidth(btnText2)) / 2;
            int ty2 = menuBtnRect.y + 25;
            g2.drawString(btnText2, tx2, ty2);
        }

        if(paused && !isLifeLost && !gameOver && !gameWon && !showTutorial) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, logicalWidth, logicalHeight);
            g2.setColor(Color.white);
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            String text = "PAUSED";
            int centerX = (logicalWidth - g2.getFontMetrics().stringWidth(text)) / 2;
            g2.drawString(text, centerX, logicalHeight / 2 - 50);

            // Manual RETURN TO MENU button for Pause
            pauseMenuBtnRect.x = (logicalWidth - 200) / 2;
            pauseMenuBtnRect.y = logicalHeight / 2 + 50;
            g2.setColor(new Color(50, 50, 50));
            g2.fill(pauseMenuBtnRect);
            g2.setColor(Color.white);
            g2.draw(pauseMenuBtnRect);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            String btnText = "RETURN TO MENU";
            int tx = pauseMenuBtnRect.x + (pauseMenuBtnRect.width - g2.getFontMetrics().stringWidth(btnText)) / 2;
            int ty = pauseMenuBtnRect.y + 25;
            g2.drawString(btnText, tx, ty);
        }

        // --- COUNTDOWN OVERLAY ---
        if(!gameStarted && !gameWon && !gameOver && !isLifeLost && !paused && !showTutorial) {
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, logicalWidth, logicalHeight);

            g2.setColor(Color.white);
            g2.setFont(new Font("Arial", Font.BOLD, 100));
            String text = (countdown > 0) ? String.valueOf(countdown) : "GO!";
            int centerX = (logicalWidth - g2.getFontMetrics().stringWidth(text)) / 2;
            g2.drawString(text, centerX, logicalHeight / 2 + 35);
        }

        drawHUDMessage(g2, logicalWidth, logicalHeight);
        drawVoidHUD(g2, logicalWidth);
        g2.dispose();
    }

    private void updateLevel10Arena() {
        if (crystals.size() < previousCrystalCount) {
            map.triggerShatter();
            // Pillars gone at intervals (4 pillars, dynamic based on crystal count)
            int crystalsDestroyed = previousCrystalCount - crystals.size();

            // If 5 crystals, pillars fall at 1, 2, 3, 4 crystals destroyed
            if (crystalsDestroyed <= 4) {
                map.removePillar(crystalsDestroyed - 1);
            }
            previousCrystalCount = crystals.size();
        }

        if (archerBosses.isEmpty() && crystals.isEmpty()) {
            hudMessage = "THE SEAL HAS BROKEN! ESCAPE NOW!";
            hudMessageTimer = 60;

            // Wait for player to reach the exit tile (ID 2)
            int pCol = (player.x + tileSize / 2) / tileSize;
            int pRow = (player.y + tileSize / 2) / tileSize;
            if (map.map[pRow][pCol] == 2) {
                level10Phase = 2;
                showTutorial = true;
                tutorialPhase = 5;
                coins.clear();
                enemies = new Enemy[0];
                map.loadChaseMap();
                player.x = 100;
                player.y = (maxScreenRow / 2) * tileSize;
                voidWallX = -400;
                cameraX = 0;
                triesRemaining = 1;
                lives = 5;
                saveCheckpointState();
                hudMessage = "RUN! THE VOID IS COMING!";
                hudMessageTimer = 180;
            }
        }
    }

    private void updateLevel10Chase() {
        player.update();

        // --- VOID RUBBER BANDING ---
        double baseVoidSpeed = 4.8;
        double currentVoidSpeed = baseVoidSpeed;

        if (voidStopTimer > 0) {
            voidStopTimer--;
            currentVoidSpeed = 0;
        }

        double distFromLeft = voidWallX - cameraX;

        // Calculate player's current actual move speed
        double playerActualSpeed = player.speed;
        if (staggerTimer > 0) playerActualSpeed *= 0.4;

        if (distFromLeft < 60) {
            // Keep it visible by matching or slightly exceeding player speed
            currentVoidSpeed = Math.max(baseVoidSpeed, playerActualSpeed + 0.5);
        }

        if (invincibilityFrames > 0) {
            currentVoidSpeed *= 0.5; // Slow down during player's recovery window
        }

        // Final Portal Check
        int chaseWidth = 300;
        int portalCol = chaseWidth - 10;
        Rectangle portalRect = new Rectangle(portalCol * tileSize, 0, 10 * tileSize, maxScreenRow * tileSize);

        if (player.getBounds().intersects(portalRect)) {
            currentVoidSpeed = 1.0; // Cinematic slow-down for the final charge
            portalWinTimer++;
            if (portalWinTimer >= 180) { // 3 seconds
                gameWon = true;
                gameEndTime = System.currentTimeMillis();
                showSaveScorePrompt();
            }
        } else {
            portalWinTimer = 0;
        }

        voidWallX += currentVoidSpeed;

        // Camera follow (with abyss prevention)
        if (player.x > 300) {
            cameraX = player.x - 300;
        }

        // Cap camera at the end of the chase map (300 columns)
        int maxCamX = (300 * tileSize) - screenWidth;
        if (cameraX > maxCamX) {
            cameraX = maxCamX;
        }

        // Void Wall death
        if (player.x < voidWallX && invincibilityFrames <= 0) {
            bossHitPlayer();
            voidStopTimer = 180; // Stop for 3 seconds
            // Move player 3 tiles ahead of the wall
            player.x = (int)voidWallX + (tileSize * 3);
        }


    }

    private void drawHUDMessage(Graphics2D g2, int width, int height) {
        if (hudMessageTimer > 0) {
            g2.setColor(new Color(255, 215, 0)); // Gold
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            int msgX = (width - g2.getFontMetrics().stringWidth(hudMessage)) / 2;
            g2.drawString(hudMessage, msgX, 150);
        }
    }

    private void drawVoidWall(Graphics2D g2) {
        int wallX = (int)voidWallX;

        // Animated pulse
        int offset = (int)(Math.sin(System.currentTimeMillis() / 150.0) * 15);
        int leadingEdgeX = wallX + 120 + offset;

        // Trigger screenshake if void is close
        double distToVoid = player.x - leadingEdgeX;
        if (distToVoid < 200 && distToVoid > 0) {
            shakeTimer = 2;
            shakeIntensity = (int)(5 * (1.0 - (distToVoid / 200.0)));
        }

        // 1. GALAXY CORE (Inside of the void - TRANSLUCENT)
        g2.setColor(new Color(20, 0, 40, 180)); // 180 alpha for translucency
        g2.fillRect(leadingEdgeX - 2000, 0, 2000, screenHeight);

        // Procedural Starfield
        java.util.Random starRand = new java.util.Random(42);
        for (int i = 0; i < 200; i++) {
            int sx = starRand.nextInt(2000) + (leadingEdgeX - 2000);
            int sy = starRand.nextInt(screenHeight);
            int size = starRand.nextInt(3) + 1;
            int brightness = 100 + starRand.nextInt(155);
            g2.setColor(new Color(180, 100, 255, (int)(brightness * 0.7))); // Dimmer stars
            g2.fillOval(sx, sy, size, size);
        }

        // 2. SMOOTH INVERSE GRADIENT (TRANSLUCENT)
        for (int i = 0; i < 60; i++) {
            float ratio = i / 59.0f;
            int r = (int)(138 * (1.0f - ratio));
            int g = (int)(43 * (1.0f - ratio));
            int b = (int)(226 * (1.0f - ratio));
            int alpha = (int)(100 + (ratio * 100)); // Lower alpha (max 200)

            g2.setColor(new Color(r, g, b, alpha));
            int sliceX = leadingEdgeX - 150 + (i * 2);
            g2.fillRect(sliceX, 0, 3, screenHeight);
        }

        // 3. THE FINAL SHADOW (Slightly Translucent Edge)
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(leadingEdgeX, 0, 15, screenHeight);
    }

    private void drawVoidHUD(Graphics2D g2, int width) {
        if (level == 10 && level10Phase == 2 && !gameWon) {
            // Draw a distance bar at the top center
            int barW = 200;
            int barH = 10;
            int barX = (width - barW) / 2;
            int barY = 20;

            g2.setColor(new Color(100, 100, 100, 150));
            g2.fillRect(barX, barY, barW, barH);

            // Progress
            double maxDist = 600.0;
            double currentDist = Math.max(0, Math.min(maxDist, player.x - voidWallX));
            int fillW = (int)(barW * (currentDist / maxDist));

            g2.setColor(new Color(138, 43, 226));
            g2.fillRect(barX, barY, fillW, barH);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("VOID DISTANCE", barX + 50, barY - 5);
        }
    }

    private void drawFinalPortal(Graphics2D g2) {
        int chaseWidth = 300;
        int portalCol = chaseWidth - 10;
        int px = portalCol * tileSize;
        int py = 4 * tileSize;
        int pw = 150;
        int ph = (maxScreenRow - 8) * tileSize;

        // Intense Aura
        float pulse = (float)(Math.sin(System.currentTimeMillis() / 200.0) * 0.3 + 0.7);
        for (int i = 0; i < 5; i++) {
            g2.setColor(new Color(0, 191, 255, (int)(80 * pulse / (i + 1))));
            int auraSize = (i + 1) * 20;
            g2.fillRoundRect(px - auraSize/2, py - auraSize/2, pw + auraSize, ph + auraSize, 30, 30);
        }

        if (finalPortalImg != null) {
            g2.drawImage(finalPortalImg, px, py, pw, ph, null);
        } else {
            g2.setColor(new Color(155, 89, 182, (int)(180 * pulse)));
            g2.fillRect(px, py, pw, ph);
        }

        if (portalWinTimer > 0) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 32));
            String lockText = "CHARGING: " + (3 - (portalWinTimer/60)) + "s";
            g2.drawString(lockText, px - 50, py - 20);

            // Progress bar
            g2.setColor(Color.GREEN);
            g2.fillRect(px, py + ph + 10, (int)(pw * (portalWinTimer/180.0)), 10);
        }
    }

    private void drawTutorialOverlay(Graphics2D g2, int width, int height) {
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, width, height);

        g2.setColor(new Color(255, 215, 0)); // Gold
        g2.setFont(new Font("Arial", Font.BOLD, 40));

        String title = "";
        String objective = "";
        String controls = "";
        String hint = "";
        String prompt = "PRESS [ENTER] TO START";

        if (tutorialPhase == 1) { // Escape Mode
            title = "DUNGEON ESCAPE";
            objective = "OBJECTIVE: Collect all coins to unlock the exit!";
            controls = "CONTROLS: WASD / ARROWS to Move | 3 TRIES";
            hint = "SYSTEM: Beating Bosses creates a CHECKPOINT!";
        } else if (tutorialPhase == 2) { // Boss
            title = "THE ARCHER BOSS";
            objective = "OBJECTIVE: Destroy all " + previousCrystalCount + " Cyan Crystals!";
            controls = "CHECKPOINT: Score & Coins will be saved!";
            hint = "TIP: Lead the Boss's explosions TO the crystals.";
            prompt = "PRESS [ENTER] TO FIGHT";
        } else if (tutorialPhase == 3) { // Casual
            title = "CASUAL ADVENTURE";
            objective = "OBJECTIVE: Navigate the dungeon and reach the exit.";
            controls = "ENEMIES: Ghosts wander... until they SEE you!";
            hint = "HINT: Once spotted, they will hunt you down!";
        } else if (tutorialPhase == 4) { // Level 10
            title = "THE FINALE";
            objective = "PHASE 1: Defeat the Twins. PHASE 2: Escape.";
            controls = "WARNING: Cover will CRUMBLE as crystals break!";
            hint = "RUN: Beware the VOID that follows...";
            prompt = "PRESS [ENTER] TO END THIS";
        } else if (tutorialPhase == 5) { // Void Chase
            title = "THE VOID";
            objective = "PHASE 2: Escape the horizontal hallway!";
            controls = "SPEED: Do NOT step on the rubble!";
            hint = "RUN: The Void is faster than it looks...";
            prompt = "PRESS [ENTER] TO RUN";
        }

        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (width - fm.stringWidth(title)) / 2, height / 2 - 120);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        fm = g2.getFontMetrics();

        g2.drawString(objective, (width - fm.stringWidth(objective)) / 2, height / 2 - 40);
        g2.drawString(controls, (width - fm.stringWidth(controls)) / 2, height / 2 + 20);

        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString(hint, (width - fm.stringWidth(hint)) / 2, height / 2 + 80);

        g2.setColor(new Color(255, 215, 0));
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        fm = g2.getFontMetrics();
        g2.drawString(prompt, (width - fm.stringWidth(prompt)) / 2, height / 2 + 160);
    }
}