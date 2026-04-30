import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class ExplosionTracker {
    GamePanel gp;
    double x, y;
    int size;
    public int phase = 0; // 0=Tracking, 1=Locked, 2=Exploding, 3=Done
    int timer = 0;

    public ExplosionTracker(GamePanel gp, int startX, int startY) {
        this.gp = gp;
        this.x = startX;
        this.y = startY;
        this.size = gp.tileSize;
    }

    public void update(int playerX, int playerY) {
        if (phase == 0) {
            timer++;
            // Slowly track player (lerping to player center)
            double targetX = playerX;
            double targetY = playerY;
            x += (targetX - x) * 0.05; // 5% distance per frame
            y += (targetY - y) * 0.05;

            if (timer > 180) { // 3 seconds locking on
                phase = 1;
                timer = 0;
            }
        } else if (phase == 1) {
            timer++;
            // Locked in place, turns green
            if (timer > 60) { // 1 second warning
                phase = 2; // DETONATE
                timer = 0;
                checkExplosion();
            }
        } else if (phase == 2) {
            timer++;
            if (timer > 15) { // brief explosion visual
                phase = 3; // Finished
            }
        }
    }

    private void checkExplosion() {
        // BoomRect expands mathematically by 15px per side (30px total size increase, 18px tighter than previous build)
        Rectangle boomRect = new Rectangle((int) x - 15, (int) y - 15, size + 30, size + 30);

        // Did player get caught in the blast?
        if (gp.player.getBounds().intersects(boomRect)) {
            gp.bossHitPlayer(); // special instant damage
        }

        // Did we hit a crystal?
        boolean hitCrystal = false;
        for (Crystal c : gp.crystals) {
            if (c.active && boomRect.intersects(c.getExpandedBounds())) {
                c.active = false;
                hitCrystal = true;
            }
        }

        if (hitCrystal) {
            int damage = (gp.level == 10) ? 2 : 1;
            for (ArcherBoss ab : gp.archerBosses) {
                ab.hit(damage);
            }
        }
    }

    public void draw(Graphics2D g2) {
        if (phase == 0) {
            g2.setColor(new Color(255, 0, 0, 150)); // Translucent Red
            g2.fillRect((int) x - size/2, (int) y - size/2, size*2, size*2);
        } else if (phase == 1) {
            g2.setColor(new Color(0, 255, 0, 150)); // Translucent Green
            g2.fillRect((int) x - size/2, (int) y - size/2, size*2, size*2);
        } else if (phase == 2) {
            // Detonation visual
            g2.setColor(new Color(255, 100, 0, 200));
            g2.fillOval((int) x - size, (int) y - size, size * 3, size * 3);
            g2.setColor(Color.YELLOW);
            g2.fillOval((int) x - size/2, (int) y - size/2, size * 2, size * 2);
        }
    }
}