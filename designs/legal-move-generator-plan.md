# Legal Move Generator — Design Document

**Package:** `soc/rules/`  
**Effort:** ~23 days  
**ImmutableSOC changes:** 0 (zero state modifications)  
**Status:** Design complete, ready for implementation

## Overview

A pure-function layer between ImmutableSOC's replay applier and any RL framework. Determines all legal Catan moves from state + current-turn move log. No RL knowledge, no state mutation.

### Layer Architecture

```
src/main/scala/
  game/              # Generic game engine (ImmutableGame, GameState, GameAction...)
  soc/
    core/            # Catan domain types (Resource, Vertex, SOCBoard, moves...)
    base/            # ImmutableSOC — move applier (actions, state, BaseGame, builder...)
    legality/        # Legal Move Generator — wraps ImmutableSOC (THIS LAYER)
```

`soc/rules/` is a **peer** of `soc/base/`, not a sub-package. This makes the wrapping relationship explicit in the directory tree:

```
┌──────────────────────────────┐
│   RL Framework Layer          │  obs tensors, action masks, rewards
├──────────────────────────────┤
│   soc/rules/               │  ← THIS LAYER (wraps ImmutableSOC)
│   Legal Move Generator        │
├──────────────────────────────┤
│   soc/base/                   │  ImmutableSOC (applies validated moves)
├──────────────────────────────┤
│   soc/core/                   │  Domain types
├──────────────────────────────┤
│   game/                       │  Generic game engine
└──────────────────────────────┘
```

**Why `soc/rules/` not a sub-package of `soc/base/`?**

- A sub-package inside `soc/base/` implies the rules engine is a feature *inside* ImmutableSOC
- `soc/rules/` is a separate module that **wraps** `soc/base/` — it depends on ImmutableSOC but sits above it
- The dependency flows one direction: `soc/rules/` → `soc/base/` → `soc/core/` → `game/`
- Consumers (RL frameworks) depend only on `soc/rules/`, never directly on `soc/base/`

## Key Architectural Decision: Phase from Move Log

Instead of an external `TurnPhase` variable, phase is **derived** from `(state, turnMoves)` where `turnMoves` is the sequence of moves applied since the last `EndTurnMove`. The RL environment maintains this log (flush on EndTurn). Phase is a ~40-line pure function — no state to maintain.

Reasons:
- ImmutableSOC has no current-player tracking and no phase concept
- Adding phase tracking to ImmutableSOC breaks its simple replay-applier design
- The turn's move log contains all information needed to derive the phase
- This mirrors ImmutableSOC's philosophy: derive from data, don't add state

## Core API

```scala
trait LegalMoveGenerator[-STATE, +MOVE]:
  /** Returns all legal moves for a player in the current state and turn context. */
  def legalMoves(state: STATE, player: Int, turnMoves: Seq[MOVE]): Seq[MOVE]
  
  /** Returns legal moves grouped by runtime class for hierarchical policies. */
  def legalMovesGrouped(state: STATE, player: Int, turnMoves: Seq[MOVE]): Map[Class[?], Seq[MOVE]]
  
  /** Validates a specific move (used for lazy trade combos and MCTS expansion). */
  def isLegal(state: STATE, player: Int, turnMoves: Seq[MOVE], move: MOVE): Boolean
  
  /** Returns true if the game has ended (any player ≥ 10 VP, including hidden VPs). */
  def isTerminal(state: STATE): Boolean
  
  /** Returns the winning player(s), or None if game is ongoing. */
  def winners(state: STATE): Option[Set[Int]]
```

Two concrete instances:

```scala
object PerfectInfoLegalMoves
  extends LegalMoveGenerator[PerfectInfoState, PerfectInfoMove]

object PublicInfoLegalMoves
  extends LegalMoveGenerator[PublicInfoState, PublicInfoMove]
```

Both share internal helper logic; only inventory/dev-card lookups differ.

## Phase Derivation

