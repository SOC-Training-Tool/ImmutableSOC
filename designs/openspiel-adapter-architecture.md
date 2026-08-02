# OpenSpiel Adapter Architecture

**Status:** Planned (decisions resolved via grilling, 2026-08-02)
**Depends on:** `designs/legal-move-generator-plan.md` (completed, commit `ddc61a1`)

## Overview

This document describes the Python adapter layer that connects ImmutableSOC (Scala 3) to OpenSpiel's Python RL ecosystem, enabling reinforcement learning agents to play Catan. The adapter is bridge-agnostic — the JVM-Python transport mechanism (jpype initially, Py4J or gRPC later) is isolated behind a Python interface.

The Python adapter and OpenSpiel wrapper live in a **separate repository** from ImmutableSOC. The only coupling is a runtime dependency on the compiled ImmutableSOC JAR. ImmutableSOC remains a pure Scala library with zero Python awareness.

```
┌──────────────────────────────────────────────────────────┐
│                    Adapter Repo (Python)                  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │              RL Training Loop                       │  │
│  │  (OpenSpiel algorithms, Gymnasium, SB3, RLlib...)  │  │
│  ├────────────────────────────────────────────────────┤  │
│  │          OpenSpiel Python Game Wrapper              │  │
│  │     pyspiel.Game / pyspiel.State subclasses         │  │
│  │              (~100 lines, thin shim)                │  │
│  ├────────────────────────────────────────────────────┤  │
│  │             Bridge-Agnostic Adapter                  │  │
│  │  CatanAdapter — owns all conversion logic            │  │
│  │  ├── Observation tensor encoding   (727 floats)     │  │
│  │  ├── Action ID ↔ Move mapping      (0..1023)        │  │
│  │  ├── Reward computation             (VP + efficiency)│  │
│  │  ├── Phase auto-advance loop        (decision-point)│  │
│  │  └── Masked-full-info support        (perfect/public)│  │
│  ├────────────────────────────────────────────────────┤  │
│  │            JVM Bridge Interface                      │  │
│  │  Abstract base class                                 │  │
│  │  ┌──────────┐  ┌────────┐  ┌──────────────────┐    │  │
│  │  │  jpype    │  │  Py4J   │  │ subprocess JSON  │    │  │
│  │  │ (initial) │  │(future) │  │    (future)       │    │  │
│  │  └──────────┘  └────────┘  └──────────────────┘    │  │
│  └────────────────────────────────────────────────────┘  │
│                          │                                │
│            loads via jpype (runtime .jar)                 │
└──────────────────────────┼────────────────────────────────┘
                           │
┌──────────────────────────┼────────────────────────────────┐
│         ImmutableSOC Repo (Scala)                          │
│                          ▼                                │
│  │    ImmutableSOC + LegalMoveGenerator                   │
│  │    └── PerfectInfoState, PublicInfoState              │
│  │    └── applyMove(move, state) → (output, newState)    │
│  │    └── legalMoves(state, player, turnMoves)            │
│                                                          │
│  No Python files. No jpype dependency.                    │
│  Publishes JAR to GitHub Packages.                        │
└──────────────────────────────────────────────────────────┘
```

## Component Design

### 1. JVM Bridge Interface (Python abstract base class)

