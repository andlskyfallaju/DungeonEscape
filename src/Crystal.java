import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Crystal {
    public int x, y;
    public boolean active = true;
    GamePanel gp;
    
    public Crystal(GamePanel gp, int col, int row) {
        this.gp = gp;
        this.x = col * gp.tileSize;
        this.y = row * gp.tileSize;
    }
    
    public Rectangle getExpandedBounds() {
        // Reduced leeway per user feedback (18px expansion per side instead of 24px)
        int leeway = 18;
        return new Rectangle(x - leeway, y - leeway, gp.tileSize + leeway * 2, gp.tileSize + leeway * 2);
    }
    
    public void draw(Graphics2D g2) {
        if (!active) return;
        g2.setColor(new Color(0, 255, 255, 200)); // Glowing Cyan
        
        // Draw a diamond shape
        int[] xPts = {x + gp.tileSize/2, x + gp.tileSize, x + gp.tileSize/2, x};
        int[] yPts = {y, y + gp.tileSize/2, y + gp.tileSize, y + gp.tileSize/2};
        g2.fillPolygon(xPts, yPts, 4);
        
        g2.setColor(Color.WHITE);
        g2.drawPolygon(xPts, yPts, 4);
    }
}
