-- Table: public.ztimer

-- DROP TABLE public.ztimer;

CREATE TABLE public.ztimer
(
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    title character varying(256) COLLATE pg_catalog."default" NOT NULL,
    start character varying COLLATE pg_catalog."default" NOT NULL,
    stop character varying COLLATE pg_catalog."default" NOT NULL,
    description text COLLATE pg_catalog."default",
    CONSTRAINT ztimer_pkey PRIMARY KEY (id)
)
    WITH (
        OIDS = FALSE
    )
    TABLESPACE pg_default;

ALTER TABLE public.ztimer
    OWNER to timer;

-- Index: title_idx

-- DROP INDEX public.title_idx;

CREATE INDEX title_idx
    ON public.ztimer USING btree
        (title COLLATE pg_catalog."default")
    TABLESPACE pg_default;