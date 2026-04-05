# Dungeon Escape - Detailed Technical Documentation

This document provides a comprehensive deep dive into the architecture, logic, and functional interactions within the Dungeon Escape codebase.

---

## 1. System Architecture & Entry Points

The project utilizes a hybrid approach involving **JavaFX** for high-level UI (menus) and **Java Swing** for the high-performance game engine.

### [GameMenu.java](file:///c:/Users/andim/OneDrive/Documentos/Uni%20Work%202025/Year%202/Semester%201/Programming%20in%20Java/DungeonEscape/src/GameMenu.java) (JavaFX)
The primary entry point. It manages the splash screen and main navigation.
- **`start(Stage stage)`**: Initializes the JavaFX environment. Crucially, it calls `Platform.setImplicitExit(false)` to ensure the JavaFX thread stays alive even after the menu stage is closed, allowing the game to return to the menu later.
- **`createScoreboard()`**: Dynamically builds a stylized list of high scores by fetching data from the `ScoreManager`. It uses `VBox` and `Text` nodes with CSS-like styling.
- **Button Logic**: The "START GAME" button closes the JavaFX stage and spawns the `Main.java` Swing thread.

### [Main.java](file:///c:/Users/andim/OneDrive/Documentos/Uni%20Work%202025/Year%202/Semester%201/Programming%20in%20Java/DungeonEscape/src/Main.java) (Swing)
The glue between the menu and the game engine.
- **`quitToMenu()`**: A static utility that disposes of the game's `JFrame` and uses `Platform.runLater()` to safely revive the JavaFX `GameMenu` on the correct thread.

---

## 2. The Engine: [GamePanel.java](file:///c:/Users/andim/OneDrive/Documentos/Uni%20Work%202025/Year%202/Semester%201/Programming%20in%20Java/DungeonEscape/src/GamePanel.java)

The `GamePanel` is the "Brain" of the project, extending `JPanel` and implementing `Runnable` to host the main game thread.

### The Game Loop (`run()` method)
Runs a `while` loop at ~60 FPS (16ms sleep). In every cycle, it performs two critical tasks:
1.  **`update()`**: Processes all physics, AI, and state changes.
2.  **`repaint()`**: Triggers `paintComponent()` to render the new state to the screen.

### State Logic & Transitions
The `update()` function acts as a state machine:
- **Countdown State**: If `!gameStarted`, it calculates the 3-second timer. No entity movement occurs here.
- **Result State**: If `gameOver` or `gameWon`, it freezes logic and waits for the user to press **'R'**.
- **Active State**: Normal gameplay where `player.update()` and `enemy.update()` are called.

### Life Management & Snapshots
- **`saveLevelState()`**: Captured during `generateLevel()`. It creates a deep copy of the map array and stores the initial `x,y` coordinates of all entities.
- **`loadLevelState()`**: When a life is lost (but lives > 0), this restores the blueprint. This ensures the player retries the *exact same* layout they died on, preventing "unfair" RNG on death.

---

## 3. Level Generation: [Map.java](file:///c:/Users/andim/OneDrive/Documentos/Uni%20Work%202025/Year%202/Semester%201/Programming%20in%20Java/DungeonEscape/src/Map.java)

The map is a 2D integer array (`map[row][col]`).
- **`ensurePathExists(px, py, ex, ey)`**: Uses a "Drunkard's Walk" or direct carving approach to ensure there is always a guaranteed sequence of floor tiles (0) between the player's start and the exit (2).
- **`placeExitFarFromPlayer()`**: Prevents the exit from spawning right next to the player by checking the Manhattan distance until a suitable tile is found.

---

## 4. Entity Logic & AI

### [Player.java](file:///c:/Users/andim/OneDrive/Documentos/Uni%20Work%202025/Year%202/Semester%201/Programming%20in%20Java/DungeonEscape/src/Player.java)
Handles movement via the `KeyHandler`. 
- **Collision Detection**: Its `checkCollision()` function looks at the tile array in the direction of movement. It prevents the player from entering tiles marked as `1` (Wall).

