package temple.generate.server.go

object GoHTTPStatus extends Enumeration {
  type GoHTTPStatus = Value
  val StatusBadRequest, StatusUnauthorized, StatusNotFound, StatusInternalServerError = Value
}
