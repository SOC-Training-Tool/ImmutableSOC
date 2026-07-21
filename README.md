# ImmutableSOC

A Scala library for applying Settlers of Catan game events to immutable, type-safe game state. ImmutableSOC is a **replay applier**: it takes a sequence of moves that have already been validated by an authoritative rules engine and deterministically transforms state. It supports both perfect information (server-side) and public information (player perspective) game modes.

> **Note:** This library does not enforce Catan rules. Move legality — resources, board placement, turn order, trade ratios, robber sequence, etc. — is the responsibility of the layer that produces the event stream. ImmutableSOC trusts its inputs and applies them.

## Installation

Add ImmutableSOC to your project:

**sbt:**
```scala
libraryDependencies += "io.github.soc-training-tool" %% "immutablesoc" % "0.0.7-SNAPSHOT"
```

**Maven:**
```xml
<dependency>
  <groupId>io.github.soc-training-tool</groupId>
  <artifactId>immutablesoc_2.13</artifactId>
  <version>0.0.7-SNAPSHOT</version>
</dependency>
```

## Quick Start

```scala
import soc.base.BaseGame._
import soc.base._
import soc.base.state._
import soc.core._
import soc.core.Resources._
import soc.core.state._
import soc.base.DevelopmentCards._

// Create a game instance
val game = perfectInfoGame

// Initialize game state (construct it directly)
val board = BaseBoard(
  List[Hex[Resource]](
    ResourceHex(WHEAT, 6), ResourceHex(ORE, 2), ResourceHex(SHEEP, 5),
    ResourceHex(ORE, 8), ResourceHex(WOOD, 4), ResourceHex(BRICK, 11),
    ResourceHex(SHEEP, 12), ResourceHex(ORE, 9), ResourceHex(SHEEP, 10),
    ResourceHex(BRICK, 8), Desert, ResourceHex(WHEAT, 3),
    ResourceHex(SHEEP, 9), ResourceHex(BRICK, 10), ResourceHex(WOOD, 3),
    ResourceHex(WOOD, 6), ResourceHex(WHEAT, 5), ResourceHex(WOOD, 4),
    ResourceHex(WHEAT, 11)
  ),
  ports = List(MISC, ORE, MISC, WHEAT, MISC, BRICK, WOOD, SHEEP, MISC)
)

val devDeck = List.fill(14)(KNIGHT) ++ List.fill(5)(POINT) ++
              List.fill(2)(MONOPOLY) ++ List.fill(2)(ROAD_BUILDER) ++
              List.fill(2)(YEAR_OF_PLENTY)

val initState = PerfectInfoState(
  robberLocation       = RobberLocation(10),
  privateInventories   = PrivateInventories(Map.empty),
  privateDevCardInv    = PrivateDevCardInv(Map.empty),
  developmentCardDeck  = DevelopmentCardDeck(devDeck),
  bank                 = Bank(InventorySet.fromMap(Map(WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19))),
  turn                 = Turn(0),
  playerPoints         = PlayerPoints(Map.empty),
  largestArmyPlayer    = LargestArmyPlayer(None),
  playerArmyCount      = PlayerArmyCount(Map.empty),
  vertexBuildingState  = VertexBuildingState(Map.empty),
  socRoadLengths       = SOCRoadLengths(Map.empty),
  socLongestRoadPlayer = SOCLongestRoadPlayer(None),
  board                = board,
  edgeBuildingState    = EdgeBuildingState(Map.empty),
  moveCount            = MoveCount(0),
  setupPlacementOrder  = SetupPlacementOrder(Nil)
)

// Apply a validated move (the library trusts the move and applies it)
  val newState = game.applyMove(RollDiceMoveResult(0, 5), initState)

// Query state
val points = newState.select[PlayerPoints]
```

## Game Variants

ImmutableSOC provides two game modes:

### PerfectInfoGame
Complete information variant where all game state is fully visible. Use this for:
- Server-side game logic
- AI training with complete information
- Game analysis and debugging
- Single-player simulations

All player resources and development cards are tracked precisely.

### PublicInfoGame
Partial information variant where only public information is visible. Use this for:
- Player-facing game state
- Multiplayer games with hidden information
- Realistic game simulations
- Converting server state to player perspectives

Only resource/card counts are visible, not the specific cards each player holds.

## Replay Applier Model

ImmutableSOC is intentionally a thin **replay applier**, not an authoritative rules engine. Think of it as a deterministic state reducer for a stream of validated Catan events.

