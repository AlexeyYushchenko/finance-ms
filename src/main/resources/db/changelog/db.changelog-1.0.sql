--liquibase formatted sql

--changeset ayushchenko:1
CREATE TABLE IF NOT EXISTS currency
(
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(4) NOT NULL UNIQUE,
    okv_code    VARCHAR(3) NOT NULL UNIQUE,
    name        VARCHAR(50),
    enabled     BOOLEAN,

    created_at  TIMESTAMP,
    modified_at TIMESTAMP,
    created_by  VARCHAR(64),
    modified_by VARCHAR(64)
);

--changeset ayushchenko:2
CREATE TABLE IF NOT EXISTS service_type
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,

    created_at  TIMESTAMP DEFAULT NOW(),
    modified_at TIMESTAMP DEFAULT NOW(),
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
    UNIQUE (language_code, localized_name)
);

--changeset ayushchenko:4
CREATE TABLE IF NOT EXISTS payment_type
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,

    created_at  TIMESTAMP DEFAULT NOW(),
    modified_at TIMESTAMP DEFAULT NOW(),
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
    UNIQUE (language_code, localized_name)
);

--changeset ayushchenko:6
CREATE TABLE IF NOT EXISTS payment
(
    id                 BIGSERIAL PRIMARY KEY,
    client_id          INT                              NOT NULL,
    amount             DECIMAL(10, 2)                   NOT NULL,
    currency_id        INT REFERENCES currency (id)     NOT NULL,
    payment_date       DATE                             NOT NULL,
    payment_type_id    INT REFERENCES payment_type (id) NOT NULL,
    processing_fees    DECIMAL(10, 2)                   NOT NULL,
    total_amount       DECIMAL(10, 2),
    unallocated_amount DECIMAL(10, 2),
    commentary         VARCHAR(255),

    version            BIGINT                           NOT NULL DEFAULT 0,

    created_at         TIMESTAMP,
    modified_at        TIMESTAMP,
    created_by         VARCHAR(64),
    modified_by        VARCHAR(64)
);

--changeset ayushchenko:7
CREATE TABLE IF NOT EXISTS invoice_status
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,

    created_at  TIMESTAMP DEFAULT NOW(),
    modified_at TIMESTAMP DEFAULT NOW(),
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
    UNIQUE (language_code, localized_name)
);

--changeset ayushchenko:9
CREATE TABLE IF NOT EXISTS invoice
(
    id              BIGSERIAL PRIMARY KEY,
    client_id       INT                                   NOT NULL,
    service_type_id INT REFERENCES service_type (id)      NOT NULL,
    shipment_id     BIGINT,
    total_amount    DECIMAL(10, 2)                        NOT NULL,
    currency_id     INT REFERENCES currency (id)          NOT NULL,
    status_id       BIGINT REFERENCES invoice_status (id) NOT NULL,
    issue_date      DATE                                  NOT NULL,
    due_date        DATE,
    commentary      TEXT,
    version         BIGINT                                NOT NULL DEFAULT 0,

    created_at      TIMESTAMP,
    modified_at     TIMESTAMP,
    created_by      VARCHAR(64),
    modified_by     VARCHAR(64)
);

--changeset ayushchenko:10
CREATE TABLE IF NOT EXISTS payment_invoice
(
    id               BIGSERIAL PRIMARY KEY,
    version          BIGINT                                           NOT NULL DEFAULT 0,
    payment_id       BIGINT REFERENCES payment (id) ON DELETE CASCADE NOT NULL,
    invoice_id       BIGINT REFERENCES invoice (id) ON DELETE CASCADE NOT NULL,
    allocated_amount DECIMAL(10, 2), -- Original amount in payment's currency
    converted_amount DECIMAL(10, 2), -- Amount after conversion to invoice's currency
    currency_from_id INT REFERENCES currency (id)                     NOT NULL,
    currency_to_id   INT REFERENCES currency (id)                     NOT NULL,
    exchange_rate    DECIMAL(10, 6),

    created_at       TIMESTAMP,
    modified_at      TIMESTAMP,
    created_by       VARCHAR(64),
    modified_by      VARCHAR(64),

    UNIQUE (payment_id, invoice_id)
);

--changeset ayushchenko:11
CREATE TABLE IF NOT EXISTS client_balance
(
    id          BIGSERIAL PRIMARY KEY,
    client_id   INT    NOT NULL,
    currency_id INT    NOT NULL REFERENCES currency (id),
    balance     DECIMAL(15, 2),
    version     BIGINT NOT NULL DEFAULT 0,

    created_at  TIMESTAMP,
    modified_at TIMESTAMP,
    created_by  VARCHAR(64),
    modified_by VARCHAR(64),

    UNIQUE (client_id, currency_id)
);

--changeset ayushchenko:12
CREATE TABLE IF NOT EXISTS exchange_rate
(
    id                  BIGSERIAL PRIMARY KEY,
    currency_from_id    INT            NOT NULL REFERENCES currency (id),
    currency_to_id      INT            NOT NULL REFERENCES currency (id),
    official_rate       DECIMAL(15, 6) NOT NULL, -- Official exchange rate
    standard_rate       DECIMAL(15, 6) NOT NULL, -- Rate after applying your standard margin
    premium_client_rate DECIMAL(15, 6) NOT NULL, -- Rate for premium clients with lowered margin
    rate_date           DATE           NOT NULL,

    created_at          TIMESTAMP,
    modified_at         TIMESTAMP,
    created_by          VARCHAR(64),
    modified_by         VARCHAR(64),

    UNIQUE (currency_from_id, currency_to_id, rate_date)
);

--changeset ayushchenko:13
CREATE TABLE IF NOT EXISTS rate_update_log
(
    id          SERIAL PRIMARY KEY,
    update_date DATE    NOT NULL,
    status      BOOLEAN NOT NULL DEFAULT FALSE, -- Indicates whether the rates have been updated successfully
    created_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

--changeset ayushchenko:14
CREATE OR REPLACE VIEW payment_allocation_view AS
SELECT p.id,
       p.total_amount,
       COALESCE(SUM(pi.allocated_amount), 0)                  AS allocated_amount,
       p.total_amount - COALESCE(SUM(pi.allocated_amount), 0) AS unallocated_amount,
       CASE
           WHEN p.total_amount - COALESCE(SUM(pi.allocated_amount), 0) = 0 THEN TRUE
           ELSE FALSE END                                     AS is_fully_allocated
FROM payment p
         LEFT JOIN payment_invoice pi ON p.id = pi.payment_id
GROUP BY p.id, p.total_amount;



