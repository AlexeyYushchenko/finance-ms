--liquibase formatted sql

--changeset ayushchenko:1
CREATE TABLE IF NOT EXISTS currency
(
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(3) NOT NULL,
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
    service_type_id      INT REFERENCES service_type (id) ON DELETE CASCADE,
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
    payment_type_id      INT REFERENCES payment_type (id) ON DELETE CASCADE,
    language_code         VARCHAR(5),
    localized_name        VARCHAR(255) NOT NULL,
    localized_description TEXT,
    PRIMARY KEY (payment_type_id, language_code),
    UNIQUE (language_code, localized_name)
);

--changeset ayushchenko:6
CREATE TABLE IF NOT EXISTS payment
(
    id                      BIGSERIAL PRIMARY KEY,
    client_id               INT  NOT NULL,
    amount                  DECIMAL(10, 2)                   NOT NULL,
    currency_id             INT REFERENCES currency (id)     NOT NULL,
    payment_date            DATE                             NOT NULL,
    payment_type_id         INT REFERENCES payment_type (id) NOT NULL,
    payment_processing_fees DECIMAL(10, 2),
    total_amount            DECIMAL(10, 2),

    commentary              VARCHAR(255),

    created_at              TIMESTAMP,
    modified_at             TIMESTAMP,
    created_by              VARCHAR(64),
    modified_by             VARCHAR(64)
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
    client_id       INT            NOT NULL,
    service_type_id INT REFERENCES service_type (id)      NOT NULL,
    shipment_id     BIGINT,
    total_amount    DECIMAL(10, 2)                        NOT NULL,
    currency_id     INT REFERENCES currency (id)          NOT NULL,
    status_id       BIGINT REFERENCES invoice_status (id) NOT NULL,
    issue_date      DATE                                  NOT NULL,
    due_date        DATE,
    commentary      TEXT,

    created_at      TIMESTAMP,
    modified_at     TIMESTAMP,
    created_by      VARCHAR(64),
    modified_by     VARCHAR(64)
);

--changeset ayushchenko:10
CREATE TABLE IF NOT EXISTS payment_invoice
(
    id               BIGSERIAL PRIMARY KEY,
    payment_id       BIGINT REFERENCES payment (id) ON DELETE CASCADE NOT NULL,
    invoice_id       BIGINT REFERENCES invoice (id) ON DELETE CASCADE NOT NULL,
    allocated_amount DECIMAL(10, 2), -- Original amount in payment's currency
    converted_amount DECIMAL(10, 2), -- Amount after conversion to invoice's currency
    currency_from_id INT REFERENCES currency (id)                     NOT NULL,
    currency_to_id   INT REFERENCES currency (id)                     NOT NULL,
    exchange_rate    DECIMAL(10, 6),
    UNIQUE (payment_id, invoice_id)
);

--changeset ayushchenko:11
CREATE TABLE IF NOT EXISTS client_balance
(
    id          BIGSERIAL PRIMARY KEY,
    client_id   INT NOT NULL,
    currency_id INT NOT NULL REFERENCES currency (id),
    balance     DECIMAL(15, 2),
    UNIQUE (client_id, currency_id)
);

-- добавить db с историей курсов