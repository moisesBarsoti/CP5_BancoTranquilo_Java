
package br.com.fiap.bancotranquilo.service;

import br.com.fiap.bancotranquilo.dto.PaymentRequest;
import br.com.fiap.bancotranquilo.dto.PaymentResponse;
import br.com.fiap.bancotranquilo.entity.Payment;
import br.com.fiap.bancotranquilo.exception.PaymentConflictException;
import br.com.fiap.bancotranquilo.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private static final BigDecimal SIMULATED_LIMIT =
            new BigDecimal("5000.00");

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        boolean approved =
                request.amount().compareTo(SIMULATED_LIMIT) <= 0;

        String status = approved ? "APPROVED" : "DECLINED";

        String message = approved
                ? "Payment approved successfully"
                : "Payment declined by simulated bank";

        String transactionId = "BT-" + UUID.randomUUID();

        paymentRepository.insertIfAbsent(
                request.orderId(),
                request.amount(),
                transactionId,
                status,
                message
        );

        Payment payment = paymentRepository
                .findByOrderId(request.orderId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Payment could not be retrieved"
                        )
                );

        if (payment.getAmount().compareTo(request.amount()) != 0) {
            throw new PaymentConflictException(
                    "Order already has a payment with a different amount"
            );
        }

        return new PaymentResponse(
                payment.getTransactionId(),
                payment.getOrderId(),
                payment.getStatus(),
                payment.getMessage()
        );
    }
}
