package game

import scala.quoted.*

// The type class/trait that proves A is inside B
trait ComponentOf[A, B]

object ComponentOf:
  // Macro entry point
  inline given derive[A, B]: ComponentOf[A, B] = ${ deriveImpl[A, B] }

  def deriveImpl[A: Type, B: Type](using Quotes): Expr[ComponentOf[A, B]] =
    import quotes.reflect.*

    // Helper function to recursively flatten union types (OrType)
    def flattenUnion(t: TypeRepr): List[TypeRepr] = t match
      case OrType(left, right) => flattenUnion(left) ++ flattenUnion(right)
      case other => List(other)

    val targetTpe = TypeRepr.of[A]
    val unionComponents = flattenUnion(TypeRepr.of[B])

    // Check if our target type matches any of the components in the union
    val isPart = unionComponents.exists(comp => comp =:= targetTpe)

    if (isPart) then
      '{ new ComponentOf[A, B] {} }
    else
      report.errorAndAbort(
        s"Type ${Type.show[A]} is not a member of the union type ${Type.show[B]}"
        )
