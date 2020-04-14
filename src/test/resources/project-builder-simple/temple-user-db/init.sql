CREATE TABLE temple_user (
  id UUID NOT NULL PRIMARY KEY,
  int_field INT NOT NULL,
  double_field DOUBLE PRECISION NOT NULL,
  string_field TEXT NOT NULL,
  bool_field BOOLEAN NOT NULL,
  date_field DATE NOT NULL,
  time_field TIME NOT NULL,
  date_time_field TIMESTAMPTZ NOT NULL,
  blob_field BYTEA NOT NULL
);
