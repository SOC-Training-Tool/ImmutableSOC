package soc.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.*
import soc.base.BaseGame.*
import soc.base.BaseGameFixtures
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*
import soc.rules.RulesFixtures.*
import soc.rules.validators.DevCardValidator

class DevCardValidatorSpec extends AnyFunSpec with Matchers:

  private val cached = new CachedBoard[Resource](BaseGameFixtures.perfectInfoFixture.board)

  private def stateWith(inventory: Map[Int, Resources] = Map.empty,
                        devCards: Map[Int, Seq[(DevelopmentCard, Int)]] = Map.empty,
                        deck: List[DevelopmentCard] = Nil,
                        turn: Int = 0): PerfectInfoState =
    afterSetupPerfect.copy(
      privateInventories = PrivateInventories(inventory),
      privateDevCardInv = PrivateDevCardInv(devCards),
      developmentCardDeck = DevelopmentCardDeck(deck),
      turn = Turn(turn)
    )

  describe("buyMoves"):

    it("allows buying a dev card when affordable and the deck is not empty"):
      val state = stateWith(inventory = Map(0 -> ResourceSet(or = 1, wh = 1, sh = 1)), deck = List(Knight, Point))
      val inv = new PerfectInfoResourceView(state)
      val devView = new PerfectInfoDevCardView(state)
      DevCardValidator.canBuy(0, inv, devView) shouldBe true
      DevCardValidator.perfectBuyMoves(0, inv, devView, state.developmentCardDeck.cards.headOption).map(_.card) shouldBe Seq(Knight)

    it("rejects buying when the deck is empty"):
      val state = stateWith(inventory = Map(0 -> ResourceSet(or = 1, wh = 1, sh = 1)), deck = Nil)
      val inv = new PerfectInfoResourceView(state)
      val devView = new PerfectInfoDevCardView(state)
      DevCardValidator.canBuy(0, inv, devView) shouldBe false
      DevCardValidator.perfectBuyMoves(0, inv, devView, None) shouldBe empty

    it("rejects buying when the player cannot afford the cost"):
      val state = stateWith(inventory = Map(0 -> ResourceSet(or = 1, wh = 1)), deck = List(Knight))
      val inv = new PerfectInfoResourceView(state)
      DevCardValidator.canBuy(0, inv, new PerfectInfoDevCardView(state)) shouldBe false

  describe("card ownership and timing"):

    it("allows playing a card owned from a previous turn"):
      val state = stateWith(devCards = Map(0 -> Seq((Knight, 1))), turn = 2)
      val devView = new PerfectInfoDevCardView(state)
      DevCardValidator.canPlay(0, Nil, devView, Knight, 2) shouldBe true

    it("blocks playing a card bought on the current turn"):
      val state = stateWith(devCards = Map(0 -> Seq((Knight, 2))), turn = 2)
      DevCardValidator.canPlay(0, Nil, new PerfectInfoDevCardView(state), Knight, 2) shouldBe false

    it("enforces the one-dev-card-per-turn limit"):
      val state = stateWith(devCards = Map(0 -> Seq((Knight, 1), (Monopoly, 1))), turn = 2)
      val devView = new PerfectInfoDevCardView(state)
      val played = Seq(PlayMonopolyMoveResult[Resource](0, WHEAT, Map.empty))
      DevCardValidator.canPlay(0, played, devView, Knight, 2) shouldBe false

    it("blocks plays when the player owns no such card"):
      val state = stateWith(devCards = Map(0 -> Seq((Knight, 1))), turn = 2)
      DevCardValidator.canPlay(0, Nil, new PerfectInfoDevCardView(state), Monopoly, 2) shouldBe false

  describe("play moves"):

    it("enumerates knight plays with robber placement"):
      val state = stateWith(devCards = Map(0 -> Seq((Knight, 1))), turn = 2)
      val devView = new PerfectInfoDevCardView(state)
      val moves = DevCardValidator.perfectPlayKnightMoves(
        0, Nil, devView, 2, state.robberLocation, cached, state.vertexBuildingState,
        new PerfectInfoResourceView(state), _ => Some(Wood)
      )
      moves should not be empty
      moves.forall(_.inner.player == 0) shouldBe true
      moves.map(_.inner.robberHexId).toSet should not contain (10)

    it("enumerates monopoly for every resource type"):
      val state = stateWith(devCards = Map(0 -> Seq((Monopoly, 1))), turn = 2)
      val devView = new PerfectInfoDevCardView(state)
      val moves = DevCardValidator.playMonopolyMoves(0, Nil, devView, 2)
      moves.map(_.res).toSet shouldBe Resources.all.toSet

    it("enumerates year of plenty for any two resources"):
      val state = stateWith(devCards = Map(0 -> Seq((YearOfPlenty, 1))), turn = 2)
      val devView = new PerfectInfoDevCardView(state)
      val moves = DevCardValidator.playYearOfPlentyMoves(0, Nil, devView, 2)
      moves.length shouldBe 25
      moves.forall(m => Resources.all.contains(m.c1) && Resources.all.contains(m.c2)) shouldBe true

    it("road builder returns 1-2 connected free roads"):
      val state = stateWith(
        devCards = Map(0 -> Seq((RoadBuilder, 1))),
        turn = 2
      ).copy(
        vertexBuildingState = VertexBuildingState(Map(Vertex(41) -> PlayerBuilding(0, Settlement))),
        edgeBuildingState = EdgeBuildingState(Map(Edge(Vertex(40), Vertex(41)) -> PlayerBuilding(0, Road)))
      )
      val devView = new PerfectInfoDevCardView(state)
      val moves = DevCardValidator.playRoadBuilderMoves(0, Nil, devView, 2, state.edgeBuildingState, state.vertexBuildingState, cached)
      moves should not be empty
      moves.exists(_.edge2.isEmpty) shouldBe true
      moves.exists(_.edge2.nonEmpty) shouldBe true
      val possibleEdges = Set(
        Edge(Vertex(40), Vertex(39)),
        Edge(Vertex(17), Vertex(40)),
        Edge(Vertex(41), Vertex(42)),
        Edge(Vertex(41), Vertex(51))
      )
      moves.forall(m => possibleEdges.contains(m.edge1)) shouldBe true
