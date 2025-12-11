# ImmutableSOC

A Scala library for managing Settlers of Catan games with immutable state. ImmutableSOC provides a type-safe, functional implementation of Catan game rules, supporting both perfect information (server-side) and public information (player perspective) game modes.

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
import soc.core._
import game._
import shapeless._

// Create a game instance
val game = PerfectInfoGame.game

// Initialize game state
val board = BaseBoard(hexes, ports)
val initState = ImmutableGame.initialize[PerfectInfoState]
  .replaceAll(board :: Bank(bank) :: DevelopmentCardDeck(devDeck) :: robberLocation :: HNil)

// Apply a move
val newState = game.applyMove(
  Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 5)),
  initState
)

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
All moves are wrapped in Coproducts (type-safe unions):
```scala
Coproduct[PerfectInfoMoves](BuildRoadMove(playerId, edge))
Coproduct[PublicInfoMoves](TradeMove(player, partner, give, get))
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
              List.fill(5)(Point) ++
              List.fill(2)(Monopoly) ++
              List.fill(2)(RoadBuilder) ++
              List.fill(2)(YearOfPlenty)

val robberLocation = RobberLocation(7) // Start on desert

// Initialize Perfect Info Game
val initState = ImmutableGame.initialize[PerfectInfoState]
  .replaceAll(
    board :: Bank(bank) :: DevelopmentCardDeck(devDeck) ::
    robberLocation :: HNil
  )

// Or initialize Public Info Game
val publicState = ImmutableGame.initialize[PublicInfoState]
  .replaceAll(
    board :: Bank(bank) :: DevelopmentCardDeckSize(devDeck.size) ::
    robberLocation :: HNil
  )
```

### Making Moves

#### Initial Placement
```scala
import shapeless._

// First settlement and road for player 0
val state1 = game.applyMove(
  Coproduct[PerfectInfoMoves](
    InitialPlacementMove(Vertex(33), Edge(4, 33), isFirstPlacement = true, 0)
  ),
  initState
)

// Second settlement and road for player 0
val state2 = game.applyMove(
  Coproduct[PerfectInfoMoves](
    InitialPlacementMove(Vertex(15), Edge(15, 38), isFirstPlacement = false, 0)
  ),
  state1
)
```

#### Turn Actions
```scala
// Roll dice
val afterRoll = game.applyMove(
  Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 8)),
  currentState
)

// End turn
val afterTurn = game.applyMove(
  Coproduct[PerfectInfoMoves](EndTurnMove(0)),
  afterRoll
)
```

#### Building
```scala
// Build a road
val withRoad = game.applyMove(
  Coproduct[PerfectInfoMoves](BuildRoadMove(0, Edge(48, 49))),
  currentState
)

// Build a settlement
val withSettlement = game.applyMove(
  Coproduct[PerfectInfoMoves](BuildSettlementMove(0, Vertex(48))),
  withRoad
)

// Build a city (upgrade settlement)
val withCity = game.applyMove(
  Coproduct[PerfectInfoMoves](BuildCityMove(0, Vertex(33))),
  withSettlement
)
```

#### Trading
```scala
// Port trade (4:1 generic port - 4 wheat for 1 wood)
val afterPortTrade = game.applyMove(
  Coproduct[PerfectInfoMoves](
    PortTradeMove(0, ResourceSet(wh = 4), ResourceSet(wo = 1))
  ),
  currentState
)

// Player-to-player trade (player 1 trades with player 3)
val afterPlayerTrade = game.applyMove(
  Coproduct[PublicInfoMoves](
    TradeMove(1, 3, ResourceSet(WOOD), ResourceSet(WHEAT, SHEEP))
  ),
  currentState
)
```

#### Development Cards
```scala
// Buy a development card (perfect info shows which card)
val afterBuy = game.applyMove(
  Coproduct[PerfectInfoMoves](
    PerfectInfoBuyDevelopmentCardMoveResult(0, KNIGHT)
  ),
  currentState
)

// Buy a development card (public info hides the card)
val afterPublicBuy = game.applyMove(
  Coproduct[PublicInfoMoves](
    BuyDevelopmentCardMoveResult[DevelopmentCard](0, None)
  ),
  publicState
)

// Play Knight card
val afterKnight = game.applyMove(
  Coproduct[PerfectInfoMoves](
    PerfectInfoPlayKnightResult(
      PerfectInfoRobberMoveResult(0, 13, Some(PlayerSteal(1, BRICK)))
    )
  ),
  currentState
)

// Play Monopoly
val afterMonopoly = game.applyMove(
  Coproduct[PublicInfoMoves](
    PlayMonopolyMoveResult(0, SHEEP, Map(1 -> 3, 2 -> 2, 3 -> 1))
  ),
  publicState
)

// Play Year of Plenty
val afterYOP = game.applyMove(
  Coproduct[PublicInfoMoves](
    PlayYearOfPlentyMove(0, WHEAT, WHEAT)
  ),
  publicState
)

// Play Road Builder
val afterRoadBuilder = game.applyMove(
  Coproduct[PublicInfoMoves](
    PlayRoadBuilderMove(0, Edge(10, 33), Edge(33, 56))
  ),
  publicState
)

// Play Victory Point card
val afterPoint = game.applyMove(
  Coproduct[PublicInfoMoves](PlayPointMove(0)),
  publicState
)
```

