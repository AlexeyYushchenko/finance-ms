package ru.utlc.financialmanagementservice.dto.ledgerentry;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;

import java.math.BigDecimal;
import java.time.LocalDate;

import static ru.utlc.financialmanagementservice.constants.RefTypeConstants.*;

public record LedgerEntryCreateUpdateDto(

        @NotNull(message = "{validation.ledger.partnerId.required}")
        Long partnerId,

        @NotNull(message = "{validation.ledger.currencyId.required}")
        Integer currencyId,

        @NotNull(message = "{validation.ledger.amount.required}")
        @DecimalMin(value = "0.01", message = "{validation.paymentInvoice.allocatedAmount.min}")
        @Digits(integer = 15, fraction = 2, message = "{validation.ledger.amount.format}")
        BigDecimal amount,

        @NotNull(message = "{validation.ledger.referenceTypeId.required}")
        Integer referenceTypeId,

        Long invoiceId,
        Long paymentId,

        @PastOrPresent(message = "{validation.ledger.transactionDate.pastOrPresent}")
        @NotNull(message = "{validation.ledger.transactionDate.required}")
        LocalDate transactionDate
) {

    // Factory method for creating a Payment ledger entry (initial entry)
    public static LedgerEntryCreateUpdateDto forPayment(PaymentReadDto dto) {
        return new LedgerEntryCreateUpdateDto(
                dto.partnerId(),
                dto.currencyId(),
                dto.totalAmount(),
                REFTYPE_PAYMENT,
                null,
                dto.id(),
                dto.paymentDate()
        );
    }

    // Factory method for creating a Payment adjustment entry
    public static LedgerEntryCreateUpdateDto forPaymentAdjustment(PaymentReadDto dto, BigDecimal difference) {
        return new LedgerEntryCreateUpdateDto(
                dto.partnerId(),
                dto.currencyId(),
                difference,
                REFTYPE_PAYMENT_ADJUSTMENT,
                null,
                dto.id(),
                dto.paymentDate()
        );
    }

    // Factory method for creating a Payment reversal entry
    public static LedgerEntryCreateUpdateDto forPaymentReversal(PaymentReadDto dto) {
        return new LedgerEntryCreateUpdateDto(
                dto.partnerId(),
                dto.currencyId(),
                dto.totalAmount() != null ? dto.totalAmount().negate() : null,
                REFTYPE_PAYMENT_REVERSAL,
                null,
                dto.id(),
                dto.paymentDate()
        );
    }

    // Factory method for creating an Invoice ledger entry (invoices are stored as negative)
    public static LedgerEntryCreateUpdateDto forInvoice(InvoiceReadDto dto) {
        return new LedgerEntryCreateUpdateDto(
                dto.partnerId(),
                dto.currencyId(),
                dto.totalAmount().negate(),
                REFTYPE_INVOICE,
                dto.id(),
                null,
                dto.issueDate()
        );
    }

    // Factory method for creating an Invoice adjustment entry (adjustments are negative)
    public static LedgerEntryCreateUpdateDto forInvoiceAdjustment(InvoiceReadDto dto, BigDecimal difference) {
        return new LedgerEntryCreateUpdateDto(
                dto.partnerId(),
                dto.currencyId(),
                difference.negate(),
                REFTYPE_INVOICE_ADJUSTMENT,
                dto.id(),
                null,
                dto.issueDate()
        );
    }

    // Factory method for creating an Invoice reversal entry (reversal makes the negative invoice positive)
    public static LedgerEntryCreateUpdateDto forInvoiceReversal(InvoiceReadDto dto) {
        return new LedgerEntryCreateUpdateDto(
                dto.partnerId(),
                dto.currencyId(),
                dto.totalAmount(),
                REFTYPE_INVOICE_REVERSAL,
                dto.id(),
                null,
                dto.issueDate()
        );
    }

    public static LedgerEntryCreateUpdateDto forAllocation(Long partnerId,
                                                           Integer currencyId,
                                                           BigDecimal amount,
                                                           Long paymentId,
                                                           Long invoiceId,
                                                           LocalDate transactionDate) {
        return new LedgerEntryCreateUpdateDto(partnerId, currencyId, amount, REFTYPE_ALLOCATION, invoiceId, paymentId, transactionDate);
    }
}
