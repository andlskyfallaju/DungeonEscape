import java.awt.*;

public class ArcherBoss {
    GamePanel gp;
    public double x, y;
    int cooldown = 0;
    int speed = 2; // Slower than normal enemies, relies on projectiles

    public int hp = 5;
    public int frenzyTimer = 0;
    double frenzyAngle = 0; // For spreading arrow shots
    int repelTimer = 0;

    // Dynamic Effects
    private java.util.List<Point> trail = new java.util.ArrayList<>();
    private java.util.List<Point> particles = new java.util.ArrayList<>();

    // Taunts
    private String currentTaunt = "";
    private int tauntTimer = 0;
    private int damageFlash = 0;

    private String[] taunts = {
            "THE VOID CONSUMES ALL...",
            "RUN WHILE YOU STILL CAN!",
            "YOUR SOUL IS MINE!",
            "THE DARKNESS IS COMING...",
            "YOU CANNOT ESCAPE THE INEVITABLE!"
    };

    // Pathfinding
    int actionLockCounter = 0;
    java.util.List<Node> path = new java.util.ArrayList<>();
    int trackerCooldown = 400; // Delay for first spawned tracker

    // Attack Variables
    int burstPhase = 0; // 0=None, 1=Cocking, 2=Firing
    int burstTimer = 0;
    int burstShotsRemaining = 0;
    int burstDelay = 0;
    int burstCooldown = 0;
    boolean willDropExplosiveThisBurst = false;

    public ArcherBoss(GamePanel gp, int x, int y) {
        this.gp = gp;
        this.x = x;
        this.y = y;
    }

    public void hit(int damage) {
        hp -= damage;
        if (hp <= 0) {
            gp.bossDefeated();
        } else {
            frenzyTimer = 300; // 5 Seconds of bullet hell
            cooldown = 0;
        }
    }

    public void hit() {
        hit(1);
    }

