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
    public ArcherBoss archerBoss = null;
    public boolean isBossLevel = false;
    public int invincibilityFrames = 0;

    // Snapshot for "Same Level" restart logic
    int[][] initialMap;
    int initialPlayerX, initialPlayerY;
    Point[] initialEnemyPositions;
    private int coinsCollected = 0;
    private boolean scoreSaved = false;

    final int tileSize = 48;
    int maxScreenCol = 20;
    int maxScreenRow = 15;
    boolean gameOver = false;
    boolean gameWon = false;
    Enemy[] enemies = new Enemy[3];
    boolean gameStarted = false;
    long startTime;
    int countdown = 3;
    public boolean paused = false;
    public BufferedImage splashImage, coinImg, powerupImg, lockedDoorImg;
    int screenWidth = tileSize * maxScreenCol;
    int screenHeight = tileSize * maxScreenRow;

    Thread gameThread;
    private boolean isLifeLost = false;
    private Rectangle menuBtnRect = new Rectangle(0, 0, 200, 40);

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
                    int logicalWidth = maxScreenCol * tileSize;
                    int logicalHeight = maxScreenRow * tileSize;
                    double scaleX = (double)getWidth() / logicalWidth;
                    double scaleY = (double)getHeight() / logicalHeight;
                    double scaleFactor = Math.min(scaleX, scaleY);
                    
                    int offsetX = (int)(getWidth() - logicalWidth * scaleFactor) / 2;
                    int offsetY = (int)(getHeight() - logicalHeight * scaleFactor) / 2;

                    int mouseX = (int)((e.getX() - offsetX) / scaleFactor);
                    int mouseY = (int)((e.getY() - offsetY) / scaleFactor);
                    
                    if (menuBtnRect.contains(mouseX, mouseY)) {
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

        // Reset state for items
        coins.clear();
        powerups.clear();
        arrows.clear();
        explosionTrackers.clear();
        crystals.clear();
        archerBoss = null;
        isBossLevel = false;
        invincibilityFrames = 0;
        slowTimer = 0;
        exitLocked = (currentMode == GameMode.ESCAPE);
        isLifeLost = false;
        
        if (currentMode == GameMode.ESCAPE && level % 5 == 0) {
            isBossLevel = true;
        }

        if (isBossLevel) {
            map.loadArenaMap();
        } else {
            map.loadMap(level);
        }
        
        Point p = map.getRandomFreeTile();
        player.x = p.x * tileSize;
        player.y = p.y * tileSize;

        Point e;
        if (isBossLevel) {
            e = map.placeExitFarFromPlayer(p.x, p.y);
            
            archerBoss = new ArcherBoss(this, (maxScreenCol/2) * tileSize, (maxScreenRow/2) * tileSize);
            enemies = new Enemy[0]; // No standard ghosts
            
            int coinCount = 5; 
            for (int i = 0; i < coinCount; i++) {
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

            int enemyCount = Math.min(12, 2 + level + (maxScreenCol / 10));
            enemies = new Enemy[enemyCount];
            for(int i = 0; i < enemies.length; i++) {
                Point ep = map.getRandomFreeTileFarFrom(p, 5);
                enemies[i] = new Enemy(this, ep.x * tileSize, ep.y * tileSize);
            }
        }

        saveLevelState(); // Snapshot for potential deaths

        // Reset countdown logic
        startTime = System.currentTimeMillis();
        gameStarted = false;
        countdown = 3;
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
        if (archerBoss != null) {
            archerBoss.x = (maxScreenCol/2) * tileSize;
            archerBoss.y = (maxScreenRow/2) * tileSize;
            archerBoss.cooldown = 0;
        }
        
        startTime = System.currentTimeMillis();
        gameStarted = false;
        countdown = 3;
        
        // Items persist on death or we could re-spawn them. 
        // For simplicity, let's just unlock the exit if no coins left.
        if (currentMode == GameMode.ESCAPE && coins.isEmpty()) {
            exitLocked = false;
        }
    }

    public boolean checkPlayerHit() {
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
        if (archerBoss != null && playerRect.intersects(archerBoss.getBounds())) {
            return true;
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
        if (invincibilityFrames <= 0) {
            lives--;
            invincibilityFrames = 60; // 1 second invulnerability
            if (lives <= 0) terminateGame();
        }
    }

    public void bossDefeated() {
        archerBoss = null;
        arrows.clear();
        explosionTrackers.clear();
        exitLocked = false; // Door unlocks!
        score += 500;
    }
    
    private void terminateGame() {
        gameOver = true;
        if (!scoreSaved) {
            scoreSaved = true;
            String finalScore = String.valueOf(score);
            String finalLevel = String.valueOf(level);
            String finalMode = currentMode.toString().toUpperCase();
            String finalCoins = String.valueOf(coinsCollected);
            
            // Show prompt on UI thread
            SwingUtilities.invokeLater(() -> {
                String name = JOptionPane.showInputDialog(this, "GAME OVER! Final Score: " + finalScore + "\nEnter your name:", "Save Score", JOptionPane.PLAIN_MESSAGE);
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
        // Toggle Pause
        if (keyH.escPressed) {
            paused = !paused;
            keyH.escPressed = false;
        }

        if (paused) return;

        // Handle Invincibility
        if (invincibilityFrames > 0) {
            invincibilityFrames--;
        }

        if (isLifeLost) {
            if (keyH.restartPressed) {
                keyH.restartPressed = false;
                loadLevelState();
                isLifeLost = false;
            }
            return;
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

        if(!gameStarted) {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            countdown = 3 - (int)elapsed;
            if(countdown <= 0) {
                gameStarted = true;
            }
            return;
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

        if (archerBoss != null) archerBoss.update(player.x, player.y);
        
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

        if(checkPlayerHit()) {
            if (isBossLevel) {
                bossHitPlayer(); // special instant no-reset damage
            } else {
                lives--;
                if(lives <= 0) {
                    terminateGame();
                } else {
                    isLifeLost = true;
                    keyH.restartPressed = false; // reset flag
                }
            }
        }

        if(playerReachedExit()) {
            level++;
            score += 100;
            generateLevel();
        }
    }

    private void restartGame() {
        score = 0;
        level = 1;
        lives = 5;
        coinsCollected = 0;
        scoreSaved = false;
        gameOver = false;
        gameWon = false;
        generateLevel();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // --- ADAPTIVE SCALING ---
        int logicalWidth = maxScreenCol * tileSize;
        int logicalHeight = maxScreenRow * tileSize;
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

        if (archerBoss != null) archerBoss.draw(g2);
        
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

        // --- BOSS HUD ---
        if (archerBoss != null) {
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
            int filled = (hw * archerBoss.hp) / 5;
            g2.fillRect(hx, hy, filled, hh);
            
            g2.setColor(Color.WHITE);
            g2.drawRect(hx, hy, hw, hh);
        }

        // --- OVERLAYS ---
        if(gameOver || gameWon || isLifeLost) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, logicalWidth, logicalHeight);
            
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            String mainText = "";
            if (gameWon) { 
                g2.setColor(Color.green); 
                mainText = "DUNGEON ESCAPED!"; 
            } else if (gameOver) { 
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
            String btnText = "RETURN TO MENU";
            int tx = menuBtnRect.x + (menuBtnRect.width - g2.getFontMetrics().stringWidth(btnText)) / 2;
            int ty = menuBtnRect.y + 25;
            g2.drawString(btnText, tx, ty);
        }

        if(paused && !isLifeLost && !gameOver && !gameWon) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, logicalWidth, logicalHeight);
            g2.setColor(Color.white);
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            String text = "PAUSED";
            int centerX = (logicalWidth - g2.getFontMetrics().stringWidth(text)) / 2;
            g2.drawString(text, centerX, logicalHeight / 2);
        }

        // --- COUNTDOWN OVERLAY ---
        if(!gameStarted && !gameWon && !gameOver && !isLifeLost && !paused) {
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, logicalWidth, logicalHeight);
            
            g2.setColor(Color.white);
            g2.setFont(new Font("Arial", Font.BOLD, 100));
            String text = (countdown > 0) ? String.valueOf(countdown) : "GO!";
            int centerX = (logicalWidth - g2.getFontMetrics().stringWidth(text)) / 2;
            g2.drawString(text, centerX, logicalHeight / 2 + 35);
        }

        g2.dispose();
    }
}
