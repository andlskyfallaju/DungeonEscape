import java.awt.*;
import java.util.Random;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Enemy {

    int x, y;
    int speed;

    String direction;
    GamePanel gp;

    int actionLockCounter = 0;
    java.util.List<Node> path = new java.util.ArrayList<>();
    public BufferedImage agroUp, agroDown, agroLeft, agroRight;
    public BufferedImage wanderUp, wanderDown, wanderLeft, wanderRight;

    public java.util.List<Node> findPath(int startCol, int startRow, int goalCol, int goalRow) {

        java.util.List<Node> openList = new java.util.ArrayList<>();
        java.util.List<Node> closedList = new java.util.ArrayList<>();

        Node startNode = new Node(startCol, startRow);
        Node goalNode = new Node(goalCol, goalRow);

        openList.add(startNode);

        while(!openList.isEmpty()) {

            Node current = openList.get(0);

            for(Node node : openList) {
                if(node.fCost < current.fCost) {
                    current = node;
                }
            }

            openList.remove(current);
            closedList.add(current);

            if(current.col == goalNode.col && current.row == goalNode.row) {
                return constructPath(current);
            }

            int[][] directions = {{0,-1},{0,1},{-1,0},{1,0}};

            for(int[] dir : directions) {

                int newCol = current.col + dir[0];
                int newRow = current.row + dir[1];

                if(newCol < 0 || newRow < 0 ||
                        newCol >= gp.maxScreenCol || newRow >= gp.maxScreenRow) continue;

                int tile = gp.map.map[newRow][newCol];

                // 🚫 Block walls AND exit
                if(tile != 0) continue;

                Node neighbor = new Node(newCol, newRow);

                if(containsNode(closedList, neighbor)) continue;

                int gCost = current.gCost + 1;
                int hCost = Math.abs(newCol - goalCol) + Math.abs(newRow - goalRow);

                boolean inOpen = containsNode(openList, neighbor);

                if(!inOpen || gCost < neighbor.gCost) {
                    neighbor.gCost = gCost;
                    neighbor.hCost = hCost;
                    neighbor.calculateFCost();
                    neighbor.parent = current;

                    if(!inOpen) openList.add(neighbor);
                }
            }
        }

        return new java.util.ArrayList<>();
    }

    private boolean containsNode(java.util.List<Node> list, Node node) {
        for(Node n : list) {
            if(n.col == node.col && n.row == node.row) return true;
        }
        return false;
    }

    private java.util.List<Node> constructPath(Node node) {
        java.util.List<Node> path = new java.util.ArrayList<>();

        while(node != null) {
            path.add(0, node);
            node = node.parent;
        }

        return path;
    }

    public enum State { CHASE, WANDER }
    public State currentState = State.WANDER;
    private Node wanderTarget = null;
    private int wanderLockCounter = 0;

    public Enemy(GamePanel gp, int x, int y) {

        this.gp = gp;
        this.x = x;
        this.y = y;

        Random rand = new Random();
        speed = (int)((1 + rand.nextInt(2) + (gp.level / 4)) * 1.5); // Scaled for 48px

        direction = "left";

        getEnemyImage();
    }

    public void getEnemyImage() {
        try {
            agroUp = ImageIO.read(getClass().getResourceAsStream("/res/enemy_agro_up.png"));
            agroDown = ImageIO.read(getClass().getResourceAsStream("/res/enemy_agro_down.png"));
            agroLeft = ImageIO.read(getClass().getResourceAsStream("/res/enemy_agro_left.png"));
            agroRight = ImageIO.read(getClass().getResourceAsStream("/res/enemy_agro_right.png"));

            wanderUp = ImageIO.read(getClass().getResourceAsStream("/res/enemy_wander_up.png"));
            wanderDown = ImageIO.read(getClass().getResourceAsStream("/res/enemy_wander_down.png"));
            wanderLeft = ImageIO.read(getClass().getResourceAsStream("/res/enemy_wander_left.png"));
            wanderRight = ImageIO.read(getClass().getResourceAsStream("/res/enemy_wander_right.png"));
        } catch (Exception e) {
            System.out.println("Error loading enemy sprites: " + e.getMessage());
        }
    }

    public final int width = 30;
    public final int height = 30;

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    private int skipCounter = 0;

    public void update(int playerX, int playerY) {

        // --- SLOW MOTION EFFECT ---
        if (gp.slowTimer > 0) {
            skipCounter++;
            if (skipCounter % 3 != 0) return; // Only update every 3rd frame (1/3 speed)
        }

        actionLockCounter++;

        // Only recalculate path every 15 frames for performance and consistency
        if(actionLockCounter >= 15 || path.isEmpty()) {
            updateAI();
            actionLockCounter = 0;
        }

        moveTowardsPath();
        applySeparation();
    }

    private void updateAI() {
        int startCol = x / gp.tileSize;
        int startRow = y / gp.tileSize;

        // --- MODE-SPECIFIC STATE SELECTION ---
        if (gp.currentMode == GamePanel.GameMode.ESCAPE) {
            currentState = State.WANDER;
        } else {
            // Casual Mode: Only CHASE if within 10 tiles AND one of the 4 closest
            double distToPlayer = Math.sqrt(Math.pow(x - gp.player.x, 2) + Math.pow(y - gp.player.y, 2));
            if (distToPlayer < gp.tileSize * 10) {
                int closerEnemies = 0;
                for (Enemy e : gp.enemies) {
                    if (e != null && e != this) {
                        double otherDist = Math.sqrt(Math.pow(e.x - gp.player.x, 2) + Math.pow(e.y - gp.player.y, 2));
                        if (otherDist < distToPlayer) closerEnemies++;
                    }
                }
                currentState = (closerEnemies < 4) ? State.CHASE : State.WANDER;
            } else {
                currentState = State.WANDER;
            }
        }

        if (currentState == State.CHASE) {
            int goalCol = gp.player.x / gp.tileSize;
            int goalRow = gp.player.y / gp.tileSize;
            path = findPath(startCol, startRow, goalCol, goalRow);
        } else {
            // WANDER logic: Pick a random spot every few seconds
            wanderLockCounter++;
            if (wanderLockCounter >= 10 || wanderTarget == null || (startCol == wanderTarget.col && startRow == wanderTarget.row)) {
                Point p = gp.map.getRandomFreeTile();
                wanderTarget = new Node(p.x, p.y);
                wanderLockCounter = 0;
            }
            path = findPath(startCol, startRow, wanderTarget.col, wanderTarget.row);
        }
    }

    private void moveTowardsPath() {
        if (path.size() > 1) {
            Node nextNode = path.get(1);

            // If we are already VERY close to the current node, move to the next one in the path immediately
            int distToNext = Math.abs(x - nextNode.col * gp.tileSize) + Math.abs(y - nextNode.row * gp.tileSize);
            if (distToNext < speed && path.size() > 2) {
                path.remove(0);
                nextNode = path.get(1);
            }

            int targetX = nextNode.col * gp.tileSize;
            int targetY = nextNode.row * gp.tileSize;

            int currentSpeed = (currentState == State.CHASE) ? speed : Math.max(1, speed / 2);

            int moveX = 0;
            int moveY = 0;

            if (x < targetX) moveX = Math.min(currentSpeed, targetX - x);
            else if (x > targetX) moveX = Math.max(-currentSpeed, targetX - x);

            if (y < targetY) moveY = Math.min(currentSpeed, targetY - y);
            else if (y > targetY) moveY = Math.max(-currentSpeed, targetY - y);

            // Update direction based on movement
            if (Math.abs(moveX) > Math.abs(moveY)) {
                if (moveX > 0) direction = "right";
                else direction = "left";
            } else if (Math.abs(moveY) > 0) {
                if (moveY > 0) direction = "down";
                else direction = "up";
            }

            // Alignment/Centering
            if (moveX != 0 && moveY == 0) {
                int centerY = (y / gp.tileSize) * gp.tileSize;
                if (y < centerY) y = Math.min(y + 1, centerY);
                if (y > centerY) y = Math.max(y - 1, centerY);
            }
            if (moveY != 0 && moveX == 0) {
                int centerX = (x / gp.tileSize) * gp.tileSize;
                if (x < centerX) x = Math.min(x + 1, centerX);
                if (x > centerX) x = Math.max(x - 1, centerX);
            }

            if (!checkCollision(x + moveX, y + moveY)) {
                x += moveX;
                y += moveY;
            } else {
                if (!checkCollision(x + moveX, y)) x += moveX;
                else if (!checkCollision(x, y + moveY)) y += moveY;
                else path.clear(); 
            }
        }
    }

    private void applySeparation() {
        for (Enemy other : gp.enemies) {
            if (other != null && other != this) {
                double dist = Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
                if (dist < 15) { 
                    if (x < other.x) { if (!checkCollision(x - 1, y)) x--; }
                    else { if (!checkCollision(x + 1, y)) x++; }
                    
                    if (y < other.y) { if (!checkCollision(x, y - 1)) y--; }
                    else { if (!checkCollision(x, y + 1)) y++; }
                }
            }
        }
    }

    public boolean checkCollision(int x, int y) {
        int padding = 4;
        int leftCol = (x + padding) / gp.tileSize;
        int rightCol = (x + width - padding) / gp.tileSize;
        int topRow = (y + padding) / gp.tileSize;
        int bottomRow = (y + height - padding) / gp.tileSize;

        if (leftCol < 0 || rightCol >= gp.maxScreenCol || topRow < 0 || bottomRow >= gp.maxScreenRow) return true;

        int tile1 = gp.map.map[topRow][leftCol];
        int tile2 = gp.map.map[topRow][rightCol];
        int tile3 = gp.map.map[bottomRow][leftCol];
        int tile4 = gp.map.map[bottomRow][rightCol];

        return (tile1 == 1 || tile2 == 1 || tile3 == 1 || tile4 == 1 || tile1 == 2 || tile2 == 2 || tile3 == 2 || tile4 == 2);
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = null;

        if (currentState == State.CHASE) {
            switch (direction) {
                case "up": image = agroUp; break;
                case "down": image = agroDown; break;
                case "left": image = agroLeft; break;
                case "right": image = agroRight; break;
            }
            // Fallback for agro
            if (image == null) image = agroDown;
        } else {
            switch (direction) {
                case "up": image = wanderUp; break;
                case "down": image = wanderDown; break;
                case "left": image = wanderLeft; break;
                case "right": image = wanderRight; break;
            }
            // Fallback for wander
            if (image == null) image = wanderDown;
        }

        if (image != null) {
            // Calculate aspect ratio to prevent "smooshing"
            double ratio = (double)image.getWidth() / image.getHeight();
            int drawHeight = gp.tileSize;
            int drawWidth = (int)(drawHeight * ratio);

            // Re-center horizontally and vertically over the 20x20 hitbox
            int drawX = x - (drawWidth - width) / 2;
            int drawY = y - (drawHeight - height) / 2;

            g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
        } else {
            // Final fallback to colored rectangles
            if (currentState == State.CHASE) {
                g2.setColor(new Color(255, 0, 0));
            } else {
                g2.setColor(new Color(255, 140, 0)); 
            }
            g2.fillRect(x, y, width, height);
        }
    }
}