import java.io.*;
import java.util.*;

public class ScoreManager {
    private static final String FILE_PATH = "highscores.txt";

    public static void saveScore(String name, int score, int level, String mode, int coins) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            writer.write(name + "," + score + "," + level + "," + mode + "," + coins);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<ScoreEntry> getTopScores() {
        List<ScoreEntry> scores = new ArrayList<>();
        File file = new File(FILE_PATH);
        
        if (!file.exists()) return scores;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String name = parts[0];
                    int s = Integer.parseInt(parts[1]);
                    int l = Integer.parseInt(parts[2]);
                    String mode = (parts.length >= 4) ? parts[3] : "CASUAL";
                    int coins = (parts.length >= 5) ? Integer.parseInt(parts[4]) : 0;
                    scores.add(new ScoreEntry(name, s, l, mode, coins));
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }

        // Sort: Score DESC, then Level DESC
        scores.sort((a, b) -> {
            if (b.score != a.score) return b.score - a.score;
            return b.level - a.level;
        });

        // Top 10 only
        if (scores.size() > 10) {
            return scores.subList(0, 10);
        }
        return scores;
    }
}
