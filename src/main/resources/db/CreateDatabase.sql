-- CREATE database, schemas, roles and users
CREATE DATABASE unique_visitors_db;

CREATE ROLE query_executor;
CREATE ROLE query_reader;

GRANT CONNECT ON DATABASE unique_visitors_db to query_executor;
GRANT CONNECT ON DATABASE unique_visitors_db to query_reader;

CREATE USER unique_visitors_user with password 'unique_visitors_pass';

GRANT query_executor to unique_visitors_user;
GRANT query_reader to unique_visitors_user;

-- IMPORTANT: RECONNECT to "unique_visitors_db" before executing the rest of the script
-- PostgreSQL doesn't have 'use database' commands in SQL
CREATE SCHEMA unique_visitors;

SET SCHEMA 'unique_visitors';

-- CREATE table for storing unique counts per scope
CREATE SEQUENCE unique_visitors.unique_counts_seq
 INCREMENT BY 1
 MINVALUE 1
 NO MAXVALUE
 CACHE 1
 NO CYCLE
 OWNED BY NONE;

CREATE TABLE visits_count (
    id BIGINT           DEFAULT nextval('unique_visitors.unique_counts_seq'::regclass) NOT NULL,
    scope               TEXT NOT NULL,
    unq_count           INT NOT NULL DEFAULT 0,
    last_update_date    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE visits_count ADD CONSTRAINT visits_count_pkey PRIMARY KEY (id);

-- POPULATE table with default scopes
INSERT INTO visits_count(scope)
VALUES
('last 5 seconds'),
('last 1 minute'),
('last 5 minutes'),
('last 30 minutes'),
('last 1 hour'),
('last 1 day');

-- GRANT PERMISSIONS
GRANT USAGE ON SCHEMA unique_visitors to query_reader;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA unique_visitors to query_executor;
GRANT SELECT,USAGE ON ALL SEQUENCES IN SCHEMA unique_visitors TO query_reader;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA unique_visitors TO query_reader;
