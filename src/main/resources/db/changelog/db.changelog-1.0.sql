--changeset ayushchenko:1
CREATE TABLE IF NOT EXISTS currency
(
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(4)              NOT NULL UNIQUE,
    okv_code    VARCHAR(3)              NOT NULL UNIQUE,
    name        VARCHAR(50),
    enabled     BOOLEAN,

    created_at  TIMESTAMP DEFAULT NOW() NOT NULL,
    modified_at TIMESTAMP DEFAULT NOW() NOT NULL,
    created_by  VARCHAR(64),
    modified_by VARCHAR(64)
);

--changeset ayushchenko:2
CREATE TABLE IF NOT EXISTS service_type
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50)             NOT NULL UNIQUE,
    description TEXT,

    created_at  TIMESTAMP DEFAULT NOW() NOT NULL,
    modified_at TIMESTAMP DEFAULT NOW() NOT NULL,
    created_by  VARCHAR(64),
    modified_by VARCHAR(64)
);

--changeset ayushchenko:3
CREATE TABLE IF NOT EXISTS service_type_localization
(
    service_type_id       INT REFERENCES service_type (id) ON DELETE CASCADE,
    language_code         VARCHAR(5),
    localized_name        VARCHAR(255) NOT NULL,
    localized_description TEXT,
    PRIMARY KEY (service_type_id, language_code),
    UNIQUE (service_type_id, language_code, localized_name)
);

--changeset ayushchenko:4
CREATE TABLE IF NOT EXISTS payment_type
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50)             NOT NULL UNIQUE,
    description TEXT,

    created_at  TIMESTAMP DEFAULT NOW() NOT NULL,
    modified_at TIMESTAMP DEFAULT NOW() NOT NULL,
    created_by  VARCHAR(64),
    modified_by VARCHAR(64)
);

--changeset ayushchenko:5
CREATE TABLE IF NOT EXISTS payment_type_localization
(
    payment_type_id       INT REFERENCES payment_type (id) ON DELETE CASCADE,
    language_code         VARCHAR(5),
    localized_name        VARCHAR(255) NOT NULL,
    localized_description TEXT,
    PRIMARY KEY (payment_type_id, language_code),
    UNIQUE (payment_type_id, language_code, localized_name)
);

--changeset ayushchenko:6
CREATE TABLE IF NOT EXISTS invoice_status
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255)            NOT NULL UNIQUE,

    created_at  TIMESTAMP DEFAULT NOW() NOT NULL,
    modified_at TIMESTAMP DEFAULT NOW() NOT NULL,
    created_by  VARCHAR(64),
    modified_by VARCHAR(64)
);

--changeset ayushchenko:7
CREATE TABLE IF NOT EXISTS payment_status
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255)            NOT NULL UNIQUE,

    created_at  TIMESTAMP DEFAULT NOW() NOT NULL,
    modified_at TIMESTAMP DEFAULT NOW() NOT NULL,
    created_by  VARCHAR(64),
    modified_by VARCHAR(64)
);

--changeset ayushchenko:8
CREATE TABLE IF NOT EXISTS invoice_status_localization
(
    invoice_status_id INT REFERENCES invoice_status (id) ON DELETE CASCADE,
    language_code     VARCHAR(5),
    localized_name    VARCHAR(255) NOT NULL,
    PRIMARY KEY (invoice_status_id, language_code),
    UNIQUE (invoice_status_id, language_code, localized_name)
);

--changeset ayushchenko:9
CREATE TABLE IF NOT EXISTS reference_type
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50)             NOT NULL UNIQUE,

    created_at  TIMESTAMP DEFAULT NOW() NOT NULL,
    modified_at TIMESTAMP DEFAULT NOW() NOT NULL,
    created_by  VARCHAR(64),
    modified_by VARCHAR(64)
);

--changeset ayushchenko:10
CREATE TABLE IF NOT EXISTS payment
(
    id                 BIGSERIAL PRIMARY KEY,
    partner_id         BIGINT                             NOT NULL,
    amount             DECIMAL(10, 2)                     NOT NULL,
    currency_id        INT REFERENCES currency (id)       NOT NULL,
    payment_date       DATE                               NOT NULL,
    payment_type_id    INT REFERENCES payment_type (id)   NOT NULL,
    processing_fees    DECIMAL(10, 2)                     NOT NULL,
    total_amount       DECIMAL(10, 2),
    unallocated_amount DECIMAL(10, 2)                              DEFAULT 0,
    payment_status_id  INT REFERENCES payment_status (id) NOT NULL DEFAULT 1,
    commentary         VARCHAR(255),

    version            BIGINT                             NOT NULL DEFAULT 0,

    created_at         TIMESTAMP                                   DEFAULT NOW() NOT NULL,
    modified_at        TIMESTAMP                                   DEFAULT NOW() NOT NULL,
    created_by         VARCHAR(64),
    modified_by        VARCHAR(64)
);

--changeset ayushchenko:11
CREATE TYPE invoice_direction AS ENUM ('RECEIVABLE', 'PAYABLE');

