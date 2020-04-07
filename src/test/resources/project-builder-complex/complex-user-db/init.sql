CREATE TABLE complex_user (
  id UUID NOT NULL PRIMARY KEY,
  smallIntField SMALLINT CHECK (smallIntField <= 100) CHECK (smallIntField >= 10) NOT NULL,
  intField INT CHECK (intField <= 100) CHECK (intField >= 10) NOT NULL,
  bigIntField BIGINT CHECK (bigIntField <= 100) CHECK (bigIntField >= 10) NOT NULL,
  floatField REAL CHECK (floatField <= 300.0) CHECK (floatField >= 0.0) NOT NULL,
  doubleField DOUBLE PRECISION CHECK (doubleField <= 123.0) CHECK (doubleField >= 0.0) NOT NULL,
  stringField TEXT CHECK (length(stringField) >= 1) NOT NULL,
  boundedStringField VARCHAR(5) CHECK (length(boundedStringField) >= 0) NOT NULL,
  boolField BOOLEAN NOT NULL,
  dateField DATE NOT NULL,
  timeField TIME NOT NULL,
  dateTimeField TIMESTAMPTZ NOT NULL,
  blobField BYTEA NOT NULL
);

CREATE TABLE temple_user (
  id UUID NOT NULL PRIMARY KEY,
  intField INT NOT NULL,
  doubleField DOUBLE PRECISION NOT NULL,
  stringField TEXT NOT NULL,
  boolField BOOLEAN NOT NULL,
  dateField DATE NOT NULL,
  timeField TIME NOT NULL,
  dateTimeField TIMESTAMPTZ NOT NULL,
  blobField BYTEA NOT NULL
);