```scala
object PhaseMachine:
  enum TurnPhase:
    case Setup
    case PreRoll
    case MainPlay(devCardPlayed: Boolean)
    case DiscardPhase(pendingPlayers: Set[Int])
    case RobberPhase(player: Int)
    case GameOver

  /** Derives the current phase from state and the turn's move log. */
  def phase[M](state: STATE, turnMoves: Seq[M]): TurnPhase =
    if isTerminal(state) then GameOver
    else if inSetup(state) then Setup
    else if !diceRolled(turnMoves) then PreRoll
    else if hasSevenRolled(turnMoves) && stillNeedDiscards(state) then
      DiscardPhase(playersWithTooManyCards(state))
    else if hasSevenRolled(turnMoves) && !robberMoved(turnMoves) then
      RobberPhase(activePlayer(state))
    else
      MainPlay(hasPlayedDevCardThisTurn(turnMoves))
```

Helper functions inspect `turnMoves` for presence of specific move types:
- `diceRolled(turnMoves)` — checks for `RollDiceMoveResult`
- `hasSevenRolled(turnMoves)` — checks the dice result value equals 7
- `robberMoved(turnMoves)` — checks for `RobberMoveResult` or play-knight moves
- `hasPlayedDevCardThisTurn(turnMoves)` — checks for any dev card play move
- `inSetup(state)` — `setupPlacementOrder.length < numPlayers * 2`
- `stillNeedDiscards(state)` — any player has total cards > 7
- `playersWithTooManyCards(state)` — set of player IDs with > 7 cards

### Phase Transitions

| Current Phase | Move Applied | Next Phase |
|---|---|---|
| Setup | InitialPlacementMove (not last) | Setup |
| Setup | last InitialPlacementMove | PreRoll |
| PreRoll | Dev card play | PreRoll (marks card played in log) |
| PreRoll | RollDice (non-7) | MainPlay |
| PreRoll | RollDice = 7, players > 7 cards | DiscardPhase |
| PreRoll | RollDice = 7, no players > 7 cards | RobberPhase |
| DiscardPhase | DiscardMove | DiscardPhase (remaining) or RobberPhase |
| RobberPhase | RobberMoveResult | MainPlay |
| MainPlay | Build/Trade/Buy/PlayDevCard | MainPlay |
| MainPlay | EndTurnMove | PreRoll (next player, turnMoves flushed) |
| Any | Any move → VP ≥ 10 | GameOver |

### Phase Behavior Notes

- **PreRoll**: Dice roll is mandatory and a *chance node*. The generator signals "needs roll" — the RL layer samples the outcome (2-12) and applies `RollDiceMoveResult`. All dev card types are legal in PreRoll (standard Catan rules), limited to max 1 per turn via `turnMoves` inspection.
- **DiscardPhase**: Multi-player — ALL players with >7 cards must discard, in any order. Generator returns discards for all affected players.
- **RobberPhase**: Only for post-7 scenarios. Knight plays embed robber placement in the single `PlayKnightResult` move (no separate phase).
- **MainPlay**: All building, trading, buying, and dev card plays are legal (with the one-dev-card-per-turn limit tracked via `turnMoves`).
- **GameOver**: Detected when any player reaches 10+ VP. Terminal check sums `playerPoints` + unplayed VP dev cards.

## File Structure

```
soc/rules/                  # Wraps soc/base/ (not inside it)
├── package.scala              # PhaseMachine, TurnPhase, re-exports
├── CachedBoard.scala          # Precomputed geometry (neighbors, edges, ports, hex-to-vertex)
├── LegalMoveGenerator.scala   # trait + Perfect/Public concrete objects
└── validators/
    ├── SetupValidator.scala        # InitialPlacementMove
    ├── BuildingValidator.scala     # BuildRoad, BuildSettlement, BuildCity
    ├── TradeValidator.scala        # PortTrade + Trade (lazy parameter ranges)
    ├── RobberValidator.scala       # Robber placement + steal targets
    ├── DevCardValidator.scala      # Buy + 5 play types (Knight, Monopoly, YOP, RoadBuilder, Point)
    └── TurnValidator.scala         # EndTurn, Discard, phase gating
```

## Validator Design Pattern

Each validator is a pure object with functions that take state + player + context and return `Seq[MOVE]`. All functions are side-effect free.

