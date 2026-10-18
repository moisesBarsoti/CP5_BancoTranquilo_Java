
package br.com.fiap.bancotranquilo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequest(
    @NotNull
    @Positive
    Long orderId,

    @NotNull
    @DecimalMin("0.01")
    BigDecimal amount
) {}