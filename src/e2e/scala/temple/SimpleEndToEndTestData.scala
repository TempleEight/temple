package temple

object SimpleEndToEndTestData {

  val createStatement: String =
    """CREATE TABLE User (
      |  username TEXT,
      |  email VARCHAR(40) CHECK (length(email) >= 5),
      |  firstName TEXT,
      |  lastName TEXT,
      |  createdAt TIMESTAMPTZ,
      |  numberOfDogs INT,
      |  yeets BOOLEAN UNIQUE,
      |  currentBankBalance REAL CHECK (currentBankBalance >= 0.0),
      |  birthDate DATE,
      |  breakfastTime TIME
      |);
      |
      |CREATE TABLE Fred (
      |  field TEXT
      |);""".stripMargin
}
