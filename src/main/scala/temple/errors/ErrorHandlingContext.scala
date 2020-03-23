package temple.errors

trait ErrorHandlingContext[+E <: Exception] extends FailureContext {
  def error(msg: String): E
  final override def fail(msg: String): Nothing = throw error(msg)
}