    public void update(int playerX, int playerY) {
        if (!gp.gameStarted || gp.paused || gp.gameOver || gp.gameWon)
            return;

        // Taunt logic
        if (tauntTimer > 0) tauntTimer--;
        else if (gp.frameCount % 300 == 0) { // Every 5 seconds
            java.util.Random rand = new java.util.Random();
            currentTaunt = taunts[rand.nextInt(taunts.length)];
            tauntTimer = 120; // Show for 2 seconds
        }

        if (damageFlash > 0) damageFlash--;

        // Update Effects
        if (frenzyTimer > 0) {
            trail.add(0, new Point((int)x, (int)y));
            if (trail.size() > 5) trail.remove(trail.size() - 1);
        } else {
            trail.clear();
        }

        if (burstPhase == 1 || frenzyTimer > 0) { // Charging or Frenzy
            if (gp.frameCount % 2 == 0) {
                java.util.Random rand = new java.util.Random();
                int px = (int)x + gp.tileSize/2 + (rand.nextInt(100) - 50);
                int py = (int)y + gp.tileSize/2 + (rand.nextInt(100) - 50);
                particles.add(new Point(px, py));
            }
        }

        for (int i = 0; i < particles.size(); i++) {
            Point p = particles.get(i);
            double centerX = x + gp.tileSize/2;
            double centerY = y + gp.tileSize/2;
            if (p.x < centerX) p.x += 2; else p.x -= 2;
            if (p.y < centerY) p.y += 2; else p.y -= 2;

            if (Math.abs(p.x - centerX) < 5 && Math.abs(p.y - centerY) < 5) {
                particles.remove(i);
                i--;
            }
        }

        if (frenzyTimer > 0) {
            // FRENZY STATE: Bullet Hell Sprinkler
            frenzyTimer--;
            cooldown--;

            if (cooldown <= 0) {
                double speed = 8.5; // Slightly faster for hellish feel
                for (int i = 0; i < 4; i++) {
                    double angle = frenzyAngle + (i * 90);
                    double rad = Math.toRadians(angle);

                    double vx = Math.cos(rad) * speed;
                    double vy = Math.sin(rad) * speed;

                    // Spawn safely on the exact edge of the boss hitbox
                    double spawnX = x + gp.tileSize / 2.0;
                    double spawnY = y + gp.tileSize / 2.0;
                    spawnX += Math.cos(rad) * (gp.tileSize / 2.0);
                    spawnY += Math.sin(rad) * (gp.tileSize / 2.0);

                    gp.arrows.add(new Arrow(gp, spawnX, spawnY, vx, vy));
                }

                frenzyAngle = (frenzyAngle + 10) % 360; // Smooth 10-degree increments
                cooldown = 6; // 0.1s delay for a faster sprinkler feel
            }
            return; // Boss holds perfectly still while in frenzy
        }

        // NORMAL STATE: A-Star Chase & Hold
        actionLockCounter++;
        if (actionLockCounter >= 30) {
            actionLockCounter = 0;
            int goalCol = playerX / gp.tileSize;
            int goalRow = playerY / gp.tileSize;
            int startCol = (int) x / gp.tileSize;
            int startRow = (int) y / gp.tileSize;

            path = findPath(startCol, startRow, goalCol, goalRow);
        }

        double dist = Math.abs(playerX - x) + Math.abs(playerY - y);

        // If player is farther than 4 AStar Nodes, chase them using A*!
        int pathLen = (path == null) ? 0 : path.size();

        if (pathLen > 4) {
            Node nextNode = path.get(1);
            // Target the center of the next tile for absolute smoothness
            double targetX = nextNode.col * gp.tileSize;
            double targetY = nextNode.row * gp.tileSize;

            double dx = targetX - x;
            double dy = targetY - y;
            double angle = Math.atan2(dy, dx);

            double moveX = Math.cos(angle) * speed;
            double moveY = Math.sin(angle) * speed;

            // Proximity deadzone to prevent jittering
            if (Math.abs(dx) < speed) moveX = dx;
            if (Math.abs(dy) < speed) moveY = dy;

            // Collision Check
            int nextCol = (int) (x + moveX + gp.tileSize / 2.0) / gp.tileSize;
            int nextRow = (int) (y + moveY + gp.tileSize / 2.0) / gp.tileSize;
            if (nextRow >= 0 && nextRow < gp.maxScreenRow && nextCol >= 0 && nextCol < gp.maxScreenCol) {
                if (gp.map.map[nextRow][nextCol] != 1) {
                    x += moveX;
                    y += moveY;
                }
            }
        }
        // If player gets physically too close, retreat backwards fluidly
        else if (dist < gp.tileSize * 3) {
            double dx = x - playerX;
            double dy = y - playerY;
            double angle = Math.atan2(dy, dx);

            double moveX = Math.cos(angle) * speed;
            double moveY = Math.sin(angle) * speed;

            int nextCol = (int) (x + moveX + gp.tileSize / 2.0) / gp.tileSize;
            int nextRow = (int) (y + moveY + gp.tileSize / 2.0) / gp.tileSize;
            if (nextRow >= 0 && nextRow < gp.maxScreenRow && nextCol >= 0 && nextCol < gp.maxScreenCol) {
                if (gp.map.map[nextRow][nextCol] != 1) {
                    x += moveX;
                    y += moveY;
                }
            }
        }

        x = Math.max(gp.tileSize, Math.min(x, gp.screenWidth - gp.tileSize * 2));
        y = Math.max(gp.tileSize, Math.min(y, gp.screenHeight - gp.tileSize * 2));

        applySeparation();

        if (trackerCooldown > 0)
            trackerCooldown--;
        if (burstCooldown > 0)
            burstCooldown--;

        // --- MACHINE GUN BURST STATE MACHINE ---
        if (burstPhase == 1) {
            // Phase 1: Cocking / Wind-up (Hold perfect stillness)
            burstTimer--;
            if (burstTimer <= 0) {
                burstPhase = 2;
                burstTimer = 180; // Blast sequence lasts exactly 3 seconds
                burstShotsRemaining = (gp.level == 10) ? 3 : 4;
                burstDelay = 0;

                // One-time check per burst: will this burst contain ONE trouble detonator?
                willDropExplosiveThisBurst = (Math.random() < 0.15);
            }
            return;
        } else if (burstPhase == 2) {
            // Phase 2: Active Machine-Gun Sustained Fire
            burstTimer--;
            burstDelay--;

            if (burstDelay <= 0) {
                if (burstShotsRemaining > 0) {
                    double angle = Math.atan2((playerY + gp.tileSize / 2.0) - (y + gp.tileSize / 2.0),
                            (playerX + gp.tileSize / 2.0) - (x + gp.tileSize / 2.0));
                    double speed = 10.5;
                    double vx = Math.cos(angle) * speed;
                    double vy = Math.sin(angle) * speed;

                    double spawnX = x + gp.tileSize / 2.0 + Math.cos(angle) * (gp.tileSize / 2.0);
                    double spawnY = y + gp.tileSize / 2.0 + Math.sin(angle) * (gp.tileSize / 2.0);

                    // Controlled trouble: decision made during Cocking phase applies here once
                    if (willDropExplosiveThisBurst && gp.explosionTrackers.size() == 0) {
                        gp.explosionTrackers.add(new ExplosionTracker(gp, (int) spawnX, (int) spawnY));
                        willDropExplosiveThisBurst = false; // Only once per 3-second sequence
                    }

                    gp.arrows.add(new Arrow(gp, spawnX, spawnY, vx, vy));

                    burstShotsRemaining--;
                    burstDelay = (gp.level == 10) ? 5 : 3;
                } else {
                    // Volley completed: Engage 8-frame cooling pause before next volley initiates
                    burstShotsRemaining = (gp.level == 10) ? 3 : 4;
                    burstDelay = (gp.level == 10) ? 12 : 8;
                }
            }

            if (burstTimer <= 0) {
                burstPhase = 0;
                burstCooldown = 480; // Hard 8-second cooldown before another burst can trigger
                cooldown = 120;
            }
            return;
        }

        if (cooldown > 0)
            cooldown--;

        if (cooldown <= 0) {
            // General explosive tracker probability reduced to 15%
            if (trackerCooldown <= 0 && Math.random() < 0.15 && gp.explosionTrackers != null
                    && gp.explosionTrackers.size() == 0) {
                gp.explosionTrackers.add(new ExplosionTracker(gp, (int) x, (int) y));

                // Add a hard RNG delay barrier: 5 seconds to 10 seconds (300 - 600 frames)
                // minimum before the NEXT tracker
                trackerCooldown = 300 + (int) (Math.random() * 300);
                cooldown = 150;
            } else {
                // 8% chance to enter RAPID FIRE burst state (with 8s cool down protection)
                if (burstCooldown <= 0 && Math.random() < 0.10) {
                    burstPhase = 1;
                    // RNG Wind-up duration between 2 and 4 seconds (120 - 240 frames)
                    burstTimer = 120 + (int) (Math.random() * 120);
                } else {
                    // --- SHOTGUN SPREAD (Normal Phase) ---
                    double centerAngle = Math.atan2((playerY + gp.tileSize / 2.0) - (y + gp.tileSize / 2.0),
                            (playerX + gp.tileSize / 2.0) - (x + gp.tileSize / 2.0));
                    double speed = 8.5; // Increased baseline speed

                    // Fire 3 arrows (-15, 0, +15 degrees)
                    double[] angles = { centerAngle - Math.toRadians(15), centerAngle,
                            centerAngle + Math.toRadians(15) };
                    for (double angle : angles) {
                        double vx = Math.cos(angle) * speed;
                        double vy = Math.sin(angle) * speed;

                        double spawnX = x + gp.tileSize / 2.0 + Math.cos(angle) * (gp.tileSize / 2.0);
                        double spawnY = y + gp.tileSize / 2.0 + Math.sin(angle) * (gp.tileSize / 2.0);

                        gp.arrows.add(new Arrow(gp, spawnX, spawnY, vx, vy));
                    }
                    cooldown = (gp.level == 10) ? 100 : 36; // Lvl 10: 1.6s, Lvl 5: 0.6s delay
                }
            }
        }
    }

