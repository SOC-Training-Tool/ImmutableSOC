package soc.base

import game.{GameState, NoInput, Slice, StateField}
import soc.base.BaseGame.{BaseEdgeBuilding, BaseVertexBuilding}
import soc.base.DevelopmentCards.DevelopmentCard
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.DevTransactions.*
import soc.core.state.*

import scala.compiletime.{erasedValue, summonInline}
import scala.deriving.Mirror

trait Extractor[S, In]:
  def extract(state: S): In

object Extractor extends LowPriorityExtractor:

  given [S]: Extractor[S, NoInput.type] with
    def extract(state: S) = NoInput

private trait LowPriorityExtractor:
  inline given derived[S <: Product, In <: Product](using m: Mirror.ProductOf[In]): Extractor[S, In] =
    new Extractor[S, In]:
      def extract(state: S): In =
        m.fromProduct(extractTuple[S, m.MirroredElemTypes](state))

  private inline def extractTuple[S, Elems <: Tuple](state: S): Elems =
    inline erasedValue[Elems] match
      case _: EmptyTuple     => EmptyTuple.asInstanceOf[Elems]
      case _: (head *: tail) =>
        (summonInline[Slice[S, head]].get(state).asInstanceOf[head] *: extractTuple[S, tail](state)).asInstanceOf[Elems]

// ---- Applier: maps a delta type to a state update operation ----

trait Applier[S, Delta]:
  def apply(s: S, d: Delta): S

