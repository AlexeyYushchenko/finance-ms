--liquibase formatted sql

--changeset ayushchenko:1
INSERT INTO service_type (name, description, created_by)
VALUES ('International Delivery', 'Charging for the transportation of goods across international borders.', 'YAE'),
       ('Local Delivery', 'Fees associated with the regional transport of goods within the same country or area.',
        'YAE'),
       ('Insurance', 'Providing coverage against loss or damage during transit.', 'YAE'),
       ('Customs Clearance',
        'Charging for handling the paperwork and procedures required to clear goods through customs.', 'YAE'),
       ('Warehousing', 'Fees for storage of goods in a warehouse for a specified period.', 'YAE'),
       ('Packing and Crating',
        'Providing specialized packing services to ensure the safety and integrity of goods during transport.', 'YAE'),
       ('Freight Forwarding', 'Coordinating and arranging the entire shipping process through various carriers.',
        'YAE'),
       ('Supply Chain Consulting',
        'Charging for expert advice on optimizing supply chain operations and logistics strategies.', 'YAE'),
       ('Inventory Management',
        'Providing services to manage and track inventory levels, orders, and product deliveries.', 'YAE'),
       ('Expedited Shipping', 'Offering a premium for faster than normal delivery times.', 'YAE');

--changeset ayushchenko:2
INSERT INTO service_type_localization(service_type_id, language_code, localized_name, localized_description)
VALUES (1, 'ru', 'Международная доставка', 'Взимание платы за транспортировку товаров через международные границы'),
       (2, 'ru', 'Локальная доставка', 'Сборы, связанные с транспортировкой товаров внутри страны или региона'),
       (3, 'ru', 'Страхование', 'Предоставление страховки от потери или повреждения во время транспортировки'),
       (4, 'ru', 'Таможенное оформление',
        'Взимание платы за выполнение необходимых процедур и документации для таможенного оформления'),
       (5, 'ru', 'Складирование', 'Взимание платы за хранение товаров на складе на указанный период'),
       (6, 'ru', 'Упаковка и крейтинг',
        'Предоставление специализированных услуг по упаковке для обеспечения сохранности и целостности товаров во время транспортировки'),
       (7, 'ru', 'Экспедирование грузов',
        'Координация и организация всего процесса перевозки через различных перевозчиков'),
       (8, 'ru', 'Консультации по цепочке поставок',
        'Взимание платы за профессиональные консультации по оптимизации операций цепочки поставок и логистических стратегий'),
       (9, 'ru', 'Управление запасами',
        'Предоставление услуг по управлению и отслеживанию уровней запасов, заказов и доставки продукции'),
       (10, 'ru', 'Срочная доставка', 'Предложение премиум-услуг за доставку быстрее обычного срока');

--changeset ayushchenko:3
INSERT INTO invoice_status (name)
VALUES ('Draft'),
       ('Unpaid'),
       ('Partially paid'),
       ('Paid'),
       ('Overdue'),
       ('Cancelled');

--changeset ayushchenko:4
INSERT INTO invoice_status_localization(invoice_status_id, language_code, localized_name)
VALUES (1, 'ru', 'Черновик'),
       (2, 'ru', 'Не оплачен'),
       (3, 'ru', 'Частично оплачен'),
       (4, 'ru', 'Оплачен'),
       (5, 'ru', 'Просрочен'),
       (6, 'ru', 'Отменен');


--changeset ayushchenko:5
INSERT INTO currency (code, okv_code, name, enabled)
VALUES ('RUB', 643, 'Russian Ruble', true),
       ('USD', 840, 'US Dollar', true),
       ('EUR', 978, 'Euro', true),
       ('CNY', 156, 'Chinese Yuan', false);

--changeset ayushchenko:6
INSERT INTO payment_type (name, description)
VALUES ('Bank Transfer', 'Payment made through direct transfer of funds from one bank account to another.'),
       ('Cash at the Office', 'Payment made in cash at the company office.'),
       ('Cash at the Warehouse', 'Payment made in cash directly at the warehouse during goods pickup.'),
       ('Cash by Carrier', 'Payment made in cash to the carrier at the time of delivery.'),
       ('Reclamation', 'Refund or compensation paid due to claims or complaints about goods or services.'),
       ('Cryptocurrency', 'Payment made using digital currency such as Bitcoin, Ethereum, etc.'),
       ('Credit Card', 'Payment made using a credit card either online or offline.'),
       ('Online Payment', 'Payment made through online payment systems or gateways.'),
       ('Mobile Payment', 'Payment made through mobile payment apps or mobile wallets.');

--changeset ayushchenko:7
INSERT INTO payment_type_localization(payment_type_id, language_code, localized_name, localized_description)
VALUES (1, 'ru', 'Банковский перевод', 'Оплата средствами прямого перевода с одного банковского счета на другой'),
       (2, 'ru', 'Наличные в офисе', 'Оплата наличными в офисе компании'),
       (3, 'ru', 'Наличные на складе', 'Оплата наличными непосредственно на складе при получении товаров'),
       (4, 'ru', 'Наличные курьеру', 'Оплата наличными курьеру при доставке'),
       (5, 'ru', 'Возврат средств', 'Возврат или компенсация оплаты из-за претензий или жалоб на товары или услуги'),
       (6, 'ru', 'Криптовалюта', 'Оплата с использованием цифровой валюты, такой как Bitcoin, Ethereum и т.д.'),
       (7, 'ru', 'Оплата кредитной картой', 'Оплата с помощью кредитной карты, возможно как онлайн, так и офлайн'),
       (8, 'ru', 'Онлайн-платежи', 'Оплата через онлайн-платежные системы или платежные шлюзы'),
       (9, 'ru', 'Мобильный платеж', 'Оплата с помощью мобильных приложений или электронных кошельков');

--changeset ayushchenko:8
INSERT INTO reference_type (name, created_by, modified_by)
VALUES ('PAYMENT', 'system', 'system'),
       ('INVOICE', 'system', 'system'),
       ('ALLOCATION', 'system', 'system'),
       ('CONVERSION', 'system', 'system'),
       ('PAYMENT ADJUSTMENT', 'system', 'system'),
       ('PAYMENT REVERSAL', 'system', 'system'),
       ('INVOICE ADJUSTMENT', 'system', 'system'),
       ('INVOICE REVERSAL', 'system', 'system'),
       ('WRITE_OFF', 'system', 'system');

-- --changeset ayushchenko:9
-- INSERT INTO partner_type (name, description, created_by, modified_by)
-- VALUES ('CLIENT', 'Customers who use logistics services', 'system', 'system'),
--        ('PARTNER', 'Business partners such as warehouses, transport companies, customs agents, etc.', 'system',
--         'system');

--changeset ayushchenko:9
INSERT INTO payment_status (name)
VALUES ('Completed'),
       ('Cancelled'),
       ('Refunded');