# Dungeon Escape: Project Evolution & Technical Catalog

This document provides a detailed technical assessment of the transformation from the original "DungeonEscape" prototype (Backup) to the current feature-rich implementation.

---

## 1. Core Engine & Rendering Architecture

### **The Original State (Lacked)**
The backup utilized a fixed-coordinate system locked to a 32px tile size and a 20x15 grid. This resulted in a small, non-resizable 640x480 window. Any attempt to maximize the window resulted in empty space, and there was no abstraction between "game units" and "pixel units."

### **The Transformation (Accounted For)**
We implemented a **Universal Adaptive Scaling Engine**. The game now targets a base "Logical Resolution" of **960 x 720** (48px tiles), but uses dynamic matrix transformation to fit any window size.

### **Technical Breakdown: Scaling Process**
1.  **Logical vs. Real Dimensions**: Introduced `logicalWidth` and `logicalHeight` constants to maintain a consistent aspect ratio.
2.  **Affine Transformation**: In `GamePanel.paintComponent`, we added a `Graphics2D.scale()` call. This determines the `scaleFactor` by dividing the current window width by the target 960px.
3.  **Coordinate Mapping**: Updated the Mouse Listeners to divide the `e.getX()` and `e.getY()` raw inputs by the `scaleFactor`, ensuring that mouse clicks hit buttons accurately regardless of window size or maximization.

---

## 2. Pathfinding Subsystem & Node Logic

### **Overview: Node.java Implementation**
To guide enemy movement, we implemented an **A* (A-Star) Pathfinding algorithm**. The `Node.java` class serves as the fundamental data structure for this navigation system.

### **Node Properties & Heuristics:**
*   **Grid Coordinates (`col`, `row`)**: Represents the discrete tile position in the dungeon grid.
*   **gCost (Movement Cost)**: The actual distance from the starting point to the current node.
*   **hCost (Heuristic Estimate)**: The estimated movement cost to the target (calculated using **Manhattan Distance**).
*   **fCost (Total Cost)**: Defined as `gCost + hCost`. The AI prioritized visiting nodes with the lowest `fCost` to find the most efficient path.
*   **Node Linking**: Each node stores a reference to its `parent`, allowing the AI to "backtrack" and construct the final movement list once the goal is reached.

### **The AI Execution Loop:**
1.  **Search**: Every 15 frames, the enemy calculates a new path to the player (in Chase Mode) or a random file (in Wander Mode).
2.  **Smoothing**: The `moveTowardsPath()` logic identifies the next node in the sequence and calculates the exact pixel-based velocity vector required to steer the enemy center-point toward that node.
3.  **Axis Decoupling**: We implemented independent X and Y movement, which allows enemies to slide along walls if they are blocked in one direction.

---

## 3. Intelligent Behavior (AI) System

### **The Original State (Lacked)**
The backup AI was a simple script where all enemies chased the player constantly, leading to unrealistic overlapping (the "Death Ball" effect).

### **The Transformation (Accounted For)**
We developed a **State-Driven Multi-Tier AI** featuring `WANDER` and `CHASE` states, proximity-based agro, and separation physics.

### **Implementation Mechanics:**
1.  **State Machine**: Added a `currentState` enum. In `ESCAPE` mode, enemies default to `WANDER` (smart roaming) and only switch to `CHASE` when the player is within range.
2.  **Separation Forces**: Implemented `applySeparation()`, which applies a subtle "repulsion" force when two enemies get too close, keeping the group structure natural and readable.
3.  **Performance Optimization**: Pathfinding now only updates every 15 frames, ensuring the game remains at 60 FPS even with 10+ active enemies.

---

## 4. Physics & Collision Mechanics

### **The Original State (Lacked)**
The backup used a "Unified Collision Check." If a player moved diagonally and hit a wall, both axes of movement stopped. This created a "sticky" feel when turning corners.

### **The Transformation (Accounted For)**
We implemented **Independent Axis Sliding** and **Hitbox Centering**.

### **Technical Process:**
1.  **Axis Separation**: In `Player.update()`, we split updates into two discrete checks: `checkCollision(newX, y)` followed by `checkCollision(x, newY)`. This allows horizontal sliding while against a vertical wall.
2.  **Hitbox Padding**: Added 4-pixel internal padding to tile-collision checks, preventing the sprite from "snagging" on corner tiles.

---

## 5. Dynamic Mechanics & Mode Logic

### **Objective Logic: Escape Mode**
Unlike the basic prototype, the current engine utilizes an **Objective Validation Loop**.
1.  **State Initialization**: Upon level entry, `exitLocked` is set to true.
2.  **Item Tracking**: The game tracks a `List<Point> coins`.
3.  **Automated Unlocking**: When the `coins.size()` reaches zero, the `exitLocked` flag is cleared, and the exit tile's visual representation is updated from a padlock to an open door.

### **Dynamic Scaling Engine**
To ensure the game remains challenging, we implemented a **Mathematical Scaling System**:
*   **Enemy density**: Calculated as `min(12, 2 + level + (maxScreenCol / 10))`.
*   **Grid Expansion**: Starting at Level 5, the dungeon grid physically expands (+2 Cols, +1 Row) for every new floor, broadening the play area as the player levels up.

---

## 6. Persistence & Data Layer

### **The Original State (Lacked)**
The backup had no way to save scores. Closing the app meant total data loss.

### **The Transformation (Accounted For)**
We implemented a **File-Based Scoreboard** using CSV serialization (`highscores.txt`).

### **Data Handling Process:**
1.  **Persistent Storage**: Created `ScoreManager.java` using `BufferedWriter` for safe file writes.
2.  **Enhanced Metadata**: The system now stores Name, Score, Level, Mode, and Coins Collected—allowing for complex leaderboard analysis in the future.
3.  **Legacy Protection**: Added parsing logic to ensure older 3-column data from earlier versions remains readable by the newer 5nd-generation parser.
