# soc/base/actions

**Scope:** Individual Catan move implementations as `GameAction` instances.

## OVERVIEW

This package contains the 18 action handlers that turn a move + extracted state input into a typed output of deltas. Actions are pure functions; they never mutate state directly. The generic engine in `game/` dispatches moves to the registered action, and `StateTransformer` applies the resulting deltas to the state tuple.

Actions in this library do **not** validate move legality. They assume the incoming move has already been validated by an authoritative rules engine and simply compute the state deltas. Validation — resources, board placement, turn order, trade ratios, robber sequence, development-card timing — belongs in a layer above the replay applier.

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Add an action | New `*Action.scala` file here | Extend `GameAction[Move, Input, Output]` |
| See the action contract | `game/GameAction.scala` | `apply(move, input): Output` |
| See cost constants | `soc/base/actions/package.scala` | `SETTLEMENT_COST`, `CITY_COST`, `ROAD_COST`, `DEV_CARD_COST` |
| See a minimal action | `BuildRoadCoreAction.scala` | 22-line template: output case class + `apply` |
| See a stateful action | `RollDiceAction.scala` | Uses `RollDiceInput` to read board/turn/buildings |
| See imperfect-info action | `PublicRobberAction.scala`, `PublicBuyDevCardAction.scala` | Hide exact cards from output |
| See dev-card actions | `PlayKnightAction.scala`, `PlayMonopolyAction.scala`, `PlayYearOfPlentyAction.scala`, `PlayPointAction.scala`, `PlayRoadBuilderCoreAction.scala`, `RemoveKnightCardAction.scala` | Knight has separate remove action |

## CONVENTIONS

- Each action file defines exactly one public `class *Action extends GameAction[...]`.
- Output case classes live in the same file as the action and name every delta field explicitly.
- `NoInput.type` is used when the action only needs the move.
- `TurnInput(turn)` is the shared wrapper when an action needs the current turn.
- Costs are taken from the package object: `ROAD_COST`, `SETTLEMENT_COST`, `CITY_COST`, `DEV_CARD_COST`.
- Use `ResourceInventories.Lose`/`Gain` and `DevTransactions.*` for inventory deltas.
- Use `BoardBuildingState.add` for vertex/edge building placements.

## PRECONDITIONS PER ACTION

| Action | Preconditions expected by the replay applier |
|--------|----------------------------------------------|
| Initial placement | Vertex and edge are already legal for the current setup phase; player owns no resources are checked here. |
| Robber steal | Robber move is already validated; a steal target may be absent and is treated as no-steal. |
| Buy development card | Deck has at least one card in the authoritative event stream; empty-deck removal is now a no-op. |
| Port trade | Player has already satisfied the trade ratio and port access; inventories are updated only. |
| Build road | Player has already paid the cost and chosen a legal edge. |
| Build settlement | Player has already paid the cost and chosen a legal vertex. |
| Build city | Player has already paid the cost and chosen an existing settlement to upgrade. |
| Discard | Discard amount is already authoritative; the action only applies the delta. |
| Play knight | Card play timing and card ownership are already validated externally. |
| Play monopoly | Chosen resource and victim loss map are already authoritative. |
| Play year of plenty | Chosen resources are already authoritative. |
| Play road builder | Chosen edges are already authoritative and legal. |
| Play point | Card play timing is already authoritative. |

## ANTI-PATTERNS

- Do not produce a full new state inside an action; produce deltas only.
- Do not access `state.select[...]` directly in an action; rely on the typed `Input` provided by the engine.
- Do not add side effects (logging, I/O, randomness); actions must be pure.
- Do not duplicate cost constants; import them from the package object.
