-- Database: timerdb

-- DROP DATABASE timerdb;

CREATE DATABASE timerdb
    WITH
    OWNER = timer
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Table: public.timer

-- DROP TABLE public.timer;

CREATE TABLE public.timer
(
    id uuid NOT NULL,
    name character varying(256) COLLATE pg_catalog."default" NOT NULL,
    start timestamp without time zone NOT NULL,
    stop timestamp without time zone,
    zoneid character varying(256) COLLATE pg_catalog."default" NOT NULL,
    description text COLLATE pg_catalog."default",
    CONSTRAINT timer_pkey PRIMARY KEY (id)
)
    WITH (
        OIDS = FALSE
    )
    TABLESPACE pg_default;

ALTER TABLE public.timer
    OWNER to timer;

-- Index: nameIdx

-- DROP INDEX public."nameIdx";

CREATE INDEX "nameIdx"
    ON public.timer USING btree
        (name COLLATE pg_catalog."default")
    TABLESPACE pg_default;