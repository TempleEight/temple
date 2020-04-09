package temple.DSL.semantics

import scala.collection.mutable

/** Apply Tarjan’s strongly connected components algorithm to find cycles in the graph */
class Tarjan[T] private (graph: Map[T, Iterable[T]]) {

  private val stack: mutable.Stack[T]    = mutable.Stack.empty
  private val index: mutable.Map[T, Int] = mutable.Map.empty

  /** The smallest index of any node known to be reachable from v, including v itself */
  private val lowLink: mutable.Map[T, Int]      = mutable.Map.empty
  private val sccBuffer: mutable.Buffer[Seq[T]] = mutable.Buffer.empty

  lazy val sccs: Set[Seq[T]] = {
    for (v <- graph.keys) if (!index.contains(v)) visit(v)
    sccBuffer.toSet
  }

  private def visit(node: T): Unit = {
    // Set the depth index for v to the smallest unused index
    index(node) = index.size
    lowLink(node) = index(node)
    stack.push(node)
    for (neighbour <- graph.getOrElse(node, Nil)) {
      if (!index.contains(neighbour)) { // neighbour has not yet been visited; recurse on it
        visit(neighbour)
        lowLink(node) = math.min(lowLink(node), lowLink(neighbour))
      } else if (stack.contains(neighbour)) { // neighbour is in stack S and hence in the current SCC
        lowLink(node) = math.min(lowLink(node), index(neighbour))
      } // If neighbour is not on stack, then w is already in an scc, so we ignore it
    }
    // If this node is a root node, pop the stack and generate an SCC
    if (lowLink(node) == index(node)) {
      // Pop all elements from the stack until and including this element itself
      val scc = (stack.popWhile(_ != node) :+ stack.pop()).toSeq
      sccBuffer += scc
    }
  }

}

object Tarjan {
  def apply[T](graph: Map[T, Iterable[T]]): Set[Seq[T]] = new Tarjan(graph).sccs
}