--changeset ayushchenko:12
CREATE TABLE IF NOT EXISTS invoice
(
    id              BIGSERIAL PRIMARY KEY,
    direction       varchar(20)                        NOT NULL DEFAULT 'RECEIVABLE',
    partner_id      BIGINT                             NOT NULL,
    service_type_id INT REFERENCES service_type (id)   NOT NULL,
    shipment_id     BIGINT,
    total_amount    DECIMAL(10, 2)                     NOT NULL,
    paid_amount     DECIMAL(10, 2)                              DEFAULT 0,
    currency_id     INT REFERENCES currency (id)       NOT NULL,
    status_id       INT REFERENCES invoice_status (id) NOT NULL,
    issue_date      DATE                               NOT NULL,
    due_date        DATE,
    commentary      TEXT,
    version         BIGINT                             NOT NULL DEFAULT 0,

    created_at      TIMESTAMP                                   DEFAULT NOW() NOT NULL,
    modified_at     TIMESTAMP                                   DEFAULT NOW() NOT NULL,
    created_by      VARCHAR(64),
    modified_by     VARCHAR(64)
);

--changeset ayushchenko:13
CREATE TABLE IF NOT EXISTS transaction_ledger
(
    id                BIGSERIAL PRIMARY KEY,
    partner_id        BIGINT                             NOT NULL,
    currency_id       INT REFERENCES currency (id)       NOT NULL,
    amount            DECIMAL(15, 2)                     NOT NULL,
    base_amount       DECIMAL(15, 2)                     NOT NULL,
    reference_type_id INT REFERENCES reference_type (id) NOT NULL,
    payment_id        BIGINT REFERENCES payment (id),
    invoice_id        BIGINT REFERENCES invoice (id),
    transaction_date  DATE                               NOT NULL,
    version           BIGINT                             NOT NULL DEFAULT 0,

    created_at        TIMESTAMP DEFAULT NOW()            NOT NULL,
    modified_at       TIMESTAMP DEFAULT NOW()            NOT NULL,
    created_by        VARCHAR(64),
    modified_by       VARCHAR(64)
);

--changeset ayushchenko:14
CREATE TABLE IF NOT EXISTS exchange_rate
(
    id                  BIGSERIAL PRIMARY KEY,
    currency_from_id    INT                     NOT NULL REFERENCES currency (id),
    currency_to_id      INT                     NOT NULL REFERENCES currency (id),
    official_rate       DECIMAL(15, 6)          NOT NULL,
    standard_rate       DECIMAL(15, 6)          NOT NULL,
    premium_client_rate DECIMAL(15, 6)          NOT NULL,
    rate_date           DATE                    NOT NULL,

    created_at          TIMESTAMP DEFAULT NOW() NOT NULL,
    modified_at         TIMESTAMP DEFAULT NOW() NOT NULL,
    created_by          VARCHAR(64),
    modified_by         VARCHAR(64),

    UNIQUE (currency_from_id, currency_to_id, rate_date)
);

--changeset ayushchenko:15
CREATE TABLE IF NOT EXISTS rate_update_log
(
    id          SERIAL PRIMARY KEY,
    update_date DATE    NOT NULL,
    status      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

--changeset you:2025-06-30-invoice_cancel_guard_v2
------------------------------------------------------------------------
--  Block *any* UPDATE or DELETE once an invoice is already “cancelled”
------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION prevent_invoice_modify_when_cancelled()
    RETURNS trigger
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF OLD.status_id = 6 THEN
        RAISE EXCEPTION
            'Invoice % is cancelled and cannot be modified or deleted',
            OLD.id;
    END IF;

    -- DELETE returns OLD, UPDATE returns NEW
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS trg_invoice_no_modify_when_cancelled ON invoice;
CREATE TRIGGER trg_invoice_no_modify_when_cancelled
    BEFORE UPDATE OR DELETE --  ❗ both verbs covered
    ON invoice
    FOR EACH ROW
EXECUTE FUNCTION prevent_invoice_modify_when_cancelled();
--rollback DROP TRIGGER IF EXISTS trg_invoice_no_modify_when_cancelled ON invoice;
--rollback DROP FUNCTION IF EXISTS prevent_invoice_modify_when_cancelled();


--changeset you:2025-06-30-payment_cancel_guard_v2
------------------------------------------------------------------------
--  Block *any* UPDATE or DELETE once a payment is already “cancelled”
------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION prevent_payment_modify_when_cancelled()
    RETURNS trigger
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF OLD.payment_status_id = 2 THEN
        RAISE EXCEPTION
            'Payment % is cancelled and cannot be modified or deleted',
            OLD.id;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS trg_payment_no_modify_when_cancelled ON payment;
CREATE TRIGGER trg_payment_no_modify_when_cancelled
    BEFORE UPDATE OR DELETE
    ON payment
    FOR EACH ROW
EXECUTE FUNCTION prevent_payment_modify_when_cancelled();
--rollback DROP TRIGGER IF EXISTS trg_payment_no_modify_when_cancelled ON payment;
--rollback DROP FUNCTION IF EXISTS prevent_payment_modify_when_cancelled();


--changeset your.name:20250629-create-outbox_event
------------------------------------------------------------------------
--  Outbox table for guaranteed event delivery
------------------------------------------------------------------------

CREATE TABLE outbox_event
(
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type varchar(50)  NOT NULL,
    aggregate_id   bigint       NOT NULL,
    event_type     varchar(100) NOT NULL,
    payload        jsonb        NOT NULL,
    metadata       jsonb        NULL,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    processed      boolean      NOT NULL DEFAULT false,
    processed_at   timestamptz  NULL
);

CREATE INDEX idx_outbox_unprocessed
    ON outbox_event (processed, created_at)
    WHERE processed = false;