### [Enemy.java](file:///c:/Users/andim/OneDrive/Documentos/Uni%20Work%202025/Year%202/Semester%201/Programming%20in%20Java/DungeonEscape/src/Enemy.java)
Contains the most complex logic in the game.
- **A* Pathfinding (`findPath`)**:
    - Evaluates surrounding nodes based on **G-Cost** (distance from start) and **H-Cost** (est. distance to player).
    - It builds a `Path` of `Nodes`. The enemy then moves toward the first node in the list.
- **AI States**:
    - **CHASE**: The closest 4 enemies to the player calculate a path every few frames.
    - **WANDER**: Further enemies move semi-randomly to reduce CPU load and create unpredictable "patrols."

---

## 5. Persistence & Scoring

### [ScoreManager.java](file:///c:/Users/andim/OneDrive/Documentos/Uni%20Work%202025/Year%202/Semester%201/Programming%20in%20Java/DungeonEscape/src/ScoreManager.java)
Handles File I/O for the leaderboard.
- **`saveScore()`**: Appends a new entry to `highscores.txt` in the format `NAME|SCORE|LEVEL`.
- **`getTopScores()`**: Reads the file, parses strings into `ScoreEntry` objects, uses a custom `Comparator` to sort them by Score (descending), and returns the Top 10.

---

## 6. Functional Interactions (The "Big Picture")

1.  **Generation**: `GamePanel` asks `Map` for a floor. `Map` carves it. `GamePanel` then places `Player` and `Enemies` on free tiles.
2.  **Tick**: `GamePanel.update()` updates `Player` pos. It then sorts `Enemies` by distance to the player to decide which ones should be "smart" (CHASE) vs "dumb" (WANDER).
3.  **Collision**: Every frame, `GamePanel` checks if the `Rectangle` of the Player intersects any `Enemy`. We use `rect.grow(-4, -4)` to "shrink" the hitbox, making the game feel more forgiving.
4.  **Win Condition**: `playerReachedExit()` checks if the player's center overlaps with the center of the Exit tile (25% area check).
5.  **Game Over**: If `lives == 0`, `handleFinalGameOver()` triggers. It opens a `JOptionPane` for name entry (validated by RegEx `^[A-Z0-9]{3,5}$`), saves the score, shows the leaderboard, and finally calls `Main.quitToMenu()` to restart the whole lifecycle.

---

## 7. Development & Implementation Process ("How it was done")

The game evolved through several iterative refinement stages:

### A. Fixing the "Stuttering" Movement
Initially, enemies were recalculating paths every single frame, causing lag.
- **How it was done**: We implemented a frame-split logic. Instead of moving 1 pixel per frame, entities move in discrete increments only when certain conditions are met, and the A* path is cached rather than rebuilt constantly.

### B. Hitbox "Fairness" (The Shrink Technique)
Standard 32x32 hitboxes caused the game to end even if the player just "brushed" the pixel edge of an enemy.
- **How it was done**: We implemented the **Shrink Technique**. In the collision code, we use `hitbox.grow(-4, -4)`. This creates an invisible 4-pixel buffer. The player can now "near miss" an enemy visually without triggering a Game Over, which feels much more rewarding to the player.

### C. The Level "Blueprint" (Snapshots)
We noticed that if a player died and the level regenerated randomly, they might lose progress or get a "harder" map on their last life.
- **How it was done**: We added `initialMap` and `initialEnemyPositions` arrays. When a level is first generated, it's saved as a **"Blueprint."** On death, `loadLevelState()` simply copies the saved data back into the active arrays. This mimics modern roguelites where you retry the same challenge until you succeed or run out of lives.

### D. Hybrid Window Management
Integrating JavaFX (Menu) and Swing (Game) is notoriously tricky because they run on separate threads.
- **How it was done**: We used **Platform.runLater()** to send commands back to the JavaFX thread from the Swing thread. By setting `Platform.setImplicitExit(false)`, we prevented the JavaFX "engine" from shutting down when the main menu window was closed, allowing us to "resuscitate" the menu once the game window is disposed.

### E. Precise Win Detection
The original win condition was binary (just being on the tile).
- **How it was done**: We implemented a **Centered Radius** check. The code calculates the center of the 32x32 Exit tile and creates a small 16x16 "Sweet Spot." This forces the player to actually enter the ladder/hatch visually rather than just touching the corner.
