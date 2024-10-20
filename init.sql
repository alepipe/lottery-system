CREATE TABLE IF NOT EXISTS principal (
    id uuid PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS lottery (
    id uuid PRIMARY KEY,
    end_date date NOT NULL,
    winner_ballot_id uuid
);

CREATE TABLE IF NOT EXISTS ballot (
    id uuid PRIMARY KEY,
    lottery_id uuid  REFERENCES lottery NOT NULL,
    principal_id uuid REFERENCES principal NOT NULL
);

ALTER TABLE lottery ADD FOREIGN KEY (winner_ballot_id) REFERENCES ballot;