```python
class JVMBridge(ABC):
    """Bridge-agnostic interface to the Scala engine."""

    @abstractmethod
    def new_initial_state(self, num_players: int, seed: int, perfect_info: bool) -> Any:
        """Create a fresh game state. Returns an opaque jpype/Scala reference."""

    @abstractmethod
    def apply_move(self, state: Any, move: Any) -> tuple[Any, Any]:
        """Apply a typed move to state. Returns (output, new_state)."""

    @abstractmethod
    def legal_moves(self, state: Any, player: int, turn_moves: list) -> list[Any]:
        """All legal typed moves for the player."""

    @abstractmethod
    def legal_moves_grouped(self, state: Any, player: int, turn_moves: list) -> dict:
        """Legal moves grouped by class type."""

    @abstractmethod
    def is_legal(self, state: Any, player: int, turn_moves: list, move: Any) -> bool:
        """Validate a single specific move."""

    @abstractmethod
    def is_terminal(self, state: Any) -> bool:
        """Game-over check."""

    @abstractmethod
    def winners(self, state: Any) -> set[int] | None:
        """Winning players, or None if not terminal."""

    @abstractmethod
    def get_road_builder_roads(self, state: Any, player: int) -> list[Any]:
        """Valid free-road edges for active road builder card."""

    @abstractmethod
    def get_phase(self, state: Any, turn_moves: list) -> str:
        """Current game phase (Setup, PreRoll, MainPlay, etc.)."""

    @abstractmethod
    def clone_state(self, state: Any) -> Any:
        """Deep copy of state (for MCTS branching)."""
```

The **jpype implementation** imports `.class` files directly:
```python
class JpypeBridge(JVMBridge):
    def __init__(self):
        import jpype
        from jpype.types import JString
        jpype.startJVM()
        self._game = jpype.JClass("soc.base.BaseGame$").MODULE$
        self._lm = jpype.JClass("soc.rules.LegalMoveGenerator$").MODULE$
        self._builder = jpype.JClass("soc.base.ImmutableGameBuilder")

    def apply_move(self, state, move):
        return self._game.perfectInfoGame().applyMoveAny(move, state)

    def legal_moves(self, state, player, turn_moves):
        j_turn_moves = jpype.java.util.ArrayList(turn_moves)
        return list(self._lm.PerfectInfoLegalMoves().legalMoves(
            state, player, j_turn_moves))
```

### 2. Action ID Encoding

`NumDistinctActions = 1024`. Exact-fit ranges, no wasted IDs per block:

| ID Range | Move Type | Slots | Mapping |
|----------|-----------|:---:|---------|
| `0` | `EndTurnMove` | 1 | Direct |
| `1–11` | Dice roll outcomes | 11 | `id = 1 + (roll − 2)` |
| `12–83` | `BuildRoadMove` | 72 | `id = 12 + edge_index` (0..71) |
| `84–137` | `BuildSettlementMove` | 54 | `id = 84 + vertex_node` (0..53) |
| `138–191` | `BuildCityMove` | 54 | `id = 138 + vertex_node` (0..53) |
| `192–216` | `PortTradeMove` | 25 | `id = 192 + give_res*5 + get_res` |
| `217–266` | `TradeMove` | 50 | `id = 217 + partner*25 + give_res*5 + get_res` (capped) |
| `267–522` | `DiscardMove` | 256 | Combinatorial index into subset of floor(n/2) from available |
| `523–541` | Robber placement + steal | 19 | `id = 523 + hex_node (excluding current robber)` |
| `542` | `BuyDevelopmentCardMove` | 1 | Direct |
| `543–561` | `PlayKnightMoveResult` | 19 | `id = 543 + robber_placement (as 523–541)` |
| `562–566` | `PlayMonopolyMoveResult` | 5 | `id = 562 + resource_type` (0=wood .. 4=ore) |
| `567–591` | `PlayYearOfPlentyMove` | 25 | `id = 567 + res1*5 + res2` |
| `592–663` | `PlayRoadBuilderMove` (step 1) | 72 | `id = 592 + edge_index` (0..71) |
| `664` | `PlayPointMove` | 1 | Direct |
| `665–826` | `InitialPlacementMove` | 162 | `id = 665 + vertex*3 + adjacent_edge_index` |
| `827–1023` | (reserved/padding) | 197 | Zeroed in legal action mask |

The `CatanAdapter` owns a bidirectional `ActionEncoder`:

