import java.io.Serializable;

public class ScoreEntry implements Serializable {
    public String name;
    public int score;
    public int level;
    public String mode;
    public int coins;

    public ScoreEntry(String name, int score, int level, String mode, int coins) {
        this.name = name;
        this.score = score;
        this.level = level;
        this.mode = mode;
        this.coins = coins;
    }

    @Override
    public String toString() {
        return name + "," + score + "," + level + "," + mode + "," + coins;
    }
}
