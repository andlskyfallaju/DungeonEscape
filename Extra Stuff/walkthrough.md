# Level 10 Finale: The Void's End

The Level 10 finale has been successfully implemented as a two-phase cinematic encounter. This update transforms the finale into a high-stakes struggle that combines boss combat with a frantic chase sequence.

## Phase 1: The Shattering Arena
In the first phase, you face off against the **Archer Boss Twins** in a decaying arena.

- **Dynamic Cover**: For every crystal destroyed, one of the four stone pillars crumbles. By the final crystal, you are left with no place to hide.
- **Collapsing Floor**: As the fight progresses, random floorboards fall into the void. These holes are non-walkable, forcing you to navigate a treacherous path while dodging arrow hell.
- **Twin Bosses**: Two bosses fire simultaneously, and damage dealt to crystals propagates to both.

## Phase 2: The Void Chase
Once the bosses are defeated, the real terror begins.

- **Scrolling Hallway**: The game camera now follows you horizontally as you sprint through a massive 300-column corridor filled with obstacles.
- **The Wall of Void**: An animated, pulsating wave of darkness relentlessly chases you from the left. Touch it, and it's game over.
- **Stagger Mechanic**: Special "Stagger Rubble" obstacles halve your movement speed for 2 seconds. These don't block you but make every second count as the Void closes in.
- **Mega Portal**: At the end of the 300 columns lies a massive portal. To win, you must enter its center and hold your position for **3 seconds** while it charges.

## Technical Refactors
- **Camera System**: Implemented a global translation system in `GamePanel` that allows for infinite horizontal scrolling while keeping the HUD and tutorials stationary.
- **Entity Lists**: Transitioned entity management (Bosses) to a dynamic list, allowing for multiple simultaneous boss instances.
- **Advanced Collision**: Enhanced the collision engine to support "Stagger" states that modify player velocity rather than just blocking movement.

## How to Test
1. Select **Escape Mode**.
2. Reach Level 10.
3. Survive the arena and lead the Void toward the portal!

> [!TIP]
> Don't get greedy with the bosses—keep an eye on the floor! Falling into a hole is just as deadly as an arrow.
