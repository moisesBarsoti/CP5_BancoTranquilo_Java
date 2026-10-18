
package br.com.fiap.bancotranquilo.dto;

public record PaymentResponse(
    String transactionId,
    Long orderId,
    String status,
    String message
) {}