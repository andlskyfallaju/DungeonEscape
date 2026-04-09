import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class ArcherBoss {
    GamePanel gp;
    public double x, y;
    int cooldown = 0;
    int speed = 2; // Slower than normal enemies, relies on projectiles

    public int hp = 5;
    public int frenzyTimer = 0;
    int frenzyAngle = 0; // For spreading arrow shots

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

    public void hit() {
        hp--;
        if (hp <= 0) {
            gp.bossDefeated();
        } else {
            frenzyTimer = 300; // 5 Seconds of bullet hell
            cooldown = 0;
        }
    }

    public void update(int playerX, int playerY) {
        if (!gp.gameStarted || gp.paused || gp.gameOver || gp.gameWon)
            return;

        if (frenzyTimer > 0) {
            // FRENZY STATE: Bullet Hell Sprinkler
            frenzyTimer--;
            cooldown--;

            if (cooldown <= 0) {
                double speed = 7.0;
                for (int i = 0; i < 4; i++) {
                    double angle = frenzyAngle + (i * 90);
                    double rad = Math.toRadians(angle);

                    double vx = Math.cos(rad) * speed;
                    double vy = Math.sin(angle) * speed;

                    // Spawn safely on the exact edge of the boss hitbox
                    double spawnX = x + gp.tileSize / 2.0;
                    double spawnY = y + gp.tileSize / 2.0;
                    spawnX += Math.cos(rad) * (gp.tileSize / 2.0);
                    spawnY += Math.sin(rad) * (gp.tileSize / 2.0);

                    gp.arrows.add(new Arrow(gp, spawnX, spawnY, vx, vy));
                }

                frenzyAngle = (frenzyAngle + 45) % 360;
                cooldown = 12; // 0.2s delay
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
                burstShotsRemaining = 4;
                burstDelay = 0;

                // One-time check per burst: will this burst contain ONE trouble detonator?
                willDropExplosiveThisBurst = (Math.random() < 0.45);
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
                    burstDelay = 3;
                } else {
                    // Volley completed: Engage 8-frame cooling pause before next volley initiates
                    burstShotsRemaining = 4;
                    burstDelay = 8;
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
                    cooldown = 36; // Faster 0.6s delay
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

    public void draw(Graphics2D g2) {
        if (frenzyTimer > 0) {
            g2.setColor(Color.RED); // Frenzy color
        } else if (burstPhase == 1) {
            // Winding-up Cocktail Visuals (Strobe effect between Orange and Yellow)
            if (burstTimer % 10 < 5)
                g2.setColor(new Color(255, 140, 0)); // Deep Orange
            else
                g2.setColor(Color.YELLOW);
        } else if (burstPhase == 2) {
            g2.setColor(Color.RED); // Machine Gun sustained fire color
        } else if (cooldown <= 20) {
            g2.setColor(new Color(255, 100, 200)); // Wind up color (Pink)
        } else {
            g2.setColor(new Color(138, 43, 226)); // Normal Purple
        }

        g2.fillRect((int) x, (int) y, gp.tileSize, gp.tileSize);
        g2.setColor(Color.WHITE);
        g2.drawRect((int) x, (int) y, gp.tileSize, gp.tileSize);

        // Evil eyes
        g2.setColor(Color.WHITE);
        g2.fillOval((int) x + 10, (int) y + 10, 8, 8);
        g2.fillOval((int) x + 30, (int) y + 10, 8, 8);
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, gp.tileSize, gp.tileSize);
    }
}
