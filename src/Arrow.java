import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Arrow {
    public double x, y;
    double dx, dy;
    public boolean active = true;
    final int size = 12; // slightly visible arrow bounds
    GamePanel gp;

    public Arrow(GamePanel gp, int startX, int startY, int targetX, int targetY) {
        this.gp = gp;
        this.x = startX;
        this.y = startY;
        
        double angle = Math.atan2(targetY - startY, targetX - startX);
        double speed = 7.0; // Fast projectile
        this.dx = Math.cos(angle) * speed;
        this.dy = Math.sin(angle) * speed;
    }

    public Arrow(GamePanel gp, double startX, double startY, double dx, double dy) {
        this.gp = gp;
        this.x = startX;
        this.y = startY;
        this.dx = dx;
        this.dy = dy;
    }

    public void update() {
        x += dx;
        y += dy;

        // Verify wall collisions via center coordinate, preventing edge-case wall clipping
        int cx = (int) (x + size / 2.0);
        int cy = (int) (y + size / 2.0);
        int col = cx / gp.tileSize;
        int row = cy / gp.tileSize;
        
        if (row >= 0 && row < gp.maxScreenRow && col >= 0 && col < gp.maxScreenCol) {
            if (gp.map.map[row][col] == 1) {
                active = false; // hit a wall or pillar
            }
        } else {
            active = false; // out of map bounds
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, size, size);
    }

    public void draw(Graphics2D g2) {
        g2.setColor(Color.YELLOW);
        g2.fillOval((int) x, (int) y, size, size);
        g2.setColor(Color.WHITE);
        g2.drawOval((int) x, (int) y, size, size);
    }
}