```python
class ActionEncoder:
    """Maps between integer action IDs and typed Scala move objects."""
    
    START_RANGES = {
        MoveType.END_TURN: 0,
        MoveType.ROLL_DICE: 1,
        MoveType.BUILD_ROAD: 12,
        MoveType.BUILD_SETTLEMENT: 84,
        MoveType.BUILD_CITY: 138,
        MoveType.PORT_TRADE: 192,
        MoveType.TRADE: 217,
        MoveType.DISCARD: 267,
        MoveType.ROBBER: 523,
        MoveType.BUY_DEV_CARD: 542,
        MoveType.PLAY_KNIGHT: 543,
        MoveType.PLAY_MONOPOLY: 562,
        MoveType.PLAY_YEAR_OF_PLENTY: 567,
        MoveType.PLAY_ROAD_BUILDER: 592,
        MoveType.PLAY_POINT: 664,
        MoveType.INITIAL_PLACEMENT: 665,
    }

    def encode(self, move: Any) -> int: ...
    def decode(self, state: Any, player: int, action_id: int) -> Any: ...
```

The SKIP_ROAD_BUILDER sentinel (for choosing to place only the first free road) occupies ID `663` co-located with the road builder range in the mask but mapped to a special-case internal move.

### 3. Observation Tensor Layout

**727 float32 channels.** Channel ordering:

#### Board State (all unmasked)

| Offset | Channels | Description |
|--------|:--:|------|
| 0–132 | 133 | 19 hexes × 7 one-hot channels (wood/brick/sheep/wheat/ore/desert/robber) |
| 133–348 | 216 | 4 players × 54 vertices: one-hot (empty/settlement/city) per vertex-per-player |
| 349–636 | 288 | 4 players × 72 edges: binary (road yes/no) per edge-per-player |

#### Per-Player State (masked for non-self players)

| Offset | Channels | Description | Masked? |
|--------|:--:|------|:--:|
| 637–656 | 20 | 4 players × 5 resources: card count per resource type, normalized by total | **Yes** |
| 657–660 | 4 | 4 players × 1: total card count (opponent inference channel) | No |
| 661–680 | 20 | 4 players × 5 dev card types: count per type | **Yes** |
| 681–684 | 4 | 4 players × 1: total unplayed dev cards | No |
| 685–688 | 4 | 4 players × 1: victory points (0.0–1.0 normalized) | No |
| 689–692 | 4 | 4 players × 1: longest road length (0.0–1.0 normalized) | No |
| 693–696 | 4 | 4 players × 1: knights played / army count (0.0–1.0 normalized) | No |
| 697–720 | 24 | 4 players × 6 port types: binary access | No |

#### Game Metadata (all unmasked)

| Offset | Channels | Description |
|--------|:--:|------|
| 721 | 1 | Turn number (0.0–1.0 normalized by max 300) |
| 722 | 1 | Current player (0.0–1.0 normalized by num_players) |
| 723 | 1 | Dice result (0.0–1.0 normalized by 12) |
| 724 | 1 | Phase one-hot index (0=Setup, 1=PreRoll, 2=Discard, 3=Robber, 4=MainPlay, 5=GameOver) |
| 725 | 1 | Dev card deck remaining (0.0–1.0 normalized by 25) |
| 726 | 1 | Move count (0.0–1.0 normalized by max 600) |

**Masking logic**: When `mask_mode=True` and encoding for `player_id=p`:
- Resource channels at offset 637 + (p*5) through 637 + (p*5)+4 → exact counts (unmasked)
- Resource channels for other players → set to 0.0
- Dev card channels at offset 661 + (p*5) through 661 + (p*5)+4 → exact counts (unmasked)
- Dev card channels for other players → set to 0.0
- Count channels (657–660, 681–684) → always unmasked (public table information)
- All other channels → unmasked (shared board state + metadata)

### 4. Reward Function

**VP-difference shaping + terminal efficiency bonus.**