```scala
object BuildingValidator:
  def roadMoves(state: STATE, player: Int, inv: ResourceView, 
                edgeState: EdgeBuildingState[BaseEdgeBuilding],
                vertexState: VertexBuildingState[BaseVertexBuilding],
                cached: CachedBoard): Seq[BuildRoadMove]
```

### Shared Infrastructure

**CachedBoard** — computed once from `BaseBoard`, reused across all validators:
```scala
class CachedBoard[Res](board: BaseBoard[Res])(using SOCBoard[Res, BaseBoard[Res]]):
  val vertices: Seq[Vertex]
  val edges: Seq[Edge]
  val neighbors: Map[Vertex, Seq[Vertex]]           // neighboringVertices
  val edgesFromVertex: Map[Vertex, Seq[Edge]]       // edges incident to vertex
  val hexesForVertex: Map[Vertex, Seq[BoardHex[Res]]]  // hexes surrounding vertex
  val numberHexes: Map[Int, Seq[BoardHex[Res]]]     // dice-roll → hexes
  val portEdges: Map[Edge, Port]                    // edge → port type
  val hexToVertices: Map[Int, Seq[Vertex]]          // hex node → its 6 vertices
```

**ResourceView** — abstracts inventory access for perfect vs public info:
```scala
trait ResourceView:
  def getTotal(player: Int): Int
  def hasEnough(player: Int, resources: InventorySet[Resource, Int]): Boolean
  def resourceAmount(player: Int, resource: Resource): Int
```

**DevCardView** — abstracts dev card inventory:
```scala
trait DevCardView:
  def hasUnexpiredCard(player: Int, card: DevelopmentCard, currentTurn: Int): Boolean
  def deckNonEmpty: Boolean
```

### Cost Constants

```scala
ROAD_COST       = ResourceSet(WOOD, BRICK)                  // 1 wood, 1 brick
SETTLEMENT_COST = ResourceSet(WOOD, BRICK, WHEAT, SHEEP)    // 1 each of 4 types
CITY_COST       = ResourceSet(ORE, ORE, ORE, WHEAT, WHEAT)  // 3 ore, 2 wheat
DEV_CARD_COST   = ResourceSet(ORE, WHEAT, SHEEP)            // 1 each of 3 types
```

## Per-Move Legality Checks

| # | Move | Phase | Key Checks | Complexity |
|---|------|-------|------------|------------|
| 1 | `InitialPlacementMove` | Setup | Slot order, empty vertex, distance rule, edge incident & empty | Medium |
| 2 | `RollDiceMoveResult` | PreRoll | No `RollDiceMoveResult` in `turnMoves` yet. Dice is a *chance node* — generator signals needs-roll, RL layer samples 2-12 | Low |
| 3 | `EndTurnMove` | MainPlay | Dice must be rolled (RollDice in `turnMoves`), correct player | Low |
| 4 | `BuildRoadMove` | MainPlay | Afford 1w+1b, empty edge, road connected to player's road/settlement, no enemy settlement blocking, ≤15 roads | Medium |
| 5 | `BuildSettlementMove` | MainPlay | Afford 1w+1b+1wh+1sh, empty vertex, distance rule, road adjacent to vertex, ≤5 settlements+cities | Medium |
| 6 | `BuildCityMove` | MainPlay | Afford 3o+2wh, owns settlement at vertex, ≤4 cities | Low |
| 7 | `PortTradeMove` | MainPlay | Port access (2:1/3:1/4:1). Returns parameter ranges lazily. | High |
| 8 | `TradeMove` | MainPlay | Both afford, partner exists, not self. Lazy: returns valid ranges per param. | High |
| 9 | `DiscardMove` | DiscardPhase | 7-rolled in `turnMoves`, total cards > 7, correct amount = floor(total/2). Any affected player. | Medium |
| 10 | `RobberMoveResult` | RobberPhase | 7-rolled, no robber move in `turnMoves` yet. New hex ≠ current, victim has building on hex + >0 cards. | Medium |
| 11 | `BuyDevCardMoveResult` | MainPlay | Afford 1o+1wh+1sh, deck not empty | Low |
| 12 | `PlayKnightResult` | PreRoll/MainPlay | Owns KNIGHT, not bought this turn (buyTurn ≠ currentTurn), no dev card played yet (check `turnMoves`) | Medium |
| 13 | `PlayMonopolyMoveResult` | PreRoll/MainPlay | Owns MONOPOLY, not bought this turn, no dev card played yet | Low |
| 14 | `PlayYearOfPlentyMove` | PreRoll/MainPlay | Owns YOP, not bought this turn, no dev card played yet, bank has chosen resources | Low |
| 15 | `PlayRoadBuilderMove` | PreRoll/MainPlay | Owns ROAD_BUILDER, not bought this turn, no dev card played yet. 1-2 free roads (same connectivity rules as BuildRoad) | Medium |
| 16 | `PlayPointMove` | Auto | Auto-revealed by `isTerminal()` when 10+ VP reached. Not an agent decision. | Low |

