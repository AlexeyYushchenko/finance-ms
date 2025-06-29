--liquibase formatted sql

--changeset ayushchenko:1
CREATE INDEX idx_currency_enabled
    ON currency(enabled);

--changeset ayushchenko:2
CREATE INDEX idx_stloc_language_code
    ON service_type_localization(language_code);

--changeset ayushchenko:3
CREATE INDEX idx_ptloc_language_code
    ON payment_type_localization(language_code);

--changeset ayushchenko:4
CREATE INDEX idx_isloc_language_code
    ON invoice_status_localization(language_code);

--changeset ayushchenko:5
CREATE INDEX idx_payment_partner_id
    ON payment(partner_id);

--changeset ayushchenko:6
CREATE INDEX idx_invoice_partner_id
    ON invoice(partner_id);

--changeset ayushchenko:7
CREATE INDEX idx_ledger_partner_id
    ON transaction_ledger(partner_id);

--changeset ayushchenko:8
CREATE INDEX idx_payment_currency_id
    ON payment(currency_id);

--changeset ayushchenko:9
CREATE INDEX idx_invoice_currency_id
    ON invoice(currency_id);

--changeset ayushchenko:10
CREATE INDEX idx_ledger_currency_id
    ON transaction_ledger(currency_id);

--changeset ayushchenko:11
CREATE INDEX idx_payment_type_id
    ON payment(payment_type_id);

--changeset ayushchenko:12
CREATE INDEX idx_payment_status_id
    ON payment(payment_status_id);

--changeset ayushchenko:13
CREATE INDEX idx_invoice_service_type_id
    ON invoice(service_type_id);

--changeset ayushchenko:14
CREATE INDEX idx_invoice_status_id
    ON invoice(status_id);

--changeset ayushchenko:15
CREATE INDEX idx_ledger_reference_type_id
    ON transaction_ledger(reference_type_id);

--changeset ayushchenko:16
CREATE INDEX idx_ledger_payment_id
    ON transaction_ledger(payment_id);

--changeset ayushchenko:17
CREATE INDEX idx_ledger_invoice_id
    ON transaction_ledger(invoice_id);

--changeset ayushchenko:18
CREATE INDEX idx_payment_date
    ON payment(payment_date);

--changeset ayushchenko:19
CREATE INDEX idx_invoice_issue_date
    ON invoice(issue_date);

--changeset ayushchenko:20
CREATE INDEX idx_invoice_due_date
    ON invoice(due_date);

--changeset ayushchenko:21
CREATE INDEX idx_ledger_transaction_date
    ON transaction_ledger(transaction_date);

--changeset ayushchenko:22
CREATE INDEX idx_exchangerate_rate_date
    ON exchange_rate(rate_date);

--changeset ayushchenko:23
CREATE INDEX idx_rateupdate_update_date
    ON rate_update_log(update_date);