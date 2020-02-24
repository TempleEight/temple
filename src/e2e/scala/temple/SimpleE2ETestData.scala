package temple

object SimpleE2ETestData {

  val createStatement: String =
    """CREATE TABLE temple_user (
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
      |CREATE TABLE fred (
      |  field TEXT,
      |  friend INT NOT NULL
      |);""".stripMargin

  val dockerfile: String =
    """FROM golang:1.13.7-alpine
      |
      |WORKDIR /templeuser
      |
      |COPY go.mod go.sum ./
      |
      |RUN ["go", "mod", "download"]
      |
      |COPY . .
      |
      |COPY config.json /etc/templeuser-service
      |
      |RUN ["go", "build", "-o", "templeuser"]
      |
      |ENTRYPOINT ["./templeuser"]
      |
      |EXPOSE 80
      |""".stripMargin
}
