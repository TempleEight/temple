CREATE TABLE simple_temple_test_user (
  simpleTempleTestUser TEXT NOT NULL,
  email VARCHAR(40) CHECK (length(email) >= 5) NOT NULL,
  firstName TEXT NOT NULL,
  lastName TEXT NOT NULL,
  createdAt TIMESTAMPTZ NOT NULL,
  numberOfDogs INT NOT NULL,
  yeets BOOLEAN UNIQUE NOT NULL,
  currentBankBalance REAL CHECK (currentBankBalance >= 0.0) NOT NULL,
  birthDate DATE NOT NULL,
  breakfastTime TIME NOT NULL,
  id UUID NOT NULL PRIMARY KEY
);

CREATE TABLE fred (
  field TEXT,
  friend UUID NOT NULL,
  image BYTEA CHECK (octet_length(image) <= 10000000) NOT NULL,
  id UUID NOT NULL PRIMARY KEY
);