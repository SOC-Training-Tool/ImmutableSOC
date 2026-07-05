package game

trait StateField[S, F <: GameState[F]]:
  def get(state: S): F
  def set(state: S, field: F): S

trait StateFields[S]:
  def apply[F <: GameState[F]](using StateField[S, F]): StateField[S, F] = summon[StateField[S, F]]

object StateField:
  inline given derived[S <: Product, F <: GameState[F]](using
      sl: Slice[S, F]
  ): StateField[S, F] = new StateField[S, F]:
    def get(state: S): F            = sl.get(state)
    def set(state: S, field: F): S  = sl.set(state, field)

object StateFields:
  inline given derived[S <: Product]: StateFields[S] = new StateFields[S] {}