```python
SHAPING_COEFF = 0.01
EFFICIENCY_COEFF = 0.3
MAX_GAME_LENGTH = 300

def compute_reward(
    prev_points: dict[int, float],
    cur_points: dict[int, float],
    cur_player: int,
    is_terminal: bool,
    winners: set[int] | None,
    move_count: int,
) -> float:
    """Per-step reward for current player."""

    # 1. VP difference shaping
    vp_delta = cur_points[cur_player] - prev_points[cur_player]
    reward = SHAPING_COEFF * vp_delta

    # 2. Terminal reward
    if is_terminal and winners is not None:
        if cur_player in winners:
            efficiency = EFFICIENCY_COEFF * (1.0 - move_count / MAX_GAME_LENGTH)
            reward += 1.0 + efficiency   # win: 1.0 + efficiency bonus
        else:
            reward += -1.0               # loss: -1.0 (zero-sum)

    return reward
```

The reward is computed per-decision-point. The VP difference shaping provides a weak continuous signal; the terminal reward dominates with +1.0/−1.0 base plus up to +0.3 for fast wins.

### 5. Decision-Point Auto-Advance

The adapter's `step()` method implements Option B: the agent only receives observations at genuine decision points. Chance nodes (dice rolls), forced-discard phases for other players, and internal robber-phase transitions are auto-processed internally.

```
┌─ step(action_id) ───────────────────────────────────┐
│                                                      │
│  1. Map action_id → typed Scala move                 │
│  2. Call bridge.apply_move(move, state)              │
│  3. Update internal state reference                  │
│  4. Determine phase from PhaseMachine                │
│                                                      │
│  ┌─ Auto-advance loop ──────────────────────────┐   │
│  │                                                │   │
│  │  while next_phase is non-decision:             │   │
│  │    if chance(PreRoll):                         │   │
│  │      sample dice from 2-12 with correct probs  │   │
│  │      apply roll → distribute resources         │   │
│  │    elif DiscardPhase for other players:        │   │
│  │      random discard (or heuristic for v1)      │   │
│  │    elif RobberPhase:                           │   │
│  │      random robber + steal (or heuristic)      │   │
│  │    elif GameOver:                              │   │
│  │      break (terminal)                          │   │
│  │    update phase, accumulate rewards            │   │
│  │                                                │   │
│  │  if road_builder_active and 1 placed:          │   │
│  │    → agent decision (choose 2nd road or skip)  │   │
│  │                                                │   │
│  └────────────────────────────────────────────────┘   │
│                                                      │
│  5. Build observation tensor for current player      │
│  6. Apply masking (if enabled)                       │
│  7. Enumerate legal action IDs from grouped moves    │
│  8. Return (obs, total_reward, is_terminal, info)    │
└──────────────────────────────────────────────────────┘
```

**Two-step road builder**: After the first free road is applied, the adapter detects "road builder active, 1 road placed." The legal action mask is constrained to valid free-road edges only (via `bridge.get_road_builder_roads()`), plus a `SKIP_ROAD_BUILDER` sentinel at ID 663. After the second road (or skip) is applied, the mask reverts to standard MainPlay actions.

### 6. CatanAdapter — Full API

