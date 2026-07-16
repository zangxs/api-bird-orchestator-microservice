-- =========================================
-- ENUM: estados del ciclo de vida de una imagen
-- =========================================
CREATE TYPE image_status AS ENUM (
    'PROCESSING',
    'NOT_A_BIRD',
    'BIRD_DETECTED',
    'IDENTIFYING',
    'DONE',
    'FAILED',
    'PENDING_REPLACE_CONFIRMATION',
    'REPLACED',
    'REPLACE_REJECTED',
    'EXPIRED'
);

-- =========================================
-- Tabla: species (catálogo global)
-- =========================================
CREATE TABLE species (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    common_name       VARCHAR(150) NOT NULL,
    common_name_en    VARCHAR(150),
    scientific_name   VARCHAR(150) NOT NULL,
    family            VARCHAR(150),
    external_id       VARCHAR(100),
    image_url         VARCHAR(500),
    description       TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_species_scientific_name UNIQUE (scientific_name)
);

CREATE INDEX idx_species_external_id ON species (external_id);

-- =========================================
-- Tabla: image_event
-- =========================================
CREATE TABLE image_event (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    s3_key              VARCHAR(500) NOT NULL,
    status              image_status NOT NULL DEFAULT 'PROCESSING',
    bird_confidence     DECIMAL(5,4),
    species_id          UUID REFERENCES species (id),
    species_confidence  DECIMAL(5,4),
    latitude            DECIMAL(9,6),
    longitude           DECIMAL(9,6),
    failure_reason      VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ

    -- FK a users se agrega cuando exista la tabla users:
    -- CONSTRAINT fk_image_event_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_image_event_user_id ON image_event (user_id);
CREATE INDEX idx_image_event_status ON image_event (status);
CREATE INDEX idx_image_event_species_id ON image_event (species_id);

-- Trigger para mantener updated_at automático
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_image_event_updated_at
BEFORE UPDATE ON image_event
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();


ALTER TABLE image_event
    ALTER COLUMN status TYPE VARCHAR(40),
    ADD CONSTRAINT chk_image_event_status CHECK (status IN (
        'PROCESSING', 'NOT_A_BIRD', 'BIRD_DETECTED', 'IDENTIFYING', 'DONE', 'FAILED',
        'PENDING_REPLACE_CONFIRMATION', 'REPLACED', 'REPLACE_REJECTED', 'EXPIRED'
    ));