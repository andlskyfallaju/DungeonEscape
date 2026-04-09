# Dungeon Escape: Project Technical Deep-Dive (Class-by-Class)

This document provides a comprehensive technical reference for the "Dungeon Escape" source code, intended for a formal project assessment. It explains the responsibility, logic, and technical contribution of every Java class in the project.

---

## 1. Orchestration & GUI Layer

### **Main.java: The Hybrid Orchestrator**
*   **Responsibility**: The application’s entry point. It manages the `JFrame` lifecycle and handles the transition between the JavaFX Menu and the Swing Game Engine.
*   **Technical Logic**: It implements a `quitToMenu()` method that uses `Platform.runLater()` to bridge the threading gap between Swing and JavaFX, ensuring the JavaFX Application thread remains stable when transitioning from a running game.
*   **Key Contribution**: It enables the project's hybrid architecture, allowing for a modern JavaFX UI without sacrificing the high-performance Swing game loop.

### **GameMenu.java: The State Manager**
*   **Responsibility**: Manages the user-facing interface for mode selection and the high-score scoreboard.
*   **Technical Logic**: Uses a `StackPane` with a scaling root to ensure the menu remains perfectly centered regardless of the user's screen resolution. It passes selected mode strings to `Main.java` to initialize specific game logic.
*   **Key Contribution**: Provides a polished, CSS-styled frontend that enhances user experience before the game begins.

---

## 2. The Engine Core

### **GamePanel.java: The Master Controller**
*   **Responsibility**: The central "Brain" of the game. It controls the game thread (`Runnable`), updates all entities, and manages the rendering pipeline.
*   **Technical Logic**:
    *   **Thread Timing**: Manages a 60FPS loop using `Thread.sleep(16)`.
    *   **Adaptive Scaling**: Uses `Graphics2D.scale()` to translate 960x720 "Logical Coordinates" into actual window pixels.
    *   **State Management**: Controls the `isLifeLost` and `countdown` states, which dictate when game time is flowing or paused.
*   **Key Contribution**: Centralizes world state and provides the mathematical foundation for resolution-independent rendering.

### **KeyHandler.java: Asynchronous Input Mapping**
*   **Responsibility**: Listens for hardware keyboard events and translates them into Boolean flags.
*   **Technical Logic**: Implements `KeyListener`. It captures both `W/A/S/D` and `Arrow` keys, setting flags like `upPressed` or `restartPressed`. By keeping input separate from movement logic, it allows for more fluid controls.
*   **Key Contribution**: Enables multi-key input (e.g., diagonal movement) and provides a clean interface for other classes to query user intent.

---

## 3. World Logic & Navigation

### **Map.java: Procedural Generation**
*   **Responsibility**: Generates the dungeon grid, ensures paths exist, and places objectives.
*   **Technical Logic**:
    *   **Path Enforcement**: Implements `ensurePathExists()` to carve a guaranteed route between the player and the exit, preventing "impossible" map generation.
    *   **Randomization**: Scales wall density (~20% chance) to create unique dungeons for every level.
*   **Key Contribution**: Generates infinite levels with increasing complexity as the game progresses.

### **Node.java: Pathfinding Data Model**
*   **Responsibility**: A fundamental data structure for the A* pathfinding algorithm.
*   **Technical Logic**: Stores `gCost` (path distance), `hCost` (heuristic estimate), and `fCost` (total). It also holds a `parent` reference used to rebuild the path after the destination is found.
*   **Key Contribution**: Enables the AI's "Intelligence" by providing a mathematically sound way to model the world as a traversable graph.

### **Tile.java: Property Abstraction**
*   **Responsibility**: Defines the characteristics of a single grid square.
*   **Technical Logic**: A simple data class that stores `color` and `collision` booleans.
*   **Key Contribution**: Decouples visual data from collision data, allowing the map to treat different tiles as either "walkable" or "solid" through a unified array.

---

## 4. Game Entities

### **Player.java: Axis-Independent Physics**
*   **Responsibility**: Manages player state, input response, and collision physics.
*   **Technical Logic**:
    *   **Axis Decoupling**: Updates X and Y positions independently. This prevents "locking" against walls and allows the player to slide horizontally while holding the Up/Down keys.
    - **Sprite Centering**: Dynamically calculates drawing offsets to keep the animated sprite perfectly centered over the 30x30 physical hitbox.
*   **Key Contribution**: Provides responsive, professional-feeling movement mechanics.

### **Enemy.java: State-Driven AI**
*   **Responsibility**: Manages enemy navigation, behavior states, and animations.
*   **Technical Logic**:
    *   **State Machine**: Toggles between `CHASE` (pursue player) and `WANDER` (roam map) based on proximity.
    *   **Separation Force**: Implements `applySeparation()`—a subtle repulsion force that keeps enemy clusters from overlapping, keeping the challenge fair and visual feedback clear.
    *   **Pathfinding Trigger**: Re-calibrates the A* path every 15 frames for optimal performance.
*   **Key Contribution**: Provides intelligent opposition that creates a dynamic difficulty curve.

---

## 5. Data & Persistence

### **ScoreManager.java: Persistence Layer**
*   **Responsibility**: Handles file-based saving and loading of high scores.
*   **Technical Logic**: Implements `saveScore()` and `getTopScores()` using CSV (Comma-Separated Values) serialization. It handles legacy data conversion to ensure that older 3-column data remains readable by the newer 5-column system.
*   **Key Contribution**: Promotes replayability by allowing players to track their progress through permanent storage (`highscores.txt`).

### **ScoreEntry.java: Data Encapsulation**
*   **Responsibility**: A lightweight container for a single scoreboard record.
*   **Technical Logic**: Stores Name, Score, Level, Mode, and Coins for a single run. Includes a `toString()` override for easy file-writing.
*   **Key Contribution**: Simplifies data handling across the `ScoreManager`, `GamePanel`, and `GameMenu`.
