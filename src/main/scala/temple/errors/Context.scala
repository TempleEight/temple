package temple.errors

trait Context extends ErrorHandlingContext {
  def :+(string: String): Context
  def apply[T](f: T => Context => Unit)(name: String, t: T): Unit = f(t)(this :+ name)
}
