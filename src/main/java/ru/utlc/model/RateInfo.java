package ru.utlc.model;

import java.math.BigDecimal;

public record RateInfo(String charCode, BigDecimal value, BigDecimal nominal) {
}