package temple.generate.server.go

object GoHTTPStatus extends Enumeration {
  type GoHTTPStatus = Value
  val StatusBadRequest, StatusUnauthorized, StatusForbidden, StatusNotFound, StatusInternalServerError = Value
}
