package temple.errors

trait ErrorHandlingContext extends FailureContext {
  override def fail(msg: String): Nothing
}