- **Inputs are trusted.** The library assumes the move/event you pass in has already been validated by an authoritative layer (your game server, a rules engine, a referee, etc.). It will apply the move and produce new state without checking whether the move is legal.
- **State transformation is pure.** Actions have no side effects, no randomness, and no I/O. The same move applied to the same state always yields the same output and next state.
- **Derived scoring is computed.** The applier updates scores, inventories, buildings, and special awards based on the events it receives.
- **Validation lives elsewhere.** If you need to enforce rules — checking resources, board placement, turn order, trade ratios, robber sequence, development-card timing — build that layer on top and feed only validated events into ImmutableSOC.

This design makes the library useful for:
- Replaying completed games from a log
- Server-side state projection from validated commands
- AI training and analysis where an external environment produces legal moves
- Converting perfect-information server state into public or player-perspective views

## Core Concepts

### Immutable State
Game state is represented as an HList (heterogeneous list) containing all game components:
```scala
// Perfect Info State includes:
RobberLocation :: PrivateInventories :: PrivateDevelopmentCards ::
DevelopmentCardDeck :: Bank :: Turn :: PlayerPoints :: ... :: HNil
```

State elements can be accessed using `.select[Type]`:
```scala
val currentTurn = state.select[Turn]
val playerPoints = state.select[PlayerPoints]
```

### Moves
Moves are plain case-class values. The game instance is typed over a Scala 3 union of all supported moves, so you can pass any move directly:
```scala
val perfectMove: PerfectInfoMove = BuildRoadMove(0, edge)
val publicMove: PublicInfoMove   = TradeMove(0, 1, give, get)
```

### Applying Moves
Moves transform state immutably:
```scala
val newState = game.applyMove(move, currentState)
```

## Comprehensive Examples

### Game Initialization

Create a board with hexes and ports:
```scala
import soc.base._
import soc.core._
import game._

val board = BaseBoard(
  List[Hex[Resource]](
    ResourceHex(WHEAT, 6),
    ResourceHex(ORE, 2),
    ResourceHex(SHEEP, 5),
    ResourceHex(BRICK, 4),
    Desert,
    ResourceHex(WOOD, 10),
    // ... 13 more hexes for standard board
  ),
  ports = List(MISC, ORE, MISC, WHEAT, MISC, BRICK, WOOD, SHEEP, MISC)
)

val bank = InventorySet(Map(
  WOOD -> 19,
  BRICK -> 19,
  SHEEP -> 19,
  WHEAT -> 19,
  ORE -> 19
))

val devDeck = List.fill(14)(KNIGHT) ++
              List.fill(5)(POINT) ++
              List.fill(2)(MONOPOLY) ++
              List.fill(2)(ROAD_BUILDER) ++
              List.fill(2)(YEAR_OF_PLENTY)

val robberLocation = RobberLocation(7) // Start on desert

// Initialize Perfect Info Game by constructing the state directly
val initState = PerfectInfoState(
  robberLocation       = robberLocation,
  privateInventories   = PrivateInventories(Map.empty),
  privateDevCardInv    = PrivateDevCardInv(Map.empty),
  developmentCardDeck  = DevelopmentCardDeck(devDeck),
  bank                 = Bank(bank),
  turn                 = Turn(0),
  playerPoints         = PlayerPoints(Map.empty),
  largestArmyPlayer    = LargestArmyPlayer(None),
  playerArmyCount      = PlayerArmyCount(Map.empty),
  vertexBuildingState  = VertexBuildingState(Map.empty),
  socRoadLengths       = SOCRoadLengths(Map.empty),
  socLongestRoadPlayer = SOCLongestRoadPlayer(None),
  board                = board,
  edgeBuildingState    = EdgeBuildingState(Map.empty),
  moveCount            = MoveCount(0),
  setupPlacementOrder  = SetupPlacementOrder(Nil)
)

// Or initialize Public Info Game
val publicState = PublicInfoState(
  robberLocation          = robberLocation,
  publicInventories       = PublicInventories(Map.empty),
  publicDevCardInv        = PublicDevCardInv(Map.empty),
  developmentCardDeckSize = DevelopmentCardDeckSize(devDeck.size),
  bank                    = Bank(bank),
  turn                    = Turn(0),
  playerPoints            = PlayerPoints(Map.empty),
  largestArmyPlayer       = LargestArmyPlayer(None),
  playerArmyCount         = PlayerArmyCount(Map.empty),
  vertexBuildingState     = VertexBuildingState(Map.empty),
  socRoadLengths          = SOCRoadLengths(Map.empty),
  socLongestRoadPlayer    = SOCLongestRoadPlayer(None),
  board                   = board,
  edgeBuildingState       = EdgeBuildingState(Map.empty),
  moveCount               = MoveCount(0),
  setupPlacementOrder     = SetupPlacementOrder(Nil)
)
```