```python
class CatanAdapter:
    """Bridge-agnostic Catan environment adapter.
    
    Two interfaces:
    - Gymnasium-style: reset() / step() — direct RL env loop
    - Granular getters: observation / legal_actions / apply_action() — OpenSpiel State
    """

    def __init__(self, bridge: JVMBridge, num_players: int = 4,
                 mask_mode: bool = True, seed: int | None = None):
        ...

    # --- Gymnasium-style interface ---
    def reset(self) -> np.ndarray:
        """New game. Returns initial observation tensor."""
        ...

    def step(self, action_id: int) -> tuple[np.ndarray, float, bool, dict]:
        """Apply action, auto-advance, return (obs, reward, done, info)."""
        ...

    @property
    def action_space_size(self) -> int: return 1024
    @property
    def observation_space_size(self) -> int: return 727

    # --- Granular getters (for OpenSpiel State) ---
    @property
    def observation(self) -> np.ndarray: ...
    @property
    def legal_actions(self) -> list[int]: ...
    @property
    def legal_actions_mask(self) -> np.ndarray: ...
    @property
    def current_player(self) -> int: ...
    @property
    def num_players(self) -> int: ...
    @property
    def is_terminal(self) -> bool: ...
    @property
    def returns(self) -> list[float]: ...
    @property
    def reward(self) -> float: ...

    def apply_action(self, action_id: int) -> None:
        """Apply and auto-advance. Updates internal state. Follow with property reads."""
        ...

    # --- State management ---
    def clone(self) -> 'CatanAdapter':
        """Shallow copy. Scala state is shared (immutable)."""
        ...

    def set_mask_mode(self, enabled: bool) -> None:
        """Toggle between perfect-information (mask=False) and
           public-information (mask=True) observation tensors."""
        ...

    def to_dict(self) -> dict:
        """Serialize for checkpointing (via tensor roundtrip)."""
        ...

    @staticmethod
    def from_dict(data: dict, bridge: JVMBridge) -> 'CatanAdapter':
        """Reconstruct from serialized form."""
        ...
```

### 7. OpenSpiel Python Game Wrapper (~100 lines)

The `CatanGame` / `CatanState` classes are thin shims that delegate to the adapter:

```python
import pyspiel

_NUM_PLAYERS = 4
_NUM_ACTIONS = 1024
_NUM_OBS = 727

_GAME_TYPE = pyspiel.GameType(
    short_name="catan",
    long_name="Settlers of Catan",
    dynamics=pyspiel.GameType.Dynamics.SEQUENTIAL,
    chance_mode=pyspiel.GameType.ChanceMode.EXPLICIT_STOCHASTIC,
    information=pyspiel.GameType.Information.IMPERFECT_INFORMATION,
    utility=pyspiel.GameType.Utility.GENERAL_SUM,
    reward_model=pyspiel.GameType.RewardModel.REWARDS,
    max_num_players=4,
    min_num_players=3,
    provides_information_state_string=False,
    provides_information_state_tensor=False,
    provides_observation_string=False,
    provides_observation_tensor=True,
    parameter_specification={
        "players": pyspiel.GameParameter(num_players=_NUM_PLAYERS),
        "mask": pyspiel.GameParameter(default_value=True),  # public info
    },
)

_GAME_INFO = pyspiel.GameInfo(
    num_distinct_actions=_NUM_ACTIONS,
    max_chance_outcomes=11,
    num_players=_NUM_PLAYERS,
    min_utility=-1.3,
    max_utility=1.3,
    utility_sum=None,       # general sum (not constant sum)
    max_game_length=600,    # generous upper bound
)


class CatanGame(pyspiel.Game):
    def __init__(self, params=None):
        super().__init__(_GAME_TYPE, _GAME_INFO, params or {})

    def new_initial_state(self):
        return CatanState(self)

    def observation_tensor_shape(self):
        return [1, 1, _NUM_OBS]  # single frame


class CatanState(pyspiel.State):
    def __init__(self, game: CatanGame):
        super().__init__(game)
        self._adapter = CatanAdapter(
            bridge=_get_bridge(),
            num_players=game.num_players(),
            mask_mode=game.get_parameters().get("mask", True),
        )
        self._adapter.reset()

    def current_player(self):
        if self._adapter.is_terminal:
            return pyspiel.PlayerId.TERMINAL
        return self._adapter.current_player

    def _legal_actions(self, player):
        if player != self._adapter.current_player:
            return []
        return self._adapter.legal_actions

    def _apply_action(self, action):
        self._adapter.apply_action(action)   # auto-advances internally

    def is_terminal(self):
        return self._adapter.is_terminal

    def rewards(self):
        if self.is_terminal():
            return self._adapter.returns
        return [0.0] * self.num_players()

    def returns(self):
        return self._adapter.returns

    def _observation_tensor(self, player):
        return self._adapter.observation

    def is_chance_node(self):
        # Adapter handles chance internally; OpenSpiel never sees a chance state.
        return False

    def __str__(self):
        return f"CatanState(player={self._adapter.current_player}, "
               f"returns={self._adapter.returns})"

    def clone(self):
        s = CatanState(self.get_game())
        s._adapter = self._adapter.clone()
        return s


pyspiel.register_game(_GAME_TYPE, CatanGame)
```

