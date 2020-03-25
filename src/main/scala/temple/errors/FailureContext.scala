package temple.errors

trait FailureContext {
  def fail(msg: String): Unit
}