### Piece Limit Counting

Piece limits are enforced by scanning building state maps:
- Settlements: count `vertexBuildingState.map` entries where building type is `Settlement` and player matches
- Cities: count entries where building type is `City`
- Roads: count `edgeBuildingState.map` entries owned by the player
- Limits: 5 settlements + cities, 4 cities, 15 roads

These counts are computed once per `legalMoves()` call and cached for the duration of that call.

### Lazy Trade Enumeration

Trade and PortTrade moves have combinatorial parameter spaces. Instead of full enumeration:

```
legalTradeParams(state, player): 
  → (partners: Seq[Int], giveResources: Seq[Resource], 
     getResources: Seq[Resource], maxGiveAmounts: Map[Resource, Int])

legalPortTradeParams(state, player):
  → (availableRatios: Seq[(Int, Resource)],   // (2, WOOD), (3, MISC), (4, ANY)
     giveOptions: Seq[Resource], getOptions: Seq[Resource])
```

The RL layer uses these ranges for hierarchical action selection or sampling. Individual trade combos are validated via `isLegal(move)`.

## Perfect vs. Public Info Modes

Most validator logic is identical between modes. Only inventory lookups differ:

| Concern | Perfect Info | Public Info |
|---------|-------------|-------------|
| Resource affordability | Exact — checks specific card types in `PrivateInventories` | Approximate — checks total card count in `PublicInventories` |
| Dev card ownership | Exact — `PrivateDevCardInv` tracks card identity + buy-turn | Approximate — `PublicDevCardInv` tracks count only |
| Can't play same-turn bought | Enforceable — compare `buyTurn != currentTurn` | Not enforceable — no buy-turn data |
| Robber steal resource | Known — `Some(WOOD)` | May be hidden — `None` |
| Bought dev card identity | Known — exact top-of-deck card | Hidden — `Option[Card]` |
| Legal move set accuracy | Exact | Superset — some moves may be illegal in ground truth |

Public-info mode returns a **superset** of actually-legal moves. The RL environment must handle rejection when a move that appeared legal fails at execution time. This is inherent to imperfect-information games.

## RL Integration Contract

| Direction | Call | Description |
|-----------|------|-------------|
| Generator → RL | `legalMoves(state, player, turnMoves)` | Full list of legal move objects |
| Generator → RL | `legalMovesGrouped(state, player, turnMoves)` | Moves grouped by class (`Map[Class[?], Seq[MOVE]]`) |
| Generator → RL | `isLegal(state, player, turnMoves, move)` | Single-move validation |
| Generator → RL | `isTerminal(state)` | Game over detection (includes hidden VPs) |
| Generator → RL | `winners(state)` | Winning player(s) or None |
| RL → ImmutableSOC | `game.applyMoveAny(move, state)` | Apply selected move, get `(output, nextState)` |
| RL → turnMoves | `turnMoves :+ move` / `Nil` | Append after move; flush to Nil on EndTurnMove |

The `turnMoves` list is maintained entirely by the RL environment:
- Initial value: `Nil` (empty list)
- After each `applyMoveAny`: append the move
- After `EndTurnMove`: reset to `Nil`
- Passed to every generator call

## Risks & Edge Cases

### Lazy trade enumeration may need sampling
With lazy ranges, the RL layer gets parameter domains (valid partners, valid give/get resource counts, valid ratios). MCTS agents will need `sampleRandomTradeMove()` for node expansion. The generator provides helper functions but doesn't dictate the sampling strategy.

