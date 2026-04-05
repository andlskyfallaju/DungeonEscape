import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Player {
    GamePanel gp;

    int x, y;
    int speed;

    KeyHandler keyH;
    public BufferedImage up, down, left, right;
    public String direction = "down";

    public final int width = 30;
    public final int height = 30;

    public Player(GamePanel gp, KeyHandler keyH) {
        this.gp = gp;
        this.keyH = keyH;
        speed = 6;
        
        getPlayerImage();
    }

    public void getPlayerImage() {
        try {
            up = ImageIO.read(getClass().getResourceAsStream("/res/player_up.png"));
            down = ImageIO.read(getClass().getResourceAsStream("/res/player_down.png"));
            left = ImageIO.read(getClass().getResourceAsStream("/res/player_left.png"));
            right = ImageIO.read(getClass().getResourceAsStream("/res/player_right.png"));
        } catch (Exception e) {
            System.out.println("Error loading player sprites: " + e.getMessage());
        }
    }

    public boolean checkCollision(int x, int y) {

        int leftCol = x / gp.tileSize;
        int rightCol = (x + width - 1) / gp.tileSize;
        int topRow = y / gp.tileSize;
        int bottomRow = (y + width - 1) / gp.tileSize;

        int tile1 = gp.map.map[topRow][leftCol];
        int tile2 = gp.map.map[topRow][rightCol];
        int tile3 = gp.map.map[bottomRow][leftCol];
        int tile4 = gp.map.map[bottomRow][rightCol];

        return gp.map.tile[tile1].collision ||
                gp.map.tile[tile2].collision ||
                gp.map.tile[tile3].collision ||
                gp.map.tile[tile4].collision;

    }

    public void update() {

        int newX = x;
        int newY = y;

        if(keyH.upPressed) {
            newY -= speed;
            direction = "up";
        }
        if(keyH.downPressed) {
            newY += speed;
            direction = "down";
        }
        if(keyH.leftPressed) {
            newX -= speed;
            direction = "left";
        }
        if(keyH.rightPressed) {
            newX += speed;
            direction = "right";
        }

        // 🏃‍♂️ Independent X movement (Slide horizontally)
        if (!checkCollision(newX, y)) {
            x = newX;
        }

        // 🏃‍♂️ Independent Y movement (Slide vertically)
        if (!checkCollision(x, newY)) {
            y = newY;
        }
    }

    public boolean checkWin() {

        int col = x / gp.tileSize;
        int row = y / gp.tileSize;

        int tileNum = gp.map.map[row][col];

        return tileNum == 2;
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = null;

        switch (direction) {
            case "up": image = up; break;
            case "down": image = down; break;
            case "left": image = left; break;
            case "right": image = right; break;
        }

        // Fallback if the specific directional image is missing
        if (image == null) image = down; 

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
            g2.setColor(Color.white);
            g2.fillRect(x, y, 20, 20); // player = square for now
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}