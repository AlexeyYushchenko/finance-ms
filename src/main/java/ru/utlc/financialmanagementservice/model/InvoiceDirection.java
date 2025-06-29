package ru.utlc.financialmanagementservice.model;


import org.springframework.data.relational.core.mapping.Table;

public enum InvoiceDirection {
    RECEIVABLE, // Client owes you
    PAYABLE     // You owe supplier/partner
}