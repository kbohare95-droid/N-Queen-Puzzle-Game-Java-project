# N-Queen Puzzle Game (Levels, Elimination & DSA Visualizer)

An interactive **Java Swing Desktop Puzzle Game** (zero external dependencies) with **7 progressive levels**, **lives & elimination**, **countdown timer**, **star ratings**, and a **live backtracking visualizer**, built around four core DSA concepts: **2D Arrays**, **Custom Stack ADT**, **Recursion**, and **Backtracking**.

## Game Mechanics

- **7 Progressive Levels**: Apprentice (`4x4`) → Squire (`5x5`) → Knight (`6x6`) → Bishop (`7x7`) → Royal Master (`8x8`) → Grandmaster (`9x9`) → Legend (`10x10`).
- **Elimination (Game Over)**: Each level gives you **3 Lives (`♥ ♥ ♥`)** and a **Countdown Timer**. Placing a queen on an attacked square flashes a red attack line from the attacking queen and costs **1 life**. Losing all 3 lives or hitting `00:00` eliminates you!
- **Win & Progression**: Place all `N` queens safely to earn a **1–3 Star Rating**, score points, and advance to the next level.
- **DSA Live Controls**: Undo/Redo via custom `MoveStack`, Recursive Hints, optional Attacked-Square Helper, Live Stack Inspector, and animated Recursive & Custom-Stack Backtracking visualizers.

## How to Compile & Run

Requires **JDK 11 or higher**.

```powershell
# Inside the n-queen-puzzle-game/src folder:
javac *.java
java Main
```