### Making Moves

#### Initial Placement
```scala
// First settlement and road for player 0
val state1 = game.applyMove(
  InitialPlacementMove(Vertex(33), Edge(4, 33), 0),
  initState
)

// Second settlement and road for player 0
val state2 = game.applyMove(
  InitialPlacementMove(Vertex(15), Edge(15, 38), 0),
  state1
)
```

#### Turn Actions
```scala
// Roll dice (result is provided by the authoritative layer)
val afterRoll = game.applyMove(RollDiceMoveResult(0, 8), currentState)

// End turn
val afterTurn = game.applyMove(EndTurnMove(0), afterRoll)
```

#### Building
```scala
// Build a road
val withRoad = game.applyMove(BuildRoadMove(0, Edge(48, 49)), currentState)

// Build a settlement
val withSettlement = game.applyMove(BuildSettlementMove(0, Vertex(48)), withRoad)

// Build a city (upgrade settlement)
val withCity = game.applyMove(BuildCityMove(0, Vertex(33)), withSettlement)
```

#### Trading
```scala
// Port trade (4:1 generic port - 4 wheat for 1 wood)
val afterPortTrade = game.applyMove(
  PortTradeMove(0, ResourceSet(wh = 4), ResourceSet(wo = 1)),
  currentState
)

// Player-to-player trade (player 1 trades with player 3)
val afterPlayerTrade = game.applyMove(
  TradeMove(1, 3, ResourceSet(WOOD), ResourceSet(WHEAT, SHEEP)),
  currentState
)
```

#### Development Cards
```scala
// Buy a development card (perfect info shows which card)
val afterBuy = game.applyMove(
  PerfectInfoBuyDevelopmentCardMoveResult(0, KNIGHT),
  currentState
)

// Buy a development card (public info hides the card)
val afterPublicBuy = game.applyMove(
  BuyDevelopmentCardMoveResult[DevelopmentCard](0, None),
  publicState
)

// Play Knight card
val afterKnight = game.applyMove(
  PerfectInfoPlayKnightResult(
    PerfectInfoRobberMoveResult(0, 13, Some(PlayerSteal(1, BRICK)))
  ),
  currentState
)

// Play Monopoly
val afterMonopoly = game.applyMove(
  PlayMonopolyMoveResult(0, SHEEP, Map(1 -> 3, 2 -> 2, 3 -> 1)),
  publicState
)

// Play Year of Plenty
val afterYOP = game.applyMove(
  PlayYearOfPlentyMove(0, WHEAT, WHEAT),
  publicState
)

// Play Road Builder
val afterRoadBuilder = game.applyMove(
  PlayRoadBuilderMove(0, Edge(10, 33), Some(Edge(33, 56))),
  publicState
)

// Play Victory Point card
val afterPoint = game.applyMove(PlayPointMove(0), publicState)
```

#### Robber
```scala
// Move robber and steal (perfect info shows exact card stolen)
val afterRobber = game.applyMove(
  PerfectInfoRobberMoveResult(0, 9, Some(PlayerSteal(1, BRICK))),
  currentState
)

// Move robber and steal (public info shows count only)
val afterPublicRobber = game.applyMove(
  RobberMoveResult[Resource](0, 9, Some(PlayerSteal(1, Some(WHEAT)))),
  publicState
)
```

#### Discard
```scala
// Discard cards when rolling 7 with >7 cards
val afterDiscard = game.applyMove(
  DiscardMove(1, ResourceSet(or = 3, br = 1)),
  currentState
)
```

### ResourceSet Notation

Resources can be specified in two ways:

```scala
// Named parameters (recommended)
ResourceSet(wo = 2, br = 1)           // 2 wood, 1 brick
ResourceSet(wh = 4)                    // 4 wheat
ResourceSet(sh = 1, wh = 1, or = 1)   // 1 sheep, 1 wheat, 1 ore

// Explicit listing
ResourceSet(WOOD, WOOD, BRICK)        // 2 wood, 1 brick
ResourceSet(WHEAT, WHEAT, WHEAT, WHEAT) // 4 wheat

// Parameter key mapping:
// wo = WOOD
// br = BRICK
// sh = SHEEP
// wh = WHEAT
// or = ORE
```

### Querying Game State

Extract information from state using `.select[Type]`:

```scala
// Player points
val points: PlayerPoints = state.select[PlayerPoints]
val player0Points = points.points.getOrElse(0, 0)

// Current turn
val turn: Turn = state.select[Turn]
val turnNumber = turn.t

// Bank inventory
val bank: Bank[Resource] = state.select[Bank[Resource]]
val wheatInBank = bank.b.getAmount(WHEAT)

// Player inventories (Perfect Info)
val inventories: PrivateInventories[Resource] =
  state.select[PrivateInventories[Resource]]
val player0Wood = inventories.players(0).getAmount(WOOD)

// Player inventories (Public Info)
val publicInv: PublicInventories[Resource] =
  publicState.select[PublicInventories[Resource]]
val player1CardCount = publicInv.numCards(publicState, 1)

// Building placements
val settlements: VertexBuildingState[BaseVertexBuilding] =
  state.select[VertexBuildingState[BaseVertexBuilding]]

val roads: EdgeBuildingState[BaseEdgeBuilding] =
  state.select[EdgeBuildingState[BaseEdgeBuilding]]

// Robber location
val robber: RobberLocation = state.select[RobberLocation]
val robberHex = robber.hex

// Longest road
val longestRoad: SOCLongestRoadPlayer =
  state.select[SOCLongestRoadPlayer]

// Largest army
val largestArmy: LargestArmyPlayer =
  state.select[LargestArmyPlayer]
```

### Complete Workflow Example

```scala
import soc.base.BaseGame._
import soc.base._
import soc.base.state._
import soc.core._
import soc.core.Resources._
import soc.core.state._
import soc.base.DevelopmentCards._

// 1. Setup
val game = perfectInfoGame
val board = BaseBoard(
  List[Hex[Resource]](
    ResourceHex(WHEAT, 6), ResourceHex(ORE, 2), ResourceHex(SHEEP, 5),
    ResourceHex(ORE, 8), ResourceHex(WOOD, 4), ResourceHex(BRICK, 11),
    ResourceHex(SHEEP, 12), ResourceHex(ORE, 9), ResourceHex(SHEEP, 10),
    ResourceHex(BRICK, 8), Desert, ResourceHex(WHEAT, 3),
    ResourceHex(SHEEP, 9), ResourceHex(BRICK, 10), ResourceHex(WOOD, 3),
    ResourceHex(WOOD, 6), ResourceHex(WHEAT, 5), ResourceHex(WOOD, 4),
    ResourceHex(WHEAT, 11)
  ),
  ports = List(MISC, ORE, MISC, WHEAT, MISC, BRICK, WOOD, SHEEP, MISC)
)
val bank = InventorySet.fromMap(Map(WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19))
val devDeck = List.fill(14)(KNIGHT) ++ List.fill(5)(POINT) ++
              List.fill(2)(MONOPOLY) ++ List.fill(2)(ROAD_BUILDER) ++
              List.fill(2)(YEAR_OF_PLENTY)
val robberLocation = RobberLocation(10)

var state = PerfectInfoState(
  robberLocation       = robberLocation,
  privateInventories   = PrivateInventories(Map.empty),
  privateDevCardInv    = PrivateDevCardInv(Map.empty),
  developmentCardDeck  = DevelopmentCardDeck(devDeck),
  bank                 = Bank(bank),
  turn                 = Turn(0),
  playerPoints         = PlayerPoints(Map.empty),
  largestArmyPlayer    = LargestArmyPlayer(None),
  playerArmyCount      = PlayerArmyCount(Map.empty),
  vertexBuildingState  = VertexBuildingState(Map.empty),
  socRoadLengths       = SOCRoadLengths(Map.empty),
  socLongestRoadPlayer = SOCLongestRoadPlayer(None),
  board                = board,
  edgeBuildingState    = EdgeBuildingState(Map.empty),
  moveCount            = MoveCount(0),
  setupPlacementOrder  = SetupPlacementOrder(Nil)
)

// 2. Initial placement (4 players, 2 settlements each)
state = game.applyMove(InitialPlacementMove(Vertex(33), Edge(4, 33), 0), state)
// ... repeat for all players

// 3. Start normal play
state = game.applyMove(RollDiceMoveResult(0, 8), state)

// 4. Player actions
state = game.applyMove(BuildRoadMove(0, Edge(48, 49)), state)
state = game.applyMove(
  PortTradeMove(0, ResourceSet(WOOD, WOOD, WOOD, WOOD), ResourceSet(BRICK)),
  state
)
state = game.applyMove(BuildSettlementMove(0, Vertex(48)), state)

// 5. End turn
state = game.applyMove(EndTurnMove(0), state)

// 6. Check game state
val currentPoints = state.select[PlayerPoints]
val currentTurn = state.select[Turn]
println(s"Player 0 points: ${currentPoints.points.getOrElse(0, 0)}")
println(s"Turn: ${currentTurn.t}")
```