**Key design note**: The adapter absorbs chance nodes internally (decision B from design). OpenSpiel's `State.is_chance_node()` always returns `False`. Dice rolls are sampled during `_apply_action()`, not as a separate chance-player turn. This simplifies the training loop while remaining compatible with OpenSpiel algorithms that expect `EXPLICIT_STOCHASTIC` — the `max_chance_outcomes` and `chance_mode` in `GameType` are informational metadata; OpenSpiel algorithms don't require `is_chance_node()` to return True if the game handles randomness internally.

### 8. Training Loop (Self-Play)

One model controls all players with a player-relative observation tensor:

```python
model = CatanPolicyNetwork(num_actions=1024, obs_size=727)
replay_buffer = ReplayBuffer()

agent = OpenSpielAgent(model, replay_buffer)

for episode in range(num_episodes):
    state = game.new_initial_state()
    trajectory = []

    while not state.is_terminal():
        current_player = state.current_player()
        obs = state.observation_tensor(current_player)
        legal_mask = state.legal_actions_mask(current_player)

        action = model.select_action(obs, legal_mask)
        state.apply_action(action)

        reward = state.rewards()[current_player]
        next_obs = state.observation_tensor(current_player)

        trajectory.append((obs, action, reward, next_obs, legal_mask))

    # Terminal returns
    for t, (obs, act, _, next_obs, mask) in enumerate(trajectory):
        # Discounted return from t to end, player perspective
        total_reward = compute_discounted_return(trajectory, t, gamma=0.99)
        replay_buffer.push(obs, act, total_reward, next_obs, mask)

    if episode % train_every == 0:
        model.train(replay_buffer.sample(batch_size))
```

For multi-agent RL algorithms (NFSP, CFR), the OpenSpiel algorithms layer handles trajectory partitioning by player internally.

### 9. Repository & File Layout

#### ImmutableSOC Repo (Scala) — completed

```
src/main/scala/soc/rules/            # Scala — completed (ddc61a1)
├── LegalMoveGenerator.scala
├── PhaseMachine.scala (in package.scala)
└── validators/
    ├── SetupValidator.scala
    ├── BuildingValidator.scala
    ├── TradeValidator.scala
    ├── RobberValidator.scala
    ├── DevCardValidator.scala
    └── TurnValidator.scala

src/main/scala/soc/base/BaseGame.scala  # Added: initial state factories
#   perfectInfoInitialState(board, robberLocation?, devCardDeck?, bank?)
#   publicInfoInitialState(board, robberLocation?, deckSize?, bank?)
#   standardDevCardDeck: List[DevelopmentCard]
#   findDesert(board): Option[Int]

# No Python files. No jpype dependency.
# Publishes JAR via GitHub Packages: sbt publish
```

#### Adapter Repo (Python) — to be created

```
catan-rl/                            # Standalone Python package
├── pyproject.toml                   # dependencies: jpype, numpy, pyspiel
├── src/catan_rl/
│   ├── __init__.py
│   ├── bridge/
│   │   ├── __init__.py
│   │   ├── base.py                  # JVMBridge ABC
│   │   └── jpype_bridge.py          # JpypeBridge (loads ImmutableSOC JAR at runtime)
│   ├── adapter/
│   │   ├── __init__.py
│   │   ├── adapter.py               # CatanAdapter (main class)
│   │   ├── action_encoder.py        # ActionEncoder (ID ↔ Move mapping)
│   │   ├── observation.py           # ObservationTensor encoder + masking
│   │   └── reward.py                # Reward computation
│   └── openspiel/
│       ├── __init__.py
│       └── catan_game.py            # CatanGame / CatanState (OpenSpiel wrapper)
└── tests/
    ├── test_action_encoder.py
    ├── test_observation.py
    ├── test_adapter.py
    └── test_openspiel_integration.py
```

