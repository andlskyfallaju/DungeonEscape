# Dungeon Escape - Project Evolution Changelog

This document summarizes the significant changes, new features, and architectural improvements we've implemented throughout this project's development.

## 🏗️ Architecture & Engine Improvements
*   **JavaFX Menu Integration**: Successfully migrated the project from basic Swing launchers to a modern **JavaFX-based Menu system** (`GameMenu.java`).
*   **Adaptive Resolution & Scaling**: 
    - Standardized resolution to **960 x 720** for all screens.
    - Implemented a scaling engine using `Graphics2D.scale()` and translation fixes to allow full window resizability while maintaining a perfect aspect ratio.
*   **Manual UI Rendering**: Solved "flickering button" glitches by transitioning from AWT components to custom-drawn Graphics2D buttons.
*   **Level Persistence**: Added a state snapshot system to allow the exact same level layout to be restarted when a life is lost.

---

## 🎮 Gameplay Features: "Escape Mode"
*   **Coin Collection system**: Players must collect a set number of coins to unlock the exit.
*   **Locked Door Mechanic**: The exit remains locked with a physical padlock icon until all objectives are met.
*   **Power-ups**: Introduced the **Slow Power-up**, providing a tactical slow-down of enemies.
*   **Infinite Level Expansion**: Map size physically increases after **Level 5**, creating larger and more complex dungeons.

---

## 👾 Enemy AI & Dungeon Mechanics
*   **Hybrid AI logic**: Enemies wander naturally until the player is in range, then switch to chase mode.
*   **Map Complexity Scaling**: Level-based expansion and difficulty growth.
*   **Cinematic Countdown**: Added a "3-2-1" overlay triggered at the start and restart of every level.

---

## 🏆 Scoreboard & Persistence
*   **Extended Data Persistence**: The high-score system now tracks Player Name, Score, Level Reached, Game Mode, and Coins Collected.
*   **Backward Compatibility**: The system handles older save files gracefully, ensuring no data loss.
*   **Scoreboard UI Overhaul**: Richer information display within the JavaFX high scores list.

---

## 🐛 Major Bug Fixes
*   **Visual Glitch Resolution**: Fixed flickering buttons by using manual rendering.
*   **Resizing & Mouse Fix**: Corrected coordinate mapping so the game is fully playable even in a resized or maximized window.
*   **JavaFX & Threading Fix**: Resolved crashes related to returning to the menu from a running game session.
