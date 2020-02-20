package temple

object SimpleEndToEndTestData {

  val createStatement: String =
    """CREATE TABLE User (
      |  username TEXT NOT NULL,
      |  email VARCHAR(40) CHECK (length(email) >= 5) NOT NULL,
      |  firstName TEXT NOT NULL,
      |  lastName TEXT NOT NULL,
      |  createdAt TIMESTAMPTZ NOT NULL,
      |  numberOfDogs INT NOT NULL,
      |  yeets BOOLEAN UNIQUE NOT NULL,
      |  currentBankBalance REAL CHECK (currentBankBalance >= 0.0) NOT NULL,
      |  birthDate DATE NOT NULL,
      |  breakfastTime TIME NOT NULL
      |);
      |
      |CREATE TABLE Fred (
      |  field TEXT,
      |  friend INT NOT NULL
      |);""".stripMargin
}
