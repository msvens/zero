CREATE DATABASE timerdb
    WITH
    OWNER = timer
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

CREATE TABLE timer (
  id SERIAL PRIMARY KEY,
  title VARCHAR(1024) NOT NULL,
  start TIMESTAMP,
  stop TIMESTAMP,
  description TEXT
);