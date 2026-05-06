# Proposal: Endgame & Progression Tuning

## 1. Difficulty Scaling (Easing the Curve)

The current formula (`2 + level + 2`) hits the 12-enemy cap very quickly (around Level 8), which creates a "gridlock" where movement becomes impossible.

### Proposed Changes:
- **Linear Smoothing**: Change the enemy count to `2 + (level / 2)`. This ensures that even at Level 10, you only have 7 ghosts, making the game about tactical movement rather than luck.
- **Dynamic Checkpoints**: If you reach Level 5 and beat the Boss, your "Restart" point moves to Level 6. No more going back to Level 1 after a hard-fought boss victory.
- **The "Breathe" Mechanic**: On levels higher than 5, give the player 2 seconds of additional "Ghost Freeze" after the countdown ends. This lets you survey the map before the hunt begins.
- **Health Refills**: Every 3 levels successfully cleared, the player recovers 1 lost heart.

---

## 2. The Endgame (The Level 10 Finale)

Instead of just infinite levels, we can create a "Final Escape" sequence at Level 10.

### The "Shattered Arena" (Boss Phase):
- **Double Trouble**: Level 10 spawns **two** Archer Bosses in a larger arena. 
- **Shattering Floor**: As you break crystals, the outer perimeter of the arena starts falling away into the void.

### The "Collapse" (Escape Phase):
- Once the bosses are defeated, the exit doesn't just open. You have 30 seconds to navigate a dynamically collapsing maze (walls turning into void) to reach the final portal.
- **Victory Reward**: Reaching the portal triggers a unique **"DUNGEON CONQUERED"** splash screen and places a golden crown icon next to your name on the Scoreboard.

---

## 3. Comparison Table

| Feature | Current | Proposed |
| :--- | :--- | :--- |
| **Enemy Cap** | 12 (Hit at Lvl 8) | 12 (Hit at Lvl 20) |
| **Restart Point** | Level 1 | Boss Checkpoints (5, 10) |
| **Lives** | Fixed 5 | +1 Heart Every 3 Levels |
| **Power-ups** | Slow-down only | Rare "Invincibility" on Boss levels |

Would you like me to start by implementing the **Checkpoint system** or the **Scaling adjustments** first?
