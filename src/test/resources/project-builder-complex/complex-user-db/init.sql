CREATE TABLE complex_user (
  id UUID NOT NULL PRIMARY KEY,
  small_int_field SMALLINT CHECK (small_int_field <= 100) CHECK (small_int_field >= 10) NOT NULL,
  int_field INT CHECK (int_field <= 100) CHECK (int_field >= 10) NOT NULL,
  big_int_field BIGINT CHECK (big_int_field <= 100) CHECK (big_int_field >= 10) NOT NULL,
  float_field REAL CHECK (float_field <= 300.0) CHECK (float_field >= 0.0) NOT NULL,
  double_field DOUBLE PRECISION CHECK (double_field <= 123.0) CHECK (double_field >= 0.0) NOT NULL,
  string_field TEXT CHECK (length(string_field) >= 1) NOT NULL,
  bounded_string_field VARCHAR(5) CHECK (length(bounded_string_field) >= 0) NOT NULL,
  bool_field BOOLEAN NOT NULL,
  date_field DATE NOT NULL,
  time_field TIME NOT NULL,
  date_time_field TIMESTAMPTZ NOT NULL,
  blob_field BYTEA NOT NULL
);

CREATE TABLE temple_user (
  id UUID NOT NULL PRIMARY KEY,
  parent_id UUID NOT NULL,
  int_field INT NOT NULL,
  double_field DOUBLE PRECISION NOT NULL,
  string_field TEXT NOT NULL,
  bool_field BOOLEAN NOT NULL,
  date_field DATE NOT NULL,
  time_field TIME NOT NULL,
  date_time_field TIMESTAMPTZ NOT NULL,
  blob_field BYTEA NOT NULL
);