**Runtime contract**: The adapter repo depends on ImmutableSOC only at runtime — it loads the compiled JAR via jpype. The JAR location is configurable (classpath variable). ImmutableSOC version is pinned in `pyproject.toml` as a metadata dependency (documentation only, since jpype loads the JAR directly).

### 10. Implementation Order

All Python work happens in the adapter repo. ImmutableSOC needs no changes.

| Step | Component | Repo | Est. | Depends on |
|------|-----------|:---:|:--:|------------|
| 1 | `JVMBridge` ABC + `JpypeBridge` (loads ImmutableSOC JAR) | Python | 3-5 days | jpype setup, ImmutableSOC JAR published |
| 2 | `ActionEncoder` (ID ↔ Move) | Python | 3-5 days | Step 1 |
| 3 | `ObservationTensor` encoder + masking | Python | 2-3 days | Step 1 |
| 4 | `CatanAdapter` core (+ `step`/`reset`/auto-advance) | Python | 3-5 days | Steps 1-3 |
| 5 | `CatanAdapter.reward` | Python | 1-2 days | Step 4 |
| 6 | `CatanAdapter.clone`/serialize | Python | 1-2 days | Step 4 |
| 7 | `CatanGame`/`CatanState` OpenSpiel wrapper | Python | 1-2 days | Steps 4-6 |
| 8 | OpenSpiel build + integration test | Python | 2-3 days | Step 7 |
| 9 | Self-play training loop validation | Python | 2-3 days | Step 8 |
| | **Total** | | **~3-5 weeks** | |

**ImmutableSOC prerequisite**: The JAR must be published to GitHub Packages before Step 1. State factories are done (`BaseGame.perfectInfoInitialState` / `publicInfoInitialState`). No other Scala changes are needed — the existing `LegalMoveGenerator`, `BaseGame`, and `BaseGameFixtures` cover all adapter requirements.

### 11. Decisions Summary

| # | Decision | Choice |
|---|----------|--------|
| 1 | JVM-Python bridge | jpype initially, with `JVMBridge` ABC for future swap to Py4J/gRPC |
| 2 | Action encoding | Flat integer IDs, exact-fit ranges, `NumDistinctActions=1024` |
| 3 | Observation tensor | Masked Full Info (Option D) — 727 channels, toggleable mask per player |
| 4 | Adapter style | Gymnasium-style `reset()`/`step()` + granular getters (dual interface) |
| 5 | Adapter location | Pure Python, owns tensor encoding + action mapping |
| 6 | Road builder | Two-step sequential (agent places road 1, then road 2 or skip) |
| 7 | Dice/chance | OpenSpiel native chance mode; adapter absorbs internally |
| 8 | Phase handling | Decision-point only (Option B) — auto-advance past chance/discard/robber |
| 9 | Reward | VP-difference shaping (+0.01 per VP) + terminal efficiency bonus (+0.3 max) |
| 10 | Legal action mask | Allocate-and-fill: iterate legal_moves, set mask bits |
| 11 | OpenSpiel game type | Python game (`pyspiel.register_game`) |
| 12 | Self-play | One model, player-relative tensor rotation |
| 13 | State model | Adapter holds immutable Scala state reference; `clone()` is shallow copy |
| 14 | Serialization | Tensor roundtrip first, JSON later |
| 15 | Terminal reward | Winner-only efficiency bonus: `+1.0 + 0.3 × (1 − moves/max)` |
| 16 | Repository split | Adapter lives in separate repo; ImmutableSOC is a runtime JAR dependency only |