### Public-info move rejection at runtime
Public-info returns approximate legality. The RL environment must handle `applyMove` failures gracefully (the move was legal per public-info but not per ground truth). This is standard for imperfect-information games.

### Bank shortage on dice rolls
When a dice roll would distribute more resources than the bank holds, distributions are capped. The `RollDiceAction` handles this, but the legal move generator doesn't need to — dice sampling is handled by the RL environment.

### Longest road recalculation cost
ImmutableSOC recalculates longest road via DFS on every build. The legal move generator only checks road *placement* legality (connectivity, empty edge), not the resulting road length. The full DFS runs only when the action is applied. No performance impact on legality checks.

### Empty dev card deck
Once the 25-card deck is empty, `BuyDevCardMove` becomes illegal. Terminal states can be reached while players still have valid moves.

### No explicit `currentPlayer` field
ImmutableSOC has no current-player tracking. The generator accepts `player: Int` as an explicit parameter. The RL environment must determine whose turn it is from the game context.

## Resolved Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| PreRoll dev cards | All dev cards legal | Standard Catan rules allow any dev card before rolling |
| Trade enumeration | Lazy parameter ranges | Avoids combinatorial explosion; single-move validation via `isLegal()` |
| VP card reveal | Auto-reveal on terminal check | `isTerminal()` detects 10+ VP (including hidden). No agent decision needed. |
| Discard order | Any order | Standard Catan — discards are independent and can happen in any sequence |
| Move grouping | `Map[Class[?], Seq[MOVE]]` | Reuses existing `ClassTag` instances. No parallel `MoveCategory` enum needed. |
| Phase tracking | Derived from `(state, turnMoves)` | Eliminates external state variable. Pure function. RL env manages the move log. |

## Implementation Effort

| Phase | Days | Description |
|-------|------|-------------|
| PhaseMachine | 1 | `TurnPhase` enum, `phase()` derivation function, terminal/winners detection |
| CachedBoard | 1 | Precomputed geometry (neighbors, edges, ports, hex-to-vertex) |
| SetupValidator | 2 | Slot ordering, empty vertex/edge, distance rule |
| BuildingValidator | 3 | Road connectivity + blocking, settlement distance rule, city upgrade, piece limits |
| TradeValidator | 3 | Port access (2:1/3:1/4:1), player trade ranges, lazy enumeration |
| RobberValidator | 2 | Hex options, victim enumeration per hex |
| DevCardValidator | 3 | Buy + 5 play types, buy-turn check, embedded robber (knight), largest army |
| TurnValidator | 2 | EndTurn timing, Discard amount/cards, phase gating |
| Composition | 2 | `LegalMoveGenerator` trait + `PerfectInfoLegalMoves` + `PublicInfoLegalMoves` |
| Unit Tests | 4 | Per-validator + integration with `BaseGameFixtures` |
| **Total** | **23** | |

No modifications to ImmutableSOC are required.

## Dependencies

The `soc/rules/` package depends on:
- `game.*` — `StateField`, `StateFields`, `InventorySet`, `GameMove`
- `soc.core.*` — `Resource`, `Vertex`, `Edge`, `SOCBoard` ops, all core state types
- `soc.base.*` — `BaseBoard`, `DevelopmentCards`, all move/result types
- `soc.base.state.*` — all base state wrappers
- `soc.core.state.*` — all core state wrappers

It does NOT depend on:
- `soc.base.actions.*` — downstream consumers of validated moves
- `soc.base.StateTransformer` — no delta application needed
- `soc.base.ImmutableGameBuilder` — no game building needed

## Board Geometry

The standard Catan board:
- 19 hexes (0-18): 18 resource hexes + 1 desert (hex 10)
- 54 vertices (0-53): 6 per hex, shared between adjacent hexes
- 72 edges: unique pairs of adjacent vertices
- 9 ports: MISC (3:1) or resource-specific (2:1), mapped to specific edges

Board data is immutable (Delta = Nothing on `BaseBoard`), so `CachedBoard` is computed once at construction time.
