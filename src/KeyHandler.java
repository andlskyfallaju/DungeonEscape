import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    public boolean upPressed, downPressed, leftPressed, rightPressed, restartPressed, escPressed, enterPressed, pPressed, gPressed, lPressed, oPressed;

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {

        int code = e.getKeyCode();

        if(code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            upPressed = true;
        }
        if(code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            downPressed = true;
        }
        if(code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if(code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
        if(e.getKeyCode() == KeyEvent.VK_R) {
            restartPressed = true;
        }
        if(code == KeyEvent.VK_ESCAPE) {
            escPressed = true;
        }
        if(code == KeyEvent.VK_ENTER) {
            enterPressed = true;
        }
        if(code == KeyEvent.VK_P) {
            pPressed = true;
        }
        if(code == KeyEvent.VK_G) {
            gPressed = true;
        }
        if(code == KeyEvent.VK_L) {
            lPressed = true;
        }
        if(code == KeyEvent.VK_O) {
            oPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        int code = e.getKeyCode();

        if(code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            upPressed = false;
        }
        if(code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            downPressed = false;
        }
        if(code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
        if(code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }
        if(e.getKeyCode() == KeyEvent.VK_R) {
            restartPressed = false;
        }
        if(code == KeyEvent.VK_ESCAPE) {
            escPressed = false;
        }
        if(code == KeyEvent.VK_ENTER) {
            enterPressed = false;
        }
        if(code == KeyEvent.VK_P) {
            pPressed = false;
        }
        if(code == KeyEvent.VK_G) {
            gPressed = false;
        }
        if(code == KeyEvent.VK_L) {
            lPressed = false;
        }
        if(code == KeyEvent.VK_O) {
            oPressed = false;
        }
    }
}