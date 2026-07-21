package soc.base.state

import soc.core.{Edge, SOCBoard, Vertex}
import soc.core.SOCBoard.SOCBoardOps
import soc.core.state.{EdgeBuildingState, VertexBuildingState}

private[soc] class LongestRoadOps[Res, BOARD, VB, EB](board: BOARD, edgeBuildingMap: EdgeBuildingState[EB], vertexBuildingMap: VertexBuildingState[VB])(using
    socBoard: SOCBoard[Res, BOARD]
) {

  private val playerIds = edgeBuildingMap.map.toSeq.map(_._2.player).distinct

  def calcLongestRoadLengths(): SOCRoadLengths = SOCRoadLengths(playerIds.foldLeft(Map.empty[Int, Int]) { case (m, p) => m + (p -> calcLongestRoadLength(p)) })

  def calcLongestRoadLength(playerId: Int): Int = {
    val edges = edgeBuildingMap.map.toSeq.flatMap {
      case (edge, building) if building.player == playerId => Seq(edge)
      case _                                               => Nil
    }
    calcLongestRoadLength(playerId, edges*)
  }

  def calcLongestRoadLength(playerId: Int, roads: Edge*): Int = {
    roads.map(r => calcLongestRoadLengthRecur(playerId, List((r.v1, r.v2)), List(r))).maxOption.getOrElse(0)
  }

  private def calcLongestRoadLengthRecur(playerId: Int, stack: List[(Vertex, Vertex)], visited: List[Edge] = Nil): Int = {
    if (stack.isEmpty) visited.length
    else {
      val (v1, v2): (Vertex, Vertex) = stack.head
      if (v1 == v2) {
        return Math.max(visited.length, calcLongestRoadLengthRecur(playerId, stack.tail, visited))
      }

      def fromVertex(vertex: Vertex): List[Vertex] = if (vertexBuildingMap.map.get(vertex).fold(true)(_.player == playerId)) {
        board
          .neighboringVertices(vertex)
          .filterNot(v => visited.contains(Edge(vertex, v)))
          .filter(v => edgeBuildingMap.map.get(Edge(vertex, v)).fold(false)(_.player == playerId))
          .toList
      } else Nil

      (fromVertex(v1), fromVertex(v2)) match {
        // see road scenario1
        case (Nil, Nil)      =>
          Math.max(visited.length, calcLongestRoadLengthRecur(playerId, stack.tail, visited))

        // see road scenario2
        case (Nil, r :: Nil) =>
          calcLongestRoadLengthRecur(playerId, (v1, r) :: stack.tail, Edge(v2, r) :: visited)

        case (Nil, r1 :: r2 :: Nil) =>
          Seq(
            calcLongestRoadLengthRecur(playerId, (v1, r1) :: stack.tail, Edge(v2, r1) :: visited),
            calcLongestRoadLengthRecur(playerId, (v1, r2) :: stack.tail, Edge(v2, r2) :: visited)
          ).max

        case (l :: Nil, Nil) =>
          calcLongestRoadLengthRecur(playerId, (l, v2) :: stack.tail, Edge(l, v1) :: visited)

        case (l :: Nil, r :: Nil) =>
          calcLongestRoadLengthRecur(playerId, (l, r) :: stack.tail, Edge(r, v2) :: Edge(l, v1) :: visited)

        case (l :: Nil, r1 :: r2 :: Nil) =>
          Seq(
            calcLongestRoadLengthRecur(playerId, (l, r1) :: stack.tail, Edge(r1, v2) :: Edge(l, v1) :: visited),
            calcLongestRoadLengthRecur(playerId, (l, r2) :: stack.tail, Edge(r2, v2) :: Edge(l, v1) :: visited)
          ).max
        // see road scenario4

        case (l1 :: l2 :: Nil, Nil) =>
          Seq(
            calcLongestRoadLengthRecur(playerId, (l1, v2) :: stack.tail, Edge(l1, v1) :: visited),
            calcLongestRoadLengthRecur(playerId, (l2, v2) :: stack.tail, Edge(l2, v1) :: visited)
          ).max

        case (l1 :: l2 :: Nil, r :: Nil) =>
          Seq(
            calcLongestRoadLengthRecur(playerId, (l1, r) :: stack.tail, Edge(l1, v1) :: Edge(r, v2) :: visited),
            calcLongestRoadLengthRecur(playerId, (l2, r) :: stack.tail, Edge(l2, v1) :: Edge(r, v2) :: visited)
          ).max

        case (l1 :: l2 :: Nil, r1 :: r2 :: Nil) =>
          Seq(
            calcLongestRoadLengthRecur(playerId, (l1, r1) :: stack.tail, Edge(l1, v1) :: Edge(r1, v2) :: visited),
            calcLongestRoadLengthRecur(playerId, (l2, r2) :: stack.tail, Edge(l2, v1) :: Edge(r2, v2) :: visited),
            calcLongestRoadLengthRecur(playerId, (l1, r2) :: stack.tail, Edge(l1, v1) :: Edge(r2, v2) :: visited),
            calcLongestRoadLengthRecur(playerId, (l2, r1) :: stack.tail, Edge(l2, v1) :: Edge(r1, v2) :: visited)
          ).max
        case _                                  => visited.length
      }
    }
  }
}
