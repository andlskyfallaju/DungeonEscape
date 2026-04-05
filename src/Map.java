import java.awt.*;
import java.util.Random;
import javax.imageio.ImageIO;


public class Map {

    GamePanel gp;
    Tile[] tile;
    int[][] map;

    public Map(GamePanel gp) {
        this.gp = gp;

        tile = new Tile[3];
        map = new int[gp.maxScreenRow][gp.maxScreenCol];

        getTileImage();
        loadMap();
    }

    public Point getRandomFreeTile() {
        return getRandomFreeTileFarFrom(null, 0);
    }

    public Point getRandomFreeTileFarFrom(Point p, int minDistance) {

        Random rand = new Random();
        int col, row;
        int attempts = 0;

        while(true) {
            col = rand.nextInt(gp.maxScreenCol);
            row = rand.nextInt(gp.maxScreenRow);
            attempts++;

            if(map[row][col] == 0) { 
                if(p == null) return new Point(col, row);

                int distance = Math.abs(col - p.x) + Math.abs(row - p.y);
                if(distance >= minDistance || attempts > 100) {
                    return new Point(col, row);
                }
            }
        }
    }

    public void getTileImage() {
        try {
            // FLOOR (0)
            tile[0] = new Tile();
            tile[0].image = ImageIO.read(getClass().getResourceAsStream("/res/floor.png"));
            tile[0].collision = false;

            // WALL (1)
            tile[1] = new Tile();
            tile[1].image = ImageIO.read(getClass().getResourceAsStream("/res/wall.png"));
            tile[1].collision = true;

            // EXIT (2)
            tile[2] = new Tile();
            tile[2].image = ImageIO.read(getClass().getResourceAsStream("/res/exit.png"));
            tile[2].collision = false;
        } catch (Exception e) {
            System.out.println("Error loading tile images: " + e.getMessage());
            // Fallback colors if images fail
            tile[0] = new Tile(); tile[0].color = Color.darkGray; tile[0].collision = false;
            tile[1] = new Tile(); tile[1].color = Color.gray; tile[1].collision = true;
            tile[2] = new Tile(); tile[2].color = Color.green; tile[2].collision = false;
        }
    }

    public void loadMap() {

        Random rand = new Random();

        map = new int[gp.maxScreenRow][gp.maxScreenCol];

        for(int row = 0; row < gp.maxScreenRow; row++) {
            for(int col = 0; col < gp.maxScreenCol; col++) {

                // Border = always wall
                if(row == 0 || col == 0 || row == gp.maxScreenRow - 1 || col == gp.maxScreenCol - 1) {
                    map[row][col] = 1;
                } else {
                    // Random walls (20% chance)
                    if(rand.nextInt(100) < 20) {
                        map[row][col] = 1;
                    } else {
                        map[row][col] = 0;
                    }
                }
            }
        }
        // Note: ensurePathExists is now called from GamePanel after positions are set
    }

    public void ensurePathExists(int startX, int startY, int endX, int endY) {

        Random rand = new Random();
        int curX = startX;
        int curY = startY;

        // Path carver: moves from start to end ensuring a floor exists
        while (curX != endX || curY != endY) {
            
            // Carve a 2x2 area to make paths wider and less claustrophobic
            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    int carveX = curX + i;
                    int carveY = curY + j;
                    if (carveX > 0 && carveX < gp.maxScreenCol - 1 && carveY > 0 && carveY < gp.maxScreenRow - 1) {
                        map[carveY][carveX] = 0;
                    }
                }
            }

            // Move towards target
            if (curX != endX && (curY == endY || rand.nextBoolean())) {
                if (curX < endX) curX++;
                else curX--;
            } else {
                if (curY < endY) curY++;
                else curY--;
            }
        }
        
        // Ensure the actual exit tile is marked correctly
        map[endY][endX] = 2;
    }

    public Point placeExitFarFromPlayer(int playerCol, int playerRow) {

        Random rand = new Random();

        int col, row;

        while(true) {

            col = rand.nextInt(gp.maxScreenCol);
            row = rand.nextInt(gp.maxScreenRow);

            int distance = Math.abs(col - playerCol) + Math.abs(row - playerRow);

            // only place exit far away
            if(map[row][col] == 0 && distance > 10) {
                map[row][col] = 2;
                return new Point(col, row);
            }
        }
    }

    public void draw(Graphics2D g2) {
        for(int row = 0; row < gp.maxScreenRow; row++) {
            for(int col = 0; col < gp.maxScreenCol; col++) {
                int tileNum = map[row][col];
                int x = col * gp.tileSize;
                int y = row * gp.tileSize;

                if (tileNum == 2 && gp.exitLocked && gp.lockedDoorImg != null) {
                    g2.drawImage(gp.lockedDoorImg, x, y, gp.tileSize, gp.tileSize, null);
                } else if (tile[tileNum].image != null) {
                    g2.drawImage(tile[tileNum].image, x, y, gp.tileSize, gp.tileSize, null);
                } else {
                    g2.setColor(tile[tileNum].color);
                    g2.fillRect(x, y, gp.tileSize, gp.tileSize);
                }
            }
        }
    }
}