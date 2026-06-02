package game

trait GameState[T]:
  self: T =>
  type Delta
  def apply(delta: Delta): T
