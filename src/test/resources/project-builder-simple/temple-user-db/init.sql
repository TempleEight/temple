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