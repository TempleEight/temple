CREATE TABLE simple_temple_test_user (
  id UUID NOT NULL PRIMARY KEY,
  simple_temple_test_log TEXT NOT NULL,
  email VARCHAR(40) CHECK (length(email) >= 5) NOT NULL,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  number_of_dogs INT NOT NULL,
  yeets BOOLEAN UNIQUE NOT NULL,
  current_bank_balance REAL CHECK (current_bank_balance >= 0.0) NOT NULL,
  birth_date DATE NOT NULL,
  breakfast_time TIME NOT NULL
);

CREATE TABLE fred (
  id UUID NOT NULL PRIMARY KEY,
  parent_id UUID NOT NULL,
  field TEXT NOT NULL,
  friend UUID NOT NULL,
  image BYTEA CHECK (octet_length(image) <= 10000000) NOT NULL
);
