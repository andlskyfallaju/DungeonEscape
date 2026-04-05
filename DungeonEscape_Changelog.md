# Dungeon Escape - Project Evolutionary Changelog

This document summarizes the significant changes, new features, and architectural improvements we've implemented, organized by implementation date.

## 📅 April 5th, 2026 (Modernization & Competition)
### 🏆 Scoreboard & Persistence (~23:15 UTC)
*   **Enhanced Data Persistence**: The high-score system now tracks Player Name, Score, Level Reached, Game Mode, and Coins Collected.
*   **Backward Compatibility**: Added logic to handle older 3-column score logs safely.
*   **Rich Scoreboard UI**: Updated the JavaFX scoreboard for detailed, mode-specific layouts.

### 🎨 Visuals & UX (~23:01 UTC)
*   **Overlay Feedback**: 
    - **3-2-1 Countdown**: Added to every level start and restart.
    - **"Life Lost" & "Press R"**: Provides a clear pause and restart instruction when the player is hit.
*   **HUD Polish**: Modernized the on-screen display for better readability.

### 🏗️ Architecture & Engine Updates (~22:50 UTC)
*   **Adaptive Resolution & Scaling**: 
    - Standardized resolution to **960 x 720** for all screens.
    - Implemented a scaling engine using `Graphics2D.scale()` to allow full window resizability while maintaining a perfect aspect ratio.
*   **Manual UI Rendering**: Solved "flickering button" glitches by transitioning from AWT components to custom-drawn Graphics2D buttons.
*   **Level Persistence**: Added a state snapshot system to allow the exact same level layout to be restarted when a life is lost.

## 📅 April 3rd - 5th, 2026 (Expansion & Mechanics)
### 🎮 Gameplay Features: "Escape Mode"
*   **Coin Collection system**: Players must collect a set number of coins to unlock the exit.
*   **Locked Door Mechanic**: The exit remains locked with a physical padlock icon until all objectives are met.
*   **Power-ups**: Introduced the **Slow Power-up**, providing a tactical slow-down of enemies.
*   **Infinite Level Expansion**: Map size physically increases after **Level 5**, creating larger and more complex dungeons.

### 👾 Enemy AI Improvements
*   **Hybrid AI logic**: Enemies wander naturally until the player is in range, then switch to chase mode.
*   **Map Complexity Scaling**: Level-based expansion and difficulty growth.

## 📅 April 1st, 2026 (The Foundation)
### 🏗️ Early Architecture
*   **JavaFX Menu Integration**: Successfully migrated the project from basic Swing launchers to a modern **JavaFX-based Menu system** (`GameMenu.java`).
*   **Rich Asset Support**: Added support for splash screens, coin sprites, and power-up icons.
