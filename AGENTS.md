# PROJECT KNOWLEDGE BASE

**Generated:** 2026-07-10
**Commit:** 379c2a7
**Branch:** simple

## OVERVIEW

ImmutableSOC is a Scala 3.5.2 library that applies Settlers of Catan events to immutable, type-safe game state. It is a replay applier: it trusts already-validated moves and deterministically transforms state. It supports two game modes: perfect information (server-side) and public information (player perspective).

## STRUCTURE

```
.
├── src/main/scala/
│   ├── game/                 # Generic game engine (no Catan logic)
│   ├── soc/core/             # Catan domain types (resources, board, core moves/state)
│   ├── soc/base/             # Catan game assembly (state, board, inventories, builder)
│   └── soc/base/actions/     # 18 GameAction implementations
├── src/test/scala/           # ScalaTest specs; see COMMANDS
├── project/                  # sbt build config
└── .github/workflows/        # CI, release, publish
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Add a new move | `soc/core/moves.scala`, `soc/base/moves.scala` | Core move first, then base-level result wrappers |
| Add a new action | `soc/base/actions/` | Follow existing `GameAction[Move, Input, Output]` pattern |
| Change state shape | `soc/base/BaseGame.scala`, `soc/base/state/`, `soc/core/state/` | PerfectInfoState and PublicInfoState must stay aligned |
| Change board geometry | `soc/core/SOCBoard.scala`, `soc/base/BaseBoard.scala` | SOCBoard is a typeclass; BaseBoard is the concrete instance |
| Build a game instance | `soc/base/ImmutableGameBuilder.scala` | `register(action)...build` pipeline |
| Apply a move | `game/ImmutableGame.scala` | `applyMove[M](move, state): (OutFor[M], STATE)` |
| Read usage examples | `README.md` | Examples assume commented-out game instances are re-enabled |

## CODE MAP

No Scala LSP or codegraph is available in this environment; centrality (Refs) is unmeasured.

| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `ImmutableGame` | trait | `game/ImmutableGame.scala` | Engine interface: `applyMove[M](move, state)` |
| `GameAction` | trait | `game/GameAction.scala` | `apply(move, input) => Output` for one move type |
| `GameState` | trait | `game/GameState.scala` | State component with a typed `Delta` |
| `Slice` / `StateField` | traits | `game/Slice.scala`, `game/StateField.scala` | Compile-time field extraction from state tuple |
| `PerfectInfoState` | case class | `soc/base/BaseGame.scala` | Full server-side state (15 fields) |
| `PublicInfoState` | case class | `soc/base/BaseGame.scala` | Player-facing state (counts, no exact cards) |
| `PerfectInfoMove` / `PublicInfoMove` | type aliases | `soc/base/BaseGame.scala` | 16-move union types |
| `BaseBoard` | case class | `soc/base/BaseBoard.scala` | Concrete board with hexes + ports |
| `SOCBoard` | typeclass | `soc/core/SOCBoard.scala` | Board geometry ops (adjacency, vertices, edges) |
| `ResourceSet` | factory | `soc/core/package.scala` | Resource quantity DSL: `ResourceSet(wo=2, br=1)` |
| `StateTransformer` | object | `soc/base/StateTransformer.scala` | Compile-time delta application to tuples |
| `InventorySet` | case class | `game/InventorySet.scala` | Generic typed inventory |

## CONVENTIONS

- **Scala 3 syntax**: union types (`A | B`), `using` clauses, colon package syntax.
- **Imports**: wildcard imports are common (`import soc.core.*`, `import soc.base.state.*`).
- **Naming**: PascalCase types/objects, UPPER_SNAKE_CASE constants (`WOOD`, `SETTLEMENT_COST`).
- **State access**: use `state.select[Type]` derived from `StateField`.
- **Moves**: pass plain case-class values directly; the game instance is typed over a Scala 3 union (`PerfectInfoMove` / `PublicInfoMove`).
- **Tests**: ScalaTest `AnyFunSpec` with `Matchers`; class names end in `Spec`.
- **Commits**: conventional commits drive releases (`fix:`, `feat:`, `feat!:`).

## ANTI-PATTERNS (THIS PROJECT)

- `BaseBoard.scala:39` uses `vertexMap(node)` without a safe fallback; marked `// TODO: unsafe`.
- `StateTransformer.scala` contains `// FIX:` workarounds with `asInstanceOf` casts to dodge Scala 3 extension-method resolution issues.
- Do not add mutable `var`s in production code; all state transforms return new values.
- Do not delete or ignore the active `soc/base/actions/GameActionSpec.scala`; the larger commented-out specs are stale and not run.

## UNIQUE STYLES

- **HList state**: game state is a heterogeneous list of `GameState` components, manipulated via type-level `Slice`/`StateField` derivation.
- **Union-typed moves**: every move is an element of a Scala 3 union type (`PerfectInfoMove` / `PublicInfoMove`).
- **Action output pattern**: each action returns a case class of deltas; `StateTransformer` applies them to the state tuple at compile time.
- **Dual state models**: perfect and public info share the same action logic but swap inventory/dev-card state types.

## COMMANDS

```bash
sbt test                                    # run tests
sbt clean coverage test coverageReport      # CI command (note: coverage path references scala-2.13 but project is 3.5.2)
sbt publish                                 # publish to GitHub Packages
```

## NOTES

- Workflows publish to GitHub Packages using `GITHUB_TOKEN`; the release workflow needs a `PAT_TOKEN` secret to push tags.
- `BaseGame.perfectInfoGame` and `BaseGame.publicInfoGame` are wired via `ImmutableGameBuilder`; the nested `PerfectInfoGame`/`PublicInfoGame` objects are private.
- `ImmutableGameBuilder.build` is implemented and dispatches moves by `ClassTag`.
- Action outputs with `Option[Delta]` / `List[Delta]` fields are applied via `StateTransformer.updateFlexible` / `OutputApplier`.
- `ImmutableGame.applyMoveAny(move, state)` accepts a raw union-typed move and returns `(AllOutputs, STATE)`, allowing `List[PerfectInfoMove]` / `List[PublicInfoMove]` replays.
- Active integration tests: `BaseGameSpec` and `game/ImmutableGameSpec`; `GameStatsSpec` and the older `soc/base/GameActionSpec` remain commented out.
- `src/test/scala/soc/base/BaseGameFixtures.scala` is uncommented and compiles; it provides full-game replay fixtures for both game modes.
- The release workflow needs a `PAT_TOKEN` secret to push tags.