object Applier:

  given turnApplier[S](using sf: StateField[S, Turn]): Applier[S, Turn#Delta] with
    def apply(s: S, d: Turn#Delta): S = sf.set(s, sf.get(s)(d))

  given bankApplier[S](using sf: StateField[S, Bank[Resource]]): Applier[S, Bank[Resource]#Delta] with
    def apply(s: S, d: Bank[Resource]#Delta): S = sf.set(s, sf.get(s)(d))

  given pointsApplier[S](using sf: StateField[S, PlayerPoints]): Applier[S, PlayerPoints#Delta] with
    def apply(s: S, d: PlayerPoints#Delta): S = sf.set(s, sf.get(s)(d))

  given robberApplier[S](using sf: StateField[S, RobberLocation]): Applier[S, RobberLocation#Delta] with
    def apply(s: S, d: RobberLocation#Delta): S = sf.set(s, sf.get(s)(d))

  given vertexApplier[S](using sf: StateField[S, VertexBuildingState[BaseVertexBuilding]]): Applier[S, VertexBuildingState[BaseVertexBuilding]#Delta] with
    def apply(s: S, d: VertexBuildingState[BaseVertexBuilding]#Delta): S = sf.set(s, sf.get(s)(d))

  given edgeApplier[S](using sf: StateField[S, EdgeBuildingState[BaseEdgeBuilding]]): Applier[S, EdgeBuildingState[BaseEdgeBuilding]#Delta] with
    def apply(s: S, d: EdgeBuildingState[BaseEdgeBuilding]#Delta): S = sf.set(s, sf.get(s)(d))

  given pubDevApplier[S](using sf: StateField[S, PublicDevCardInv[DevelopmentCard]]): Applier[S, PublicDevCardInv[DevelopmentCard]#Delta] with
    def apply(s: S, d: PublicDevCardInv[DevelopmentCard]#Delta): S = sf.set(s, sf.get(s)(d))

  given privDevApplier[S](using sf: StateField[S, PrivateDevCardInv[DevelopmentCard]]): Applier[S, PrivateDevCardInv[DevelopmentCard]#Delta] with
    def apply(s: S, d: PrivateDevCardInv[DevelopmentCard]#Delta): S = sf.set(s, sf.get(s)(d))

  given deckSizeApplier[S](using sf: StateField[S, DevelopmentCardDeckSize]): Applier[S, DevelopmentCardDeckSize#Delta] with
    def apply(s: S, d: DevelopmentCardDeckSize#Delta): S = sf.set(s, sf.get(s)(d))

  given deckApplier[S](using sf: StateField[S, DevelopmentCardDeck[DevelopmentCard]]): Applier[S, DevelopmentCardDeck[DevelopmentCard]#Delta] with
    def apply(s: S, d: DevelopmentCardDeck[DevelopmentCard]#Delta): S = sf.set(s, sf.get(s)(d))

  given privGainApplier[S](using sf: StateField[S, PrivateInventories[Resource]], ri: ResourceInventory[PrivateInventories[Resource]]): Applier[S, Gain[Resource]] with
    def apply(s: S, d: Gain[Resource]): S = sf.set(s, ri.applyGain(sf.get(s), d))

  given privLoseApplier[S](using sf: StateField[S, PrivateInventories[Resource]], ri: ResourceInventory[PrivateInventories[Resource]]): Applier[S, Lose[Resource]] with
    def apply(s: S, d: Lose[Resource]): S = sf.set(s, ri.applyLose(sf.get(s), d))

  given pubGainApplier[S](using sf: StateField[S, PublicInventories[Resource]], ri: ResourceInventory[PublicInventories[Resource]]): Applier[S, Gain[Resource]] with
    def apply(s: S, d: Gain[Resource]): S = sf.set(s, ri.applyGain(sf.get(s), d))

  given pubLoseApplier[S](using sf: StateField[S, PublicInventories[Resource]], ri: ResourceInventory[PublicInventories[Resource]]): Applier[S, Lose[Resource]] with
    def apply(s: S, d: Lose[Resource]): S = sf.set(s, ri.applyLose(sf.get(s), d))

  given privPlayCardApplier[S](using sf: StateField[S, PrivateDevCardInv[DevelopmentCard]], dci: DevCardInventory[PrivateDevCardInv[DevelopmentCard]]): Applier[S, PlayCard[DevelopmentCard]] with
    def apply(s: S, d: PlayCard[DevelopmentCard]): S = sf.set(s, dci.applyPlayCard(sf.get(s), d))

  given pubPlayCardApplier[S](using sf: StateField[S, PublicDevCardInv[DevelopmentCard]], dci: DevCardInventory[PublicDevCardInv[DevelopmentCard]]): Applier[S, PlayCard[DevelopmentCard]] with
    def apply(s: S, d: PlayCard[DevelopmentCard]): S = sf.set(s, dci.applyPlayCard(sf.get(s), d))

  given gainOrLoseApplier[S](using
    ga: Applier[S, Gain[Resource]],
    la: Applier[S, Lose[Resource]]
  ): Applier[S, Gain[Resource] | Lose[Resource]] with
    def apply(s: S, d: Gain[Resource] | Lose[Resource]): S = d match
      case g: Gain[Resource] => ga(s, g)
      case l: Lose[Resource] => la(s, l)

  given publicInvApplier[S](using sf: StateField[S, PublicInventories[Resource]]): Applier[S, PublicInventories[Resource]#Delta] with
    def apply(s: S, d: PublicInventories[Resource]#Delta): S = sf.set(s, sf.get(s)(d))

  given ieApplier[S](using sf: StateField[S, PublicInventories[Resource]]): Applier[S, ImperfectInfoExchange[Resource]] with
    def apply(s: S, d: ImperfectInfoExchange[Resource]): S = sf.set(s, sf.get(s)(d))

  given listApplier[S, A](using aa: Applier[S, A]): Applier[S, List[A]] with
    def apply(s: S, ds: List[A]): S = ds.foldLeft(s)((s, d) => aa(s, d))

  given optionApplier[S, A](using aa: Applier[S, A]): Applier[S, Option[A]] with
    def apply(s: S, od: Option[A]): S = od.fold(s)(d => aa(s, d))

// ---- Updater ----

trait Updater[S, Out]:
  def update(state: S, out: Out): S

object Updater extends LowPriorityUpdater

private trait LowPriorityUpdater:
  inline given derived[S <: Product, Out <: Product](using m: Mirror.ProductOf[Out]): Updater[S, Out] =
    new Updater[S, Out]:
      def update(state: S, out: Out): S =
        applyTuple[S, m.MirroredElemTypes](state, Tuple.fromProduct(out).asInstanceOf[m.MirroredElemTypes])

  private inline def applyTuple[S, Elems <: Tuple](s: S, fields: Elems): S =
    inline erasedValue[Elems] match
      case _: EmptyTuple => s
      case _: (head *: tail) =>
        val hd = fields.asInstanceOf[head *: tail].head
        val tl = fields.asInstanceOf[head *: tail].tail
        applyTuple[S, tail](summonInline[Applier[S, head]](s, hd), tl)