    public java.util.List<Node> findPath(int startCol, int startRow, int goalCol, int goalRow) {
        java.util.List<Node> openList = new java.util.ArrayList<>();
        java.util.List<Node> closedList = new java.util.ArrayList<>();

        Node startNode = new Node(startCol, startRow);
        Node goalNode = new Node(goalCol, goalRow);

        openList.add(startNode);

        while (!openList.isEmpty()) {
            Node current = openList.get(0);
            for (Node node : openList) {
                if (node.fCost < current.fCost) {
                    current = node;
                }
            }

            openList.remove(current);
            closedList.add(current);

            if (current.col == goalNode.col && current.row == goalNode.row) {
                return constructPath(current);
            }

            int[][] directions = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };

            for (int[] dir : directions) {
                int newCol = current.col + dir[0];
                int newRow = current.row + dir[1];

                if (newCol < 0 || newRow < 0 || newCol >= gp.maxScreenCol || newRow >= gp.maxScreenRow)
                    continue;

                int tile = gp.map.map[newRow][newCol];

                // Block walls and exit
                if (tile != 0)
                    continue;

                Node neighbor = new Node(newCol, newRow);
                if (containsNode(closedList, neighbor))
                    continue;

                int gCost = current.gCost + 1;
                int hCost = Math.abs(newCol - goalCol) + Math.abs(newRow - goalRow);

                boolean inOpen = containsNode(openList, neighbor);

                if (!inOpen || gCost < neighbor.gCost) {
                    neighbor.gCost = gCost;
                    neighbor.hCost = hCost;
                    neighbor.calculateFCost();
                    neighbor.parent = current;

                    if (!inOpen)
                        openList.add(neighbor);
                }
            }
        }
        return new java.util.ArrayList<>();
    }

    private boolean containsNode(java.util.List<Node> list, Node node) {
        for (Node n : list) {
            if (n.col == node.col && n.row == node.row)
                return true;
        }
        return false;
    }

    private java.util.List<Node> constructPath(Node node) {
        java.util.List<Node> compiledPath = new java.util.ArrayList<>();
        while (node != null) {
            compiledPath.add(0, node);
            node = node.parent;
        }
        return compiledPath;
    }

    public void triggerFlash() {
        damageFlash = 10;
    }

    public void draw(Graphics2D g2) {
        // 1. Draw Trailing Ghosts (Frenzy)
        if (frenzyTimer > 0 && gp.bossImg != null) {
            for (int i = 0; i < trail.size(); i++) {
                Point p = trail.get(i);
                float alpha = 0.5f - (i * 0.1f);
                if (alpha < 0) alpha = 0;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.drawImage(gp.bossImg, p.x, p.y, gp.tileSize, gp.tileSize, null);
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        // 2. Draw Charging Particles
        if (burstPhase == 1 || frenzyTimer > 0) {
            if (frenzyTimer > 0) {
                g2.setColor(new Color(255, 0, 0, 180)); // Red particles for Frenzy
            } else {
                g2.setColor(new Color(255, 255, 0, 150)); // Yellow particles for Charging
            }
            for (Point p : particles) {
                g2.fillOval(p.x, p.y, 4, 4);
            }
        }

        int drawX = (int)x;
        int drawY = (int)y;

        // 3. Sprite Vibration (Firing)
        if (burstPhase == 2) {
            java.util.Random rand = new java.util.Random();
            drawX += rand.nextInt(7) - 3;
            drawY += rand.nextInt(7) - 3;
        }

        // BOSS SCALE: Maintain Aspect Ratio and Scale Up (2.5x height)
        int visualHeight = (int)(gp.tileSize * 2.5);
        int visualWidth = visualHeight; // Fallback
        if (gp.bossImg != null) {
            double ratio = (double)gp.bossImg.getWidth() / gp.bossImg.getHeight();
            visualWidth = (int)(visualHeight * ratio);
        }

        int offX = (visualWidth - gp.tileSize) / 2;
        int offY = (visualHeight - gp.tileSize); // Sit on the bottom of the tile

        if (gp.bossImg != null) {
            g2.drawImage(gp.bossImg, drawX - offX, drawY - offY, visualWidth, visualHeight, null);

            // Add a semi-transparent colored tint for states
            if (frenzyTimer > 0) {
                g2.setColor(new Color(255, 0, 0, 80)); // Red tint for Frenzy
                g2.fillRect(drawX - offX, drawY - offY, visualWidth, visualHeight);
            } else if (burstPhase == 1) {
                g2.setColor(new Color(255, 255, 0, 40)); // Subtle Yellow tint for Cocking
                g2.fillRect(drawX - offX, drawY - offY, visualWidth, visualHeight);
            }
        } else {
            // Fallback (Geometric)
            if (frenzyTimer > 0) {
                g2.setColor(Color.RED);
            } else if (burstPhase == 1) {
                g2.setColor(Color.YELLOW);
            } else {
                g2.setColor(new Color(138, 43, 226));
            }
            g2.fillRect(drawX, drawY, gp.tileSize, gp.tileSize);
        }

        // 4. Damage Flash
        if (damageFlash > 0) {
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillRect(drawX - offX, drawY - offY, visualWidth, visualHeight);
        }

        // 5. Speech Bubble (Taunt)
        if (tauntTimer > 0) {
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            int tw = g2.getFontMetrics().stringWidth(currentTaunt);
            int tx = drawX + gp.tileSize / 2 - tw / 2;
            int ty = drawY - 40;

            // Bubble box
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRoundRect(tx - 5, ty - 20, tw + 10, 25, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(tx - 5, ty - 20, tw + 10, 25, 10, 10);
            g2.drawString(currentTaunt, tx, ty);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, gp.tileSize, gp.tileSize);
    }

    private void applySeparation() {
        if (gp.level != 10) return; // Only for twins

        // Trigger check once every 3 seconds (180 frames)
        if (gp.frameCount % 180 == 0) {
            for (ArcherBoss other : gp.archerBosses) {
                if (other != null && other != this) {
                    double dist = Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
                    if (dist < gp.tileSize * 2) {
                        repelTimer = 120; // Move away for 2 seconds
                    }
                }
            }
        }

        if (repelTimer > 0) {
            repelTimer--;
            for (ArcherBoss other : gp.archerBosses) {
                if (other != null && other != this) {
                    double dist = Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
                    // Move away until 4 blocks (tileSize * 4)
                    if (dist < gp.tileSize * 4) {
                        if (x < other.x) x -= 2;
                        else x += 2;

                        if (y < other.y) y -= 2;
                        else y += 2;
                    }
                }
            }
        }
    }
}