#### Robber
```scala
// Move robber and steal (perfect info shows exact card stolen)
val afterRobber = game.applyMove(
  Coproduct[PerfectInfoMoves](
    PerfectInfoRobberMoveResult(0, 9, Some(PlayerSteal(1, BRICK)))
  ),
  currentState
)

// Move robber and steal (public info shows count only)
val afterPublicRobber = game.applyMove(
  Coproduct[PublicInfoMoves](
    RobberMoveResult[Resource](0, 9, Some(PlayerSteal(1, Some(WHEAT))))
  ),
  publicState
)
```

#### Discard
```scala
// Discard cards when rolling 7 with >7 cards
val afterDiscard = game.applyMove(
  Coproduct[PerfectInfoMoves](
    DiscardMove(1, ResourceSet(or = 3, br = 1))
  ),
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
import soc.core._
import game._
import shapeless._

// 1. Setup
val game = PerfectInfoGame.game
val board = BaseBoard(standardHexes, standardPorts)
val bank = InventorySet(Map(WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19))
val devDeck = standardDevelopmentCardDeck
val robberLocation = RobberLocation(7)

var state = ImmutableGame.initialize[PerfectInfoState]
  .replaceAll(board :: Bank(bank) :: DevelopmentCardDeck(devDeck) :: robberLocation :: HNil)

// 2. Initial placement (4 players, 2 settlements each)
state = game.applyMove(
  Coproduct[PerfectInfoMoves](InitialPlacementMove(Vertex(33), Edge(4, 33), true, 0)),
  state
)
// ... repeat for all players

// 3. Start normal play
state = game.applyMove(
  Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 8)),
  state
)

// 4. Player actions
state = game.applyMove(
  Coproduct[PerfectInfoMoves](BuildRoadMove(0, Edge(48, 49))),
  state
)

state = game.applyMove(
  Coproduct[PerfectInfoMoves](
    PortTradeMove(0, ResourceSet(WOOD, WOOD, WOOD, WOOD), ResourceSet(BRICK))
  ),
  state
)

state = game.applyMove(
  Coproduct[PerfectInfoMoves](BuildSettlementMove(0, Vertex(48))),
  state
)

// 5. End turn
state = game.applyMove(
  Coproduct[PerfectInfoMoves](EndTurnMove(0)),
  state
)

// 6. Check game state
val currentPoints = state.select[PlayerPoints]
val currentTurn = state.select[Turn]
println(s"Player 0 points: ${currentPoints.points.getOrElse(0, 0)}")
println(s"Turn: ${currentTurn.t}")
```

## CI/CD

This project uses GitHub Actions for continuous integration and publishing.

### Continuous Integration

The CI workflow automatically runs tests and generates coverage reports on every push and pull request.

### Publishing to Maven Central

Releases are automatically published to Maven Central using **conventional commits** for semantic versioning.

#### How It Works

When you merge to `master`, the release workflow:
1. Analyzes commit messages since the last tag
2. Determines version bump based on conventional commits:
   - `fix:` commits → **patch** version bump (0.0.7 → 0.0.8)
   - `feat:` commits → **minor** version bump (0.0.7 → 0.1.0)
   - `BREAKING CHANGE:` or `feat!:` → **major** version bump (0.0.7 → 1.0.0)
3. Updates `build.sbt` with the new version
4. Creates and pushes a git tag (e.g., `v0.0.8`)
5. The publish workflow then:
   - Runs tests
   - Signs artifacts with your GPG key
   - Publishes to Maven Central via Sonatype
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

The following secrets must be configured in your repository settings:

- `SONATYPE_USERNAME` - Your Sonatype JIRA username
- `SONATYPE_PASSWORD` - Your Sonatype JIRA password
- `PGP_SECRET` - Your GPG private key (base64-encoded)
- `PGP_PASSPHRASE` - Your GPG key passphrase
- `PGP_KEY_ID` - Your GPG key ID in hex format

**To encode your GPG key:**
```bash
# Export your private key
gpg --armor --export-secret-keys YOUR_KEY_ID > private-key.asc

# Base64 encode it
cat private-key.asc | base64 > private-key.base64

# Copy the contents of private-key.base64 to the PGP_SECRET secret
```

**To get your key ID:**
```bash
gpg --list-secret-keys --keyid-format LONG
# Use the long hex ID after 'sec   rsa4096/'
```

### Next Steps: Codecov Integration (Optional)

To enable Codecov for visual coverage tracking and PR comments:

1. Go to [codecov.io](https://codecov.io) and sign in with GitHub
2. Add the `SOC-Training-Tool/ImmutableSOC` repository
3. No token needed for public repositories - the existing workflow will automatically start uploading coverage
4. (Optional) Add a coverage badge to this README from the Codecov dashboard 