## Future Enhancements

The following improvements are natural extensions of the replay-applier design. They keep the core library focused on deterministic state transformation while making it more useful for replay, projection, and integration with an authoritative rules engine.

### Replay applier hardening

- **Event-consistency mode.** Optionally return `Either[InconsistentEvent, State]` when a move contradicts the current state (for example, a robber steal declaring a resource the victim does not have, or a monopoly declaring losses that do not match the victims' hands). This is useful for replay validators and test harnesses without turning the library into a rules engine.
- **Graceful edge-case handling.** Avoid runtime exceptions on empty decks, malformed board inputs, or missing players; document the preconditions each action expects.
- **Deterministic setup replay.** Fix second-round initial-resource grants by tracking setup placement order explicitly, instead of inferring it from `MoveCount`.
- **Consistent special-card scoring.** Resolve whether victory-point development cards count immediately on purchase or only when explicitly revealed, and make the perfect-info and public-info interpretations agree.

### Derived scoring and state computation

- **Longest road integration.** Wire `SOCRoadLengths` and `SOCLongestRoadPlayer` updates into road-building and settlement-building actions so the applier computes the 2-point award automatically during replay.
- **Largest army integration.** Update `PlayerArmyCount` and `LargestArmyPlayer` when knight cards are played.
- **Game-over detection.** Add a `winner(state)` query (or `GameStatus` field) that reports when a player has reached 10 victory points, while still allowing further moves to be applied if desired.

### Projection and multi-perspective replay

- **Perfect-to-public projection.** Provide `PublicInfoState.fromPerfect(perfectState, viewerId)` and prove that replaying projected public moves produces the same public state as projecting the perfect state after every move.
- **Player-perspective state.** Add an own-hand perspective that hides other players' cards while revealing the viewer's exact hand.

### Authoritative layer (separate from this library)

- A companion rules engine could sit above ImmutableSOC and decide which events are legal before they are applied. It would own validation such as:
  - resource and building-piece availability,
  - board placement rules (distance, connectivity, water vertices),
  - turn order and phase gating,
  - port-trade ratios and port access,
  - robber sequence (discard half, valid hex/victim, no self-steal),
  - development-card timing (one per turn, cannot play on purchase turn).
- The authoritative layer would produce a validated event stream; ImmutableSOC would remain the pure state-reducer that consumes it.

## CI/CD

This project uses GitHub Actions for continuous integration and publishing.

### Continuous Integration

The CI workflow automatically runs tests and generates coverage reports on every push and pull request.

### Publishing to GitHub Packages

Releases are automatically published to GitHub Packages using **conventional commits** for semantic versioning.

#### How It Works

When you merge to `master`, the release workflow:
1. Analyzes commit messages since the last tag
2. Determines version bump based on conventional commits:
   - `fix:` commits → **patch** version bump
   - `feat:` commits → **minor** version bump
   - `BREAKING CHANGE:` or `feat!:` → **major** version bump
3. Updates `build.sbt` with the new version
4. Creates and pushes a git tag (e.g., `v0.0.8`)
5. The publish workflow then:
   - Runs tests
   - Publishes to GitHub Packages using `GITHUB_TOKEN`
   - Creates a GitHub release

#### Conventional Commit Format

Use conventional commit messages in your PRs:

```bash
# Patch release (bug fixes)
git commit -m "fix: correct settlement placement validation"

# Minor release (new features)
git commit -m "feat: add support for 5-6 player expansion"

# Major release (breaking changes)
git commit -m "feat!: redesign game state API"
# or
git commit -m "feat: new API

BREAKING CHANGE: GameState structure has changed"
```

#### Skipping Releases

To prevent a release when merging to master, include `[skip ci]` or `[ci skip]` in your commit message:

```bash
git commit -m "docs: update README [skip ci]"
```

#### Manual Release

You can also manually create a release by pushing a tag:

```bash
git tag v0.0.7
git push origin v0.0.7
```

#### Required GitHub Secrets

The following secret must be configured in your repository settings:

- `GITHUB_TOKEN` - Provided automatically by GitHub Actions; used to publish to GitHub Packages and create releases.
- `PAT_TOKEN` - A personal access token with `contents:write` scope, used by the release workflow to push tags.

### Next Steps: Codecov Integration (Optional)

To enable Codecov for visual coverage tracking and PR comments:

1. Go to [codecov.io](https://codecov.io) and sign in with GitHub
2. Add the `SOC-Training-Tool/ImmutableSOC` repository
3. No token needed for public repositories - the existing workflow will automatically start uploading coverage
4. (Optional) Add a coverage badge to this README from the Codecov dashboard 
