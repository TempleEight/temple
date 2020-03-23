package temple.errors

trait Context[+E <: Exception] extends ErrorHandlingContext[E] {
  def :+(string: String): Context[E]
  def apply[T](f: T => Context[E] => Unit)(name: String, t: T): Unit = f(t)(this :+ name)